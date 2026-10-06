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
import io.windfall.anticheat.core.util.MaterialUtils;
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
 *   <li><b>NoFall fallback</b>: Flags a claimed on-ground state during a fall of more than
 *       {@value NO_FALL_DISTANCE} blocks when the server-side probe finds no ground beneath the
 *       player. Falls that are simply long are not violations — only the false ground claim is.</li>
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
    /**
     * Consecutive ticks an impossible on-ground claim must persist before flagging.
     *
     * <p>A lagging server can report the player a fraction above the block they have already
     * touched down on, so a single mismatching tick proves nothing.</p>
     */
    private static final int NO_FALL_STRIKES = 3;

    private static final class PlayerState {
        double expectedDeltaY;
        int hoverTicks;
        /** Y position where the current fall started, or NaN when not falling. */
        double fallStartY;
        boolean falling;
        /** Ticks in a row the client claimed ground while the ground probe disagreed. */
        int noFallStrikes;
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

        /* Run the no-fall sub-check before the on-ground reset. A no-fall hack claims on-ground
         * while still descending, so it has to be evaluated on the very tick the claim appears —
         * returning early on on-ground first made flagWithSetback unreachable. */
        handleNoFall(player, state, currentOnGround, deltaY, ctx.lastY, ctx.y);

        /** Reset state when the player touches the ground */
        if (currentOnGround) {
            state.expectedDeltaY = 0;
            state.hoverTicks = 0;
            state.falling = false;
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
     * Detects no-fall: a client claiming to be on the ground while it is still falling.
     *
     * <p>The spoof is the signal, not the fall itself. A violation therefore needs all three
     * of:</p>
     * <ol>
     *   <li>the client claiming on-ground this tick,</li>
     *   <li>with more than {@value NO_FALL_DISTANCE} blocks of fall accumulated since the
     *       descent began and velocity beyond {@value NO_FALL_VELOCITY_THRESHOLD}, and</li>
     *   <li>the server-side ground probe finding no block at or under the player's feet.</li>
     * </ol>
     *
     * <p>Condition 3 is what keeps ordinary landings out: the tick a player touches down meets
     * the first two conditions as well, but the block beneath the feet is solid, so nothing is
     * recorded. Falling a long way in open air is never flagged on its own either — only the
     * false ground claim is. This complements {@link NoFallCheck}, which uses a per-tick
     * distance and so misses a sustained spoof below its velocity threshold; this sub-check
     * accumulates instead. Both require consecutive ticks before flagging, because a lagging
     * server can briefly hold the player above a block the client has already landed on.</p>
     *
     * @param player          the player being checked
     * @param state           mutable per-player state holding the fall origin
     * @param currentOnGround whether the player claims to be on the ground this tick
     * @param deltaY          current vertical velocity (negative = falling)
     * @param lastY           previous tick Y position
     * @param currentY        current tick Y position
     */
    private void handleNoFall(WindfallPlayer player, PlayerState state, boolean currentOnGround,
                              double deltaY, double lastY, double currentY) {
        boolean descending = deltaY < -NO_FALL_VELOCITY_THRESHOLD;

        if (descending && !state.falling) {
            state.falling = true;
            state.fallStartY = lastY;
        }

        if (!state.falling) {
            if (currentOnGround) state.falling = false;
            return;
        }

        double fallDistance = state.fallStartY - currentY;

        if (currentOnGround) {
            if (fallDistance > NO_FALL_DISTANCE && descending && !hasGroundBeneath(player)) {
                /* The claim is false, so the descent is not over: keep fallStartY where it is
                 * and do not clear `falling`, otherwise this branch would restart the origin on
                 * the next tick and a buffer larger than one tick could never be reached. */
                if (++state.noFallStrikes >= NO_FALL_STRIKES) {
                    state.noFallStrikes = 0;
                    state.falling = false;
                    flagWithSetback(player);
                }
            } else {
                /* Ground claim the probe agrees with — this is a real landing. */
                state.noFallStrikes = 0;
                state.falling = false;
            }
        } else {
            state.noFallStrikes = 0;
        }
    }

    /**
     * Server-side ground probe: is there a block the player could actually be standing on?
     *
     * <p>Reads the block the feet occupy and the one directly below it, so a slab or fence
     * filling the feet block counts as ground. An unloaded chunk, offline player, vehicle,
     * swimmer or flying player all report {@code true} — an unknown state must never produce
     * a flag, only a confirmed absence of ground may.</p>
     *
     * @param player the player being probed
     * @return true if ground is present or the world cannot answer
     */
    private boolean hasGroundBeneath(WindfallPlayer player) {
        try {
            org.bukkit.entity.Player bukkit = player.getPlayer();
            if (bukkit == null || !bukkit.isOnline()) return true;
            if (bukkit.isInsideVehicle() || PredictionEngine.checkInWater(player) || bukkit.isFlying()) return true;

            org.bukkit.World world = bukkit.getWorld();
            int bx = (int) Math.floor(player.getX());
            int by = (int) Math.floor(player.getY());
            int bz = (int) Math.floor(player.getZ());

            if (!world.isChunkLoaded(bx >> 4, bz >> 4)) return true;

            return !MaterialUtils.isAirLike(world.getBlockAt(bx, by, bz).getType())
                || !MaterialUtils.isAirLike(world.getBlockAt(bx, by - 1, bz).getType());
        } catch (Exception e) {
            return true;
        }
    }
}
