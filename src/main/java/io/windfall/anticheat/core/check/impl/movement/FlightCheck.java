package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.player.data.ActionData;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects unnatural vertical movement including flight, hover, and upward motion without a valid cause.
 *
 * <p>Algorithm: Each tick the check predicts the player's expected vertical delta using
 * {@link PredictionEngine#predictDeltaY} which accounts for gravity, water/lava drag, climbing,
 * honey slowdown, slow falling, levitation, riptide, and fall-flying (elytra). The actual vertical
 * delta is compared against this prediction.
 *
 * <p>Three detection phases:
 * <ol>
 *   <li><b>Vertical deviation</b>: If |actual − predicted| exceeds {@value VERTICAL_TOLERANCE} and
 *       the player is not using riptide, elytra, or levitation, the buffer increases. An upward
 *       deviation when expected to be falling or stationary is penalized more heavily (1.5 per tick).</li>
 *   <li><b>Hover detection</b>: If the player stays airborne for more than {@value HOVER_TICK_THRESHOLD}
 *       ticks with near-zero vertical movement (&lt;{@value HOVER_DELTA_THRESHOLD}), the buffer increases
 *       by 1.0 per tick — catches hover/fly hacks that maintain a fixed Y.</li>
 *   <li><b>NoFall fallback</b>: Detects fall distance exceeding {@value NO_FALL_DISTANCE} blocks with
 *       vertical velocity exceeding {@value NO_FALL_VELOCITY_THRESHOLD} while on-ground.</li>
 * </ol>
 *
 * @see PredictionEngine#predictDeltaY for vertical movement prediction
 * @see PredictionContext for per-tick movement data
 * @see NoFallCheck for the dedicated no-fall detection
 */
@CheckData(name = "Fly A", stableKey = "windfall.movement.fly", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class FlightCheck extends Check implements PacketCheck {

    /** Initial upward velocity when a player jumps — 0.42 blocks/tick (Minecraft vanilla value) */
    private static final double JUMP_MOMENTUM = 0.42;
    /** Maximum allowed deviation between predicted and actual deltaY before it's considered suspicious */
    private static final double VERTICAL_TOLERANCE = 0.05;
    /** Widened tolerance when unconfirmed state changes exist — prevents false positives from deferred world changes */
    private static final double VERTICAL_TOLERANCE_UNCONFIRMED = 0.15;
    /** Number of consecutive ticks a player must hover before hover detection activates */
    private static final int HOVER_TICK_THRESHOLD = 20;
    /** Maximum vertical displacement per tick to count as "hovering" (near-zero movement) */
    private static final double HOVER_DELTA_THRESHOLD = 0.005;
    /** Minimum downward velocity (blocks/tick) to trigger no-fall fall-distance check */
    private static final double NO_FALL_VELOCITY_THRESHOLD = 0.5;
    /** Minimum fall distance (blocks) before the no-fall sub-check considers it a violation */
    private static final double NO_FALL_DISTANCE = 3.0;

    private static final class PlayerState {
        double expectedDeltaY;
        int hoverTicks;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Returns the per-player flight check state.
     *
     * @param player the player to retrieve state for
     * @return the player's {@link PlayerState}, creating one if absent
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming movement packets to detect vertical movement anomalies.
     *
     * <p>Predicts the expected vertical delta using full physics simulation and compares
     * it against the actual reported delta. Also triggers hover and no-fall sub-checks.
     *
     * @param player the player who sent the movement packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        ActionData actionData = player.getActionData();

        // Exempt if a piston recently pushed the player — piston movement causes erratic vertical deltas
        if (actionData.hasRecentPistonUpdate(5)) {
            decreaseBuffer(player, 0.5);
            return;
        }

        // Exempt if a block was recently placed/broken directly under the player — causes position adjustments
        if (actionData.hasRecentBlockUpdateUnder(5)) {
            decreaseBuffer(player, 0.3);
            return;
        }

        PlayerState state = getState(player);
        PredictionContext ctx = new PredictionContext(player);

        boolean currentOnGround = ctx.onGround;
        double deltaY = ctx.deltaY;

        /** Reset state when the player touches the ground */
        if (currentOnGround) {
            state.expectedDeltaY = 0;
            state.hoverTicks = 0;
            return;
        }

        /**
         * When transitioning from ground to air, determine the initial vertical velocity:
         * - If deltaY matches jump momentum (0.42 ± tolerance), seed with JUMP_MOMENTUM
         * - If deltaY is near zero, seed with 0 (e.g., walked off edge)
         */
        if (ctx.lastOnGround && !currentOnGround) {
            if (deltaY >= JUMP_MOMENTUM - 0.01 && deltaY <= JUMP_MOMENTUM + 0.15) {
                state.expectedDeltaY = JUMP_MOMENTUM;
            } else if (Math.abs(deltaY) < 0.01) {
                state.expectedDeltaY = 0;
            }
        }

        boolean hasRiptide = PredictionEngine.checkRiptiding(player);
        boolean isFallFlying = PredictionEngine.checkFallFlying(player);

        /** Predict the vertical delta using the full physics model */
        double predictedDeltaY = PredictionEngine.predictDeltaY(
                state.expectedDeltaY,
                ctx.inWater,
                ctx.inLava,
                ctx.climbing,
                PredictionEngine.checkOnHoney(player),
                ctx.hasSlowFalling,
                ctx.hasLevitation,
                ctx.hasLevitation ? PredictionEngine.getLevitationAmplifier(player) : 1.0,
                isFallFlying,
                hasRiptide
        );

        /** Deviation between predicted and actual vertical movement */
        double verticalDelta = deltaY - predictedDeltaY;

        // Bypass resistance: widen tolerance when client has unconfirmed state changes
        // (e.g., block broken under player that client hasn't processed yet)
        SimulationEngine simEngine = WindfallPlugin.getInstance().getSimulationEngine();
        boolean unconfirmedChanges = simEngine != null && simEngine.needsSimulation(player);
        double tolerance = unconfirmedChanges ? VERTICAL_TOLERANCE_UNCONFIRMED : VERTICAL_TOLERANCE;

        boolean verticalDeviation = Math.abs(verticalDelta) > tolerance
                && Math.abs(deltaY) > 0.01;

        if (verticalDeviation && !isFallFlying && !hasRiptide && !ctx.hasLevitation) {
            handleHoverDetection(player, state, ctx);

            /**
             * Upward movement when expected to be falling or stationary is heavily penalized.
             * This catches fly hacks that push the player upward against gravity.
             */
            if (deltaY > 0 && state.expectedDeltaY <= 0 && !ctx.hasLevitation && !hasRiptide && !isFallFlying) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                /**
                 * deviationRatio = magnitude of deviation relative to prediction.
                 * A ratio > 2.0 is blatant and triggers an immediate flag.
                 */
                double deviationRatio = Math.abs(verticalDelta) / Math.max(Math.abs(predictedDeltaY), 0.001);
                if (deviationRatio > 2.0) {
                    flag(player);
                    resetBuffer(player);
                } else {
                    /** Gradual buffer increase, capped at a 2.0 deviation ratio contribution */
                    increaseBuffer(player, 0.3 * Math.min(deviationRatio, 2.0));
                    if (getBuffer(player) > 5.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
            state.hoverTicks = Math.max(0, state.hoverTicks - 1);
        }

        handleNoFall(player, currentOnGround, deltaY, ctx.lastY, ctx.y);
        /** Update the expected velocity for the next tick's prediction */
        state.expectedDeltaY = deltaY;
    }

    /** No-op — flight detection only requires incoming movement packets. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Detects hover hacks — players maintaining a fixed vertical position while airborne.
     *
     * <p>Increments a hover tick counter each tick the player moves less than
     * {@value HOVER_DELTA_THRESHOLD} blocks vertically (and is not in water, lava, or climbing).
     * After {@value HOVER_TICK_THRESHOLD} consecutive hover ticks, the buffer builds, catching
     * fly hacks that keep the player suspended at a constant Y.
     *
     * @param player the player being checked
     * @param state  mutable per-player state
     * @param ctx    current tick prediction context
     */
    private void handleHoverDetection(WindfallPlayer player, PlayerState state, PredictionContext ctx) {
        double yMoved = Math.abs(ctx.deltaY);

        if (yMoved < HOVER_DELTA_THRESHOLD && !ctx.inWater && !ctx.inLava && !ctx.climbing) {
            state.hoverTicks++;
            if (state.hoverTicks > HOVER_TICK_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                    state.hoverTicks = 0;
                }
            }
        } else {
            state.hoverTicks = Math.max(0, state.hoverTicks - 1);
        }
    }

    /**
     * Detects no-fall: falling with significant velocity while simultaneously claiming on-ground.
     *
     * <p>This sub-check catches clients that spoof the on-ground flag to prevent fall damage
     * while still falling through the air. Uses a simple velocity + distance threshold.
     *
     * @param player       the player being checked
     * @param currentOnGround whether the player claims to be on the ground this tick
     * @param deltaY       current vertical velocity (negative = falling)
     * @param lastY        previous tick Y position
     * @param currentY     current tick Y position
     */
    private void handleNoFall(WindfallPlayer player, boolean currentOnGround, double deltaY,
                              double lastY, double currentY) {
        if (!currentOnGround && deltaY < -NO_FALL_VELOCITY_THRESHOLD) {
            double fallDistance = lastY - currentY;
            if (fallDistance > NO_FALL_DISTANCE) {
                if (currentOnGround) {
                    flagWithSetback(player);
                } else {
                    flag(player);
                }
            }
        }
    }
}
