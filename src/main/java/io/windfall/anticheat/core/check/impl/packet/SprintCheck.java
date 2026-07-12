package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects abnormal sprint toggling patterns that indicate automated movement (e.g., kill aura).
 *
 * <p>Detection strategy:
 * <ul>
 *   <li>Monitors sprint state changes on movement packets. Counts toggle events (sprint on↔off)
 *       within a {@value #TOGGLE_WINDOW_MS}ms sliding window</li>
 *   <li>If more than {@value #MAX_SPRINT_TOGGLE_PER_SECOND} toggles occur in the window,
 *       increments a consecutive flag counter</li>
 *   <li>Requires {@value #MIN_TOGGLE_FLAGS} consecutive flagged windows before actual flag
 *       (reduces false positives from lag spikes)</li>
 *   <li>Consecutive flags decay by 1 when the window is clean (below toggle threshold)</li>
 * </ul>
 *
 * <p>This detects kill aura and aim assist bots that rapidly toggle sprint to optimize combat
 * reach/damage while moving, which is impossible for human players to sustain.
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value #MAX_SPRINT_TOGGLE_PER_SECOND} toggles per {@value #TOGGLE_WINDOW_MS}ms</li>
 *   <li>{@value #MIN_TOGGLE_FLAGS} consecutive flagged windows before actual flag</li>
 * </ul>
 *
 * <p>Setback at VL 15, decay 0.01/tick.
 *
 * @see PacketOrderCheck for packet ordering validation
 * @see BadPacketsCheck for movement packet field validation
 */
@CheckData(name = "Sprint A", stableKey = "windfall.packet.sprint", decay = 0.01, setbackVl = 15)
public class SprintCheck extends Check implements PacketCheck {

    /** Maximum sprint state toggle events allowed per sliding window before flag */
    private static final int MAX_SPRINT_TOGGLE_PER_SECOND = 4;
    /** Duration of the sprint toggle detection window in milliseconds */
    private static final long TOGGLE_WINDOW_MS = 1000;
    /** Number of consecutive flagged windows required before actual flag (noise filter) */
    private static final int MIN_TOGGLE_FLAGS = 3;

    /**
     * Per-player state tracking sprint toggle patterns.
     */
    private static final class PlayerState {
        /** Previous sprint state for toggle detection (true = sprinting) */
        boolean lastSprinting;
        /** Start time of the current toggle counting window */
        long lastToggleTime;
        /** Number of sprint state toggles in the current window */
        int toggleCount;
        /** Number of consecutive windows that exceeded the toggle threshold */
        int consecutiveFlags;
    }

    /** Thread-safe map of player UUID to their sprint toggle state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player sprint toggle state.
     *
     * @param player the player to get state for
     * @return the player's state
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** {@inheritDoc} Clears player state to prevent memory leaks on disconnect */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes movement packets to track sprint state toggling patterns.
     * Only processes movement packets (flying, position, position+rotation).
     * Uses a sliding window with consecutive flag counting for noise reduction.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);
        boolean sprinting = player.isSprinting();
        long now = System.currentTimeMillis();

        if (now - state.lastToggleTime > TOGGLE_WINDOW_MS) {
            if (state.toggleCount > MAX_SPRINT_TOGGLE_PER_SECOND) {
                state.consecutiveFlags++;
                if (state.consecutiveFlags >= MIN_TOGGLE_FLAGS) {
                    flag(player);
                    state.consecutiveFlags = 0;
                }
            } else {
                state.consecutiveFlags = Math.max(0, state.consecutiveFlags - 1);
            }
            state.toggleCount = 0;
            state.lastToggleTime = now;
        }

        if (sprinting != state.lastSprinting) {
            state.toggleCount++;
        }
        state.lastSprinting = sprinting;
    }

    /** {@inheritDoc} No outgoing packet processing needed for sprint checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Checks if the given packet is a movement packet that carries sprint state.
     *
     * @param event the packet event to check
     * @return true if the packet is a movement packet (flying, position, position+rotation)
     */
    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
