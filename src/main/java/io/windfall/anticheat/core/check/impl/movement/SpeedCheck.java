package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.data.ActionData;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects horizontal speed exceeding the maximum predicted movement for the player's current state.
 *
 * <p>Algorithm: Each movement packet, a {@link PredictionContext} computes the server-authoritative
 * maximum horizontal speed ({@link PredictionContext#predictedMaxHorizontalSpeed}) accounting for
 * sprint, potions, ice, soul sand, cobwebs, and other movement modifiers. The player's actual
 * horizontal speed is compared against this maximum with a small tolerance margin.
 *
 * <p>Detection stages:
 * <ol>
 *   <li>If actual speed exceeds predicted max by more than {@value SPEED_TOLERANCE}x, the buffer
 *       increases proportionally to the exceed ratio.</li>
 *   <li>If the exceed ratio exceeds 2.0, the player is flagged immediately (blatant hack).</li>
 *   <li>If the buffer exceeds {@value MIN_SPEED_FLAG_BUFFER}, the player is flagged (gradual buildup).</li>
 * </ol>
 *
 * <p>Special cases:
 * <ul>
 *   <li>Pre-1.18.2 clients may report sub-threshold speeds due to protocol differences — these are
 *       excluded via {@value PRE_1_18_2_THRESHOLD}.</li>
 *   <li>Near-zero speeds (&lt;0.005) are ignored to avoid floating-point noise.</li>
 * </ul>
 *
 * @see PredictionContext for speed prediction logic
 * @see PredictionEngine for movement packet detection
 */
@CheckData(name = "Speed A", stableKey = "windfall.movement.speed", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.5)
public class SpeedCheck extends Check implements PacketCheck {

    /** Multiplier applied to the predicted max speed before flagging — 1.05 allows 5% headroom for float rounding */
    private static final double SPEED_TOLERANCE = 1.05;
    /** Minimum horizontal speed on pre-1.18.2 clients; smaller values indicate protocol-version-specific float compression artifacts */
    private static final double PRE_1_18_2_THRESHOLD = 0.03;
    /** Buffer level at which a gradual speed violation triggers a flag */
    private static final double MIN_SPEED_FLAG_BUFFER = 3.0;

    private static final class PlayerState {
        double maxObservedSpeed;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Returns the per-player speed check state.
     *
     * @param player the player to retrieve state for
     * @return the player's {@link PlayerState}, creating one if absent
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Processes incoming movement packets to compare actual vs predicted horizontal speed.
     *
     * @param player the player who sent the movement packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        ActionData actionData = player.getActionData();

        // Exempt if a piston recently pushed the player — causes sudden horizontal speed spikes
        if (actionData.hasRecentPistonUpdate(5)) {
            decreaseBuffer(player, 0.5);
            return;
        }

        // Exempt if a block was recently placed/broken under the player — causes position adjustments
        if (actionData.hasRecentBlockUpdateUnder(5)) {
            decreaseBuffer(player, 0.3);
            return;
        }

        PredictionContext ctx = new PredictionContext(player);

        /** Actual horizontal speed calculated from the movement delta */
        double actualSpeed = ctx.horizontalSpeed;

        PlayerState state = getState(player);
        /** Track the highest observed speed for diagnostic/reporting purposes */
        if (actualSpeed > state.maxObservedSpeed) {
            state.maxObservedSpeed = actualSpeed;
        }

        /**
         * Pre-1.18.2 clients (protocol &lt; 757) may report sub-pixel movement due to
         * older position encoding — skip speeds below this threshold to avoid false positives.
         */
        if (actualSpeed < PRE_1_18_2_THRESHOLD && ctx.protocolVersion < 757) {
            decreaseBuffer(player, 0.1);
            return;
        }

        /** Ignore near-zero movement (floating-point noise) */
        if (actualSpeed < 0.005) {
            decreaseBuffer(player, 0.05);
            return;
        }

        /** Server-predicted maximum horizontal speed for this tick */
        double maxSpeed = ctx.predictedMaxHorizontalSpeed;

        if (actualSpeed > maxSpeed * SPEED_TOLERANCE) {
            /**
             * exceedRatio = how many times the actual speed exceeds the predicted max.
             * A ratio of 2.0+ is blatant and triggers an immediate flag.
             */
            double exceedRatio = actualSpeed / Math.max(maxSpeed, 0.001);
            if (exceedRatio > 2.0) {
                flag(player);
                resetBuffer(player);
            } else {
                /** Gradual buffer increase proportional to the exceed amount */
                increaseBuffer(player, 0.5 * (exceedRatio - 1.0));
                if (getBuffer(player) > MIN_SPEED_FLAG_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

 /** No-op — speed detection only requires incoming movement packets. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Returns the highest horizontal speed observed for this player since login.
     * Useful for diagnostic reports and alert context.
     *
     * @param player the player to query
     * @return maximum observed horizontal speed in blocks/tick
     */
    public double getMaxObservedSpeed(WindfallPlayer player) {
        return getState(player).maxObservedSpeed;
    }
}
