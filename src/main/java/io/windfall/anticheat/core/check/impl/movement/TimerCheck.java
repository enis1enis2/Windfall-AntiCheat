package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.Iterator;

@CheckData(name = "Timer A", stableKey = "windfall.movement.timer", decay = 0.005, setbackVl = 25)
public class TimerCheck extends Check implements PacketCheck {

    private static final int NORMAL_PACKETS_PER_TICK_MIN = 1;
    private static final int NORMAL_PACKETS_PER_TICK_MAX = 3;
    private static final int LATENCY_JITTER_TOLERANCE = 2;
    private static final long WINDOW_DURATION_MS = 1000;
    private static final long WINDOW_TICK_COUNT = 20;
    private static final double SPEEDHACK_MULTIPLIER = 1.2;
    private static final double SLOWHACK_MULTIPLIER = 0.5;
    private static final double FLAG_THRESHOLD_MULTIPLIER = 1.5;
    private static final long DOUBLE_BUFFER_INTERVAL_MS = 500;

    private final ArrayDeque<Long> packetTimestamps = new ArrayDeque<>();
    private final ArrayDeque<Long> secondaryPacketTimestamps = new ArrayDeque<>();

    private long lastSecondaryFlush = System.currentTimeMillis();
    private int consecutiveHighWindows;
    private int consecutiveLowWindows;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        long now = System.currentTimeMillis();

        packetTimestamps.addLast(now);
        secondaryPacketTimestamps.addLast(now);

        cleanWindow(packetTimestamps, now);
        cleanWindow(secondaryPacketTimestamps, now);

        double packetsPerTick = packetTimestamps.size() / (double) WINDOW_TICK_COUNT;

        int jitterTolerance = Math.max(LATENCY_JITTER_TOLERANCE,
                (int) Math.ceil(player.getTransactionPing() / 50.0));

        int maxNormalPerTick = NORMAL_PACKETS_PER_TICK_MAX + jitterTolerance;
        double speedHackThreshold = maxNormalPerTick * SPEEDHACK_MULTIPLIER;
        double slowHackThreshold = Math.max(0.2, NORMAL_PACKETS_PER_TICK_MIN * SLOWHACK_MULTIPLIER);

        if (packetsPerTick > speedHackThreshold) {
            if (packetsPerTick > speedHackThreshold * FLAG_THRESHOLD_MULTIPLIER) {
                consecutiveHighWindows++;
            } else {
                consecutiveHighWindows = Math.max(0, consecutiveHighWindows - 1);
            }

            if (consecutiveHighWindows >= 2) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    consecutiveHighWindows = 0;
                }
            }
        } else if (packetsPerTick < slowHackThreshold) {
            consecutiveLowWindows++;
            if (consecutiveLowWindows >= 4) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    consecutiveLowWindows = 0;
                }
            }
        } else {
            consecutiveHighWindows = Math.max(0, consecutiveHighWindows - 1);
            consecutiveLowWindows = Math.max(0, consecutiveLowWindows - 1);
            decreaseBuffer(player, 0.1);
        }

        if (now - lastSecondaryFlush >= DOUBLE_BUFFER_INTERVAL_MS) {
            long secondaryCount = secondaryPacketTimestamps.stream()
                    .filter(t -> now - t <= DOUBLE_BUFFER_INTERVAL_MS)
                    .count();
            double secondaryRate = secondaryCount / (DOUBLE_BUFFER_INTERVAL_MS / 50.0);

            if (secondaryRate > speedHackThreshold * 1.3) {
                increaseBuffer(player, 0.5);
            }

            secondaryPacketTimestamps.clear();
            lastSecondaryFlush = now;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void cleanWindow(ArrayDeque<Long> window, long now) {
        Iterator<Long> it = window.iterator();
        while (it.hasNext()) {
            if (now - it.next() > WINDOW_DURATION_MS) {
                it.remove();
            } else {
                break;
            }
        }
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }
}
