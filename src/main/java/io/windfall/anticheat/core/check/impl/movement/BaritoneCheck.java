package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects Baritone-style automated pathfinding by identifying two signatures
 * of inhuman movement: unnaturally straight lines and perfectly constant speed
 * over a sustained sample window.
 *
 * <p><b>Detection method 1 — Straight line:</b> Computes the angular difference
 * (in radians) between consecutive movement vectors using {@code atan2}. If the
 * angle stays below {@link #STRAIGHT_LINE_TOLERANCE} for more than
 * {@link #MIN_STRAIGHT_TICKS} consecutive ticks, the buffer increases.</p>
 *
 * <p><b>Detection method 2 — Perfect path:</b> Over a rolling window of
 * {@link #PATH_SAMPLE_SIZE} segments, the ratio of segments whose speed change
 * is below 0.005 blocks/tick is computed. If this ratio exceeds
 * {@link #PERFECT_PATH_THRESHOLD} (95%), the movement is considered too uniform
 * for a human player.</p>
 *
 * @see CompatFlag#RELAX_ON_MISMATCH
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Baritone A", stableKey = "windfall.movement.baritone", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class BaritoneCheck extends Check implements PacketCheck {

    /** Maximum angular deviation (radians) between consecutive vectors to count as straight. */
    private static final double STRAIGHT_LINE_TOLERANCE = 0.02;

    /** Minimum consecutive straight-line ticks before the buffer begins increasing. */
    private static final int MIN_STRAIGHT_TICKS = 20;

    /** Fraction of segments that must have near-constant speed to trigger the perfect-path detection. */
    private static final double PERFECT_PATH_THRESHOLD = 0.95;

    /** Number of movement segments sampled for the perfect-path ratio calculation. */
    private static final int PATH_SAMPLE_SIZE = 40;

    /** Per-player movement state for straight-line and speed-variance tracking. */
    private static final class PlayerState {
        /** Ticks of consecutive movement along the same heading. */
        int straightTicks;
        /** Previous tick's X displacement. */
        double lastDeltaX;
        /** Previous tick's Z displacement. */
        double lastDeltaZ;
        /** Segments in the current window with speed difference &lt; 0.005. */
        int perfectPathSegments;
        /** Total segments processed in the current sampling window. */
        int totalSegments;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** Cleans up player state on disconnect. */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes each movement packet to update straight-line and speed-variance
     * statistics. Two independent detection heuristics may each flag the player.
     *
     * @param player the player associated with this packet
     * @param event  the incoming movement packet
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        /** Below 0.05 blocks/tick the player is effectively stationary — reset counters. */
        if (horizontalSpeed < 0.05) {
            state.straightTicks = 0;
            decreaseBuffer(player, 0.1);
            return;
        }

        double lastSpeed = Math.sqrt(state.lastDeltaX * state.lastDeltaX + state.lastDeltaZ * state.lastDeltaZ);

        if (lastSpeed > 0.05 && horizontalSpeed > 0.05) {
            /**
             * Compute the absolute angular difference between the current and previous
             * movement vectors using atan2. The result is in [0, PI] after normalisation.
             */
            double angle = Math.abs(Math.atan2(deltaZ, deltaX) - Math.atan2(state.lastDeltaZ, state.lastDeltaX));
            if (angle > Math.PI) angle = 2 * Math.PI - angle;

            if (angle < STRAIGHT_LINE_TOLERANCE) {
                state.straightTicks++;
            } else {
                state.straightTicks = 0;
            }

            state.totalSegments++;
            /** Speed difference below 0.005 blocks/tick indicates inhumanly constant speed. */
            double speedDiff = Math.abs(horizontalSpeed - lastSpeed);
            if (speedDiff < 0.005) {
                state.perfectPathSegments++;
            }
        }

        /** Detection 1: sustained straight-line movement exceeds the tick threshold. */
        if (state.straightTicks > MIN_STRAIGHT_TICKS) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
                state.straightTicks = 0;
            }
        }

        /** Detection 2: evaluate the perfect-path ratio over the rolling window. */
        if (state.totalSegments >= PATH_SAMPLE_SIZE) {
            double perfectRatio = (double) state.perfectPathSegments / state.totalSegments;
            if (perfectRatio > PERFECT_PATH_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.2);
            }
            state.totalSegments = 0;
            state.perfectPathSegments = 0;
        }

        state.lastDeltaX = deltaX;
        state.lastDeltaZ = deltaZ;
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /** Returns {@code true} if the event is a movement-type packet. */
    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
