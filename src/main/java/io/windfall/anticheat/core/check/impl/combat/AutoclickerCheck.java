package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;

@CheckData(name = "Autoclicker A", stableKey = "windfall.combat.autoclicker", decay = 0.01, setbackVl = 20)
public class AutoclickerCheck extends Check implements PacketCheck {

    private static final int MIN_CLICKS_FOR_EVAL = 20;
    private static final long CLICK_WINDOW_MS = 3000;
    // Perfectly consistent click intervals (low standard deviation) = autoclicker
    private static final double LOW_CPS = 4.0;
    private static final double HIGH_CPS = 16.0;
    private static final double STD_DEV_AUTOCLICKER_THRESHOLD = 3.0;
    // Any human has at least 15ms variance between clicks
    private static final double MIN_HUMAN_STD_DEV = 15.0;

    private final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        long now = System.currentTimeMillis();
        clickTimestamps.addLast(now);
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > CLICK_WINDOW_MS) {
            clickTimestamps.removeFirst();
        }

        if (clickTimestamps.size() < MIN_CLICKS_FOR_EVAL) return;

        double cps = clickTimestamps.size() / (CLICK_WINDOW_MS / 1000.0);
        if (cps < LOW_CPS || cps > HIGH_CPS) {
            decreaseBuffer(player, 0.2);
            return;
        }

        double stdDev = calculateStdDev();
        // High CPS with low variance = autoclicker; humans have at least 15ms std dev
        if (stdDev < STD_DEV_AUTOCLICKER_THRESHOLD && cps > LOW_CPS) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (stdDev < MIN_HUMAN_STD_DEV) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 6.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double calculateStdDev() {
        if (clickTimestamps.size() < 2) return Double.MAX_VALUE;

        long first = clickTimestamps.peekFirst();
        double mean = 0;
        int count = 0;
        for (Long ts : clickTimestamps) {
            if (ts == first) continue;
            mean += ts - first;
            count++;
        }
        if (count == 0) return Double.MAX_VALUE;
        mean /= count;

        double variance = 0;
        for (Long ts : clickTimestamps) {
            if (ts == first) continue;
            double diff = (ts - first) - mean;
            variance += diff * diff;
        }
        variance /= count;

        return Math.sqrt(variance);
    }
}
