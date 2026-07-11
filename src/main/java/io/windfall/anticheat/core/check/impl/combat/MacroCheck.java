package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.HashMap;
import java.util.Map;

/**
 * Detects macro keybinding: repetitive identical movement/action patterns
 * that indicate automated input rather than human play.
 */
@CheckData(name = "Macro A", stableKey = "windfall.combat.macro", decay = 0.01, setbackVl = 20)
public class MacroCheck extends Check implements PacketCheck {

    private static final int PATTERN_WINDOW = 50;
    private static final int MIN_REPEAT_COUNT = 8;
    private static final double REPETITION_THRESHOLD = 0.9;

    private final Map<String, Integer> movementPatterns = new HashMap<>();
    private long lastPatternTime;
    private StringBuilder patternBuffer = new StringBuilder();
    private int totalPatterns;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (!isMovementType(type)) return;

        long now = System.currentTimeMillis();
        if (now - lastPatternTime > 100) {
            flushPattern(player);
        }
        lastPatternTime = now;

        char movementCode = getMovementCode(type, player);
        patternBuffer.append(movementCode);
        totalPatterns++;

        if (patternBuffer.length() > PATTERN_WINDOW) {
            patternBuffer.deleteCharAt(0);
        }

        if (patternBuffer.length() >= 10) {
            detectRepetition(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void flushPattern(WindfallPlayer player) {
        if (patternBuffer.length() >= 5) {
            String pattern = patternBuffer.toString();
            movementPatterns.merge(pattern, 1, Integer::sum);
        }
        patternBuffer = new StringBuilder();
    }

    private void detectRepetition(WindfallPlayer player) {
        String current = patternBuffer.toString();
        if (current.length() < 10) return;

        int occurrences = 0;
        for (Map.Entry<String, Integer> entry : movementPatterns.entrySet()) {
            if (entry.getKey().contains(current.substring(current.length() - 5))) {
                occurrences += entry.getValue();
            }
        }

        double ratio = (double) occurrences / Math.max(totalPatterns, 1);
        if (ratio > REPETITION_THRESHOLD && occurrences >= MIN_REPEAT_COUNT) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
                movementPatterns.clear();
                totalPatterns = 0;
            }
        }
    }

    private char getMovementCode(PacketTypeCommon type, WindfallPlayer player) {
        if (type == PacketType.Play.Client.PLAYER_POSITION) return 'P';
        if (type == PacketType.Play.Client.PLAYER_ROTATION) return 'R';
        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) return 'M';
        if (type == PacketType.Play.Client.PLAYER_FLYING) return 'F';
        return 'X';
    }

    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
            || type == PacketType.Play.Client.PLAYER_ROTATION
            || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
            || type == PacketType.Play.Client.PLAYER_FLYING;
    }
}
