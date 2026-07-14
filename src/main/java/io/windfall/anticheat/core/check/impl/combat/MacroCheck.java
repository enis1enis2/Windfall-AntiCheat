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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects movement macros (e.g., auto-walk, anti-knockback scripts) by analyzing patterns
 * in the player's movement packet sequence.
 *
 * <p><b>Algorithm:</b> Each movement packet is encoded as a single character
 * ({@code P}osition, {@code R}otation, {@code M}ove, {@code F}lying). Characters are
 * accumulated in a sliding buffer of up to {@value PATTERN_WINDOW} packets. When the
 * buffer contains enough data (>= 10 chars), the check searches previously seen patterns
 * for substrings matching the tail 5 characters of the current buffer.</p>
 *
 * <p><b>Thresholds:</b> If the repetition ratio exceeds {@value REPETITION_THRESHOLD}
 * (90%) and the occurrence count meets {@value MIN_REPEAT_COUNT} (8+), buffer increases.
 * A flag is raised when the buffer exceeds 5.0.</p>
 *
 * <p>A pattern flush occurs after {@code 100ms} of inactivity, ensuring that bursts
 * of movement are captured as distinct pattern strings.</p>
 *
 * @see io.windfall.anticheat.core.check.Check
 */
@CheckData(name = "Macro A", stableKey = "windfall.combat.macro", decay = 0.01, setbackVl = 20)
public class MacroCheck extends Check implements PacketCheck {

    /** Maximum number of recent movement characters kept in the sliding buffer. */
    private static final int PATTERN_WINDOW = 50;
    /** Minimum number of historical occurrences required before flagging a repeated pattern. */
    private static final int MIN_REPEAT_COUNT = 8;
    /** Ratio threshold (0.0–1.0) above which a pattern is considered macro-repetitive. */
    private static final double REPETITION_THRESHOLD = 0.9;

    /** Per-player state holding the pattern buffer and historical pattern counts. */
    private static final class PlayerState {
        final Map<String, Integer> movementPatterns = new HashMap<>();
        long lastPatternTime;
        StringBuilder patternBuffer = new StringBuilder();
        int totalPatterns;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or initializes the tracking state for the given player.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming movement packets. If the gap since the last packet exceeds 100ms,
     * the current pattern buffer is flushed (recorded as a distinct pattern). The movement
     * code for the packet type is appended and repetition detection runs when the buffer
     * reaches 10+ characters.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (!isMovementType(type)) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        if (now - state.lastPatternTime > 100) {
            flushPattern(player, state);
        }
        state.lastPatternTime = now;

        char movementCode = getMovementCode(type, player);
        state.patternBuffer.append(movementCode);
        state.totalPatterns++;

        if (state.patternBuffer.length() > PATTERN_WINDOW) {
            state.patternBuffer.deleteCharAt(0);
        }

        if (state.patternBuffer.length() >= 10) {
            detectRepetition(player, state);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Flushes the current movement pattern into the historical map if it meets the minimum
     * length (5 chars), then resets the buffer. Patterns shorter than 5 chars are discarded
     * as they are too short to meaningfully identify macro behavior.
     *
     * @param player the player whose pattern to flush
     * @param state  the player's tracking state
     */
    private void flushPattern(WindfallPlayer player, PlayerState state) {
        if (state.patternBuffer.length() >= 5) {
            String pattern = state.patternBuffer.toString();
            state.movementPatterns.merge(pattern, 1, Integer::sum);
        }
        state.patternBuffer = new StringBuilder();
    }

    /**
     * Analyzes the current pattern buffer for repetitive substrings. Checks whether the
     * tail 5 characters of the buffer appear in historically recorded patterns. If the
     * repetition ratio exceeds {@value REPETITION_THRESHOLD} with at least
     * {@value MIN_REPEAT_COUNT} occurrences, buffer increases and flags when above 5.0.
     *
     * @param player the player being checked
     * @param state  the player's tracking state
     */
    private void detectRepetition(WindfallPlayer player, PlayerState state) {
        String current = state.patternBuffer.toString();
        if (current.length() < 10) return;

        int occurrences = 0;
        for (Map.Entry<String, Integer> entry : state.movementPatterns.entrySet()) {
            if (entry.getKey().contains(current.substring(current.length() - 5))) {
                occurrences += entry.getValue();
            }
        }

        double ratio = (double) occurrences / Math.max(state.totalPatterns, 1);
        if (ratio > REPETITION_THRESHOLD && occurrences >= MIN_REPEAT_COUNT) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
                state.movementPatterns.clear();
                state.totalPatterns = 0;
            }
        }
    }

    /**
     * Encodes a movement packet type as a single character for pattern analysis.
     * Using compact codes enables efficient substring matching and storage.
     *
     * @param type   the packet type
     * @param player the player (unused here, reserved for future per-version encoding)
     * @return a character code: {@code P}osition, {@code R}otation, {@code M}ove, {@code F}lying, or {@code X} for unknown
     */
    private char getMovementCode(PacketTypeCommon type, WindfallPlayer player) {
        if (type == PacketType.Play.Client.PLAYER_POSITION) return 'P';
        if (type == PacketType.Play.Client.PLAYER_ROTATION) return 'R';
        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) return 'M';
        if (type == PacketType.Play.Client.PLAYER_FLYING) return 'F';
        return 'X';
    }

    /**
     * Checks whether the given packet type represents player movement.
     *
     * @param type the packet type to check
     * @return {@code true} if the type is position, rotation, position+rotation, or flying
     */
    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
            || type == PacketType.Play.Client.PLAYER_ROTATION
            || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
            || type == PacketType.Play.Client.PLAYER_FLYING;
    }
}
