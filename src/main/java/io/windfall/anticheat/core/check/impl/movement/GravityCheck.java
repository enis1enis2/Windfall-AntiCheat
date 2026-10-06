package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects modified gravity by measuring per-tick vertical acceleration.
 *
 * <p>Most flight cheats are caught by comparing absolute fall speed against
 * {@link FlightCheck} or {@link NoFallCheck}. Gravity cheats work differently: the client keeps
 * the fall arc shape but scales it, so absolute speed at any instant looks plausible while the
 * second difference diverges every tick.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>On each airborne movement packet, predict the next deltaY from the previous one using
 *       {@link PredictionEngine#predictDeltaY}, which applies the correct gravity, drag, and
 *       status-effect modifiers for the player's environment.</li>
 *   <li>Compute the observed acceleration as {@code actualDeltaY - previousDeltaY} and the
 *       predicted acceleration the same way.</li>
 *   <li>Flag when the gap between them persists. A single divergent tick is normal — server
 *       tick timing, effect expiry mid-fall, elytra transitions — so violations must
 *       accumulate across {@value #MIN_CONSECUTIVE} consecutive ticks.</li>
 * </ol>
 *
 * <p>The acceleration form is what separates this from a speed check: a client applying
 * "gravity x1.5" produces an acceleration error of roughly {@code 0.5 * 0.08 * 0.98 = 0.039}
 * per tick, sustained, which no amount of timing jitter reproduces.</p>
 *
 * <p>Skipped entirely while any status effect that legitimately rewrites gravity is active
 * (levitation, slow falling), in fluids where the vertical model differs, while climbing or
 * honey, and during elytra or riptide movement. Those states are already covered by their own
 * checks and are the main false-positive source for acceleration-based detection.</p>
 *
 * @see FlightCheck — absolute vertical deviation
 * @see NoFallCheck — fall distance and ground claims
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Gravity", stableKey = "windfall.movement.gravity", decay = 0.02, setbackVl = 15,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class GravityCheck extends Check implements PacketCheck {

    /** Maximum allowed difference between observed and predicted acceleration, in blocks/tick². */
    private static final double ACCELERATION_TOLERANCE = 0.02;

    /** Consecutive divergent ticks required before flagging. */
    private static final int MIN_CONSECUTIVE = 4;

    /** Buffer threshold. */
    private static final double BUFFER_THRESHOLD = 3.0;

    /** Vertical speed above which the player is considered terminal and is skipped. */
    private static final double TERMINAL_SPEED = -3.92;

    /** Per-player mutable state. */
    private static final class PlayerState {
        double lastDeltaY;
        boolean primed;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        PlayerState state = getState(player);

        /* Only airborne falling motion is governed by gravity. */
        if (player.isOnGround()) {
            state.primed = false;
            reward(player);
            return;
        }

        double deltaY = player.getDeltaY();

        /* Skip states where gravity is legitimately rewritten or the model differs. */
        if (shouldSkip(player, deltaY)) {
            state.primed = false;
            reward(player);
            return;
        }

        if (!state.primed) {
            state.lastDeltaY = deltaY;
            state.primed = true;
            reward(player);
            return;
        }

        double predictedDeltaY = predictNext(player, state.lastDeltaY);

        double observedAcceleration = deltaY - state.lastDeltaY;
        double predictedAcceleration = predictedDeltaY - state.lastDeltaY;
        double deviation = Math.abs(observedAcceleration - predictedAcceleration);

        state.lastDeltaY = deltaY;

        if (deviation > ACCELERATION_TOLERANCE) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            reward(player);
        }
    }

    /**
     * Predicts the next deltaY for the player's current environment.
     *
     * @param player the falling player
     * @param currentDeltaY the current vertical velocity
     * @return the predicted next vertical velocity
     */
    private double predictNext(WindfallPlayer player, double currentDeltaY) {
        return PredictionEngine.predictDeltaY(
            currentDeltaY,
            PredictionEngine.checkInWater(player),
            PredictionEngine.checkInLava(player),
            player.isClimbing(),
            PredictionEngine.checkOnHoney(player),
            PredictionEngine.checkSlowFalling(player),
            PredictionEngine.checkLevitation(player),
            PredictionEngine.getLevitationAmplifier(player),
            PredictionEngine.checkFallFlying(player),
            PredictionEngine.checkRiptiding(player));
    }

    /**
     * Determines whether the current tick falls outside the gravity model's domain.
     *
     * @param player the player to test
     * @param deltaY current vertical velocity
     * @return true if this tick must be skipped
     */
    static boolean shouldSkip(WindfallPlayer player, double deltaY) {
        if (player.isSwimming() || player.isClimbing()) return true;
        if (PredictionEngine.checkInWater(player) || PredictionEngine.checkInLava(player)) return true;
        if (PredictionEngine.checkOnHoney(player)) return true;
        if (PredictionEngine.checkSlowFalling(player)) return true;
        if (PredictionEngine.checkLevitation(player)) return true;
        if (player.isGliding()) return true;
        if (PredictionEngine.checkRiptiding(player)) return true;
        /* Terminal velocity is clamped by the client and does not follow the free-fall arc. */
        return deltaY <= TERMINAL_SPEED;
    }

    /**
     * Expected acceleration in blocks per tick squared for free fall.
     *
     * <p>Vanilla applies {@code deltaY = (deltaY - 0.08) * 0.98} each tick, so the constant
     * acceleration term is {@code -0.08 * 0.98 = -0.0784}. Exposed for tests and tuning.</p>
     *
     * @return the expected free-fall acceleration
     */
    static double freeFallAcceleration() {
        return -PhysicsConstants.GRAVITY * PhysicsConstants.AIR_DRAG;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}