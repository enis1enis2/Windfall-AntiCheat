package io.windfall.anticheat.core.physics;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Stateless utility methods for movement prediction shared across all movement checks.
 *
 * <p>This is the core physics engine — it implements Minecraft's movement algorithm:
 * <ol>
 *   <li>Calculate base speed from sprint/crouch/potion state</li>
 *   <li>Apply block friction (ground) or air drag (airborne)</li>
 *   <li>Add player input acceleration (capped by base speed)</li>
 *   <li>Predict vertical motion (gravity, jump, swim, water, lava, levitation)</li>
 *   <li>Compare predicted position against reported position</li>
 * </ol>
 *
 * <p>All constants sourced from {@link PhysicsConstants} and {@link VersionPhysics}.
 * All methods are static — no instance state is held.
 *
 * <p>The engine supports two horizontal speed prediction modes:
 * <ul>
 *   <li><b>Without deltaY</b>: swim boost is ignored (baseline prediction)</li>
 *   <li><b>With deltaY</b>: swim boost uses vertical velocity (for swim-check specific logic)</li>
 * </ul>
 *
 * @see PredictionContext for the pre-computed snapshot that feeds into these methods
 * @see io.windfall.anticheat.core.check.impl.movement.SpeedCheck for primary consumer
 * @see io.windfall.anticheat.core.check.impl.movement.FlightCheck for vertical consumer
 */
// Stateless utility methods for movement prediction shared across all movement checks.
// All constants sourced from PhysicsConstants and VersionPhysics.
public final class PredictionEngine {

    // Movement gravity constants
    private static final double GRAVITY_SLOW_FALLING = 0.01;
    private static final double WATER_GRAVITY_OFFSET = 0.02;
    private static final double LAVA_GRAVITY_OFFSET = 0.02;
    private static final double LEVITATION_STRENGTH = 0.05;

    // Movement drag constants
    private static final double WATER_DRAG_VERTICAL = 0.8;
    private static final double LAVA_DRAG_VERTICAL = 0.5;

    // Movement bounds
    private static final double HONEY_MAX_DELTA_Y = -0.5;
    private static final double CLIMB_MAX_DELTA_Y = 0.15;

    // Potion effect names
    private static final String POTION_SPEED = "SPEED";
    private static final String POTION_SLOWNESS_OLD = "SLOW";
    private static final String POTION_SLOWNESS = "SLOWNESS";
    private static final String POTION_SLOW_FALLING = "SLOW_FALLING";
    private static final String POTION_LEVITATION = "LEVITATION";

    private static final int SPEED_POTION_MAX_LEVEL = 5;
    private static final int SLOWNESS_POTION_MAX_LEVEL = 4;
    private static final double SPEED_POTION_MULT = 0.20;
    private static final double SLOWNESS_POTION_MULT = 0.15;

    // Air acceleration factors
    private static final double GROUND_ACCEL_FACTOR = 0.16277136;
    private static final double AIR_ACCEL_FACTOR = 0.026;

    private PredictionEngine() {}

    // === PACKET DETECTION ===

    /**
     * Determines if an incoming packet carries position data.
     * Movement checks only fire for these packet types — rotation-only and
     * keep-alive packets are ignored by the check system.
     */
    public static boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    // === HORIZONTAL SPEED ===

    /**
     * Euclidean horizontal speed from position deltas.
     * Formula: sqrt(deltaX² + deltaZ²)
     */
    public static double calculateHorizontalSpeed(double deltaX, double deltaZ) {
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    /**
     * Calculates the base movement speed before friction and acceleration.
     * Order of operations matters: sprint → crouch → speed potion → slowness potion.
     *
     * @param sprinting true if player is sprinting (×1.3)
     * @param sneaking true if player is sneaking (×0.3)
     * @param speedMultiplier speed potion multiplier (1.0 if no effect)
     * @param slownessMultiplier slowness potion multiplier (1.0 if no effect)
     * @return base speed in blocks/tick
     */
    public static double calculateBaseSpeed(boolean sprinting, boolean sneaking,
                                             double speedMultiplier, double slownessMultiplier) {
        double speed = PhysicsConstants.PLAYER_WALK_SPEED;
        if (sprinting) speed *= PhysicsConstants.PLAYER_SPRINT_MULTIPLIER;
        if (sneaking) speed *= PhysicsConstants.PLAYER_CROUCH_MULTIPLIER;
        speed *= speedMultiplier;
        speed *= slownessMultiplier;
        return speed;
    }

    /**
     * Predicts the maximum horizontal speed for this tick (without swim boost).
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Determine friction: ground (0.91) or air (0.98), min with web (0.25) if climbing</li>
     *   <li>Calculate max acceleration: ground uses 0.16277136/friction³, air uses 0.026</li>
     *   <li>Swimming reduces acceleration by 10%</li>
     *   <li>Max speed = lastSpeed × friction + acceleration</li>
     * </ol>
     *
     * <p>The friction³ term for ground acceleration is the key equation:
     * {@code accelFactor = 0.16277136 / (friction × friction × friction)}
     * This ensures players can reach their base speed on normal ground but are
     * significantly slower on soul sand, ice, etc.
     */
    public static double calculateMaxHorizontalSpeed(double baseSpeed, double lastHorizontalSpeed,
                                                      boolean onGround, boolean climbing,
                                                      boolean swimming, int protocol) {
        boolean inWeb = climbing && !swimming;
        double friction;

        if (onGround) {
            friction = PhysicsConstants.GROUND_FRICTION;
        } else {
            friction = PhysicsConstants.AIR_DRAG;
        }
        if (inWeb) friction = Math.min(friction, PhysicsConstants.WEB_FRICTION);

        double maxAccel;
        if (onGround) {
            double accelFactor = GROUND_ACCEL_FACTOR / (friction * friction * friction);
            maxAccel = baseSpeed * accelFactor;
        } else {
            maxAccel = baseSpeed * AIR_ACCEL_FACTOR;
        }

        if (swimming) maxAccel *= 0.9;

        double maxSpeed = lastHorizontalSpeed * friction + maxAccel;

        return maxSpeed;
    }

    /**
     * Overload that includes deltaY for swim boost calculation.
     * Swim boost adds 0.01 × max(0, deltaY) to max speed when in water (1.13+).
     * Only meaningful for SwimCheck — other checks use the base overload.
     */
    public static double calculateMaxHorizontalSpeed(double baseSpeed, double lastHorizontalSpeed,
                                                      boolean onGround, boolean climbing,
                                                      boolean swimming, int protocol, double deltaY) {
        boolean inWeb = climbing && !swimming;
        double friction;

        if (onGround) {
            friction = PhysicsConstants.GROUND_FRICTION;
        } else {
            friction = PhysicsConstants.AIR_DRAG;
        }
        if (inWeb) friction = Math.min(friction, PhysicsConstants.WEB_FRICTION);

        double maxAccel;
        if (onGround) {
            double accelFactor = GROUND_ACCEL_FACTOR / (friction * friction * friction);
            maxAccel = baseSpeed * accelFactor;
        } else {
            maxAccel = baseSpeed * AIR_ACCEL_FACTOR;
        }

        if (swimming) maxAccel *= 0.9;

        double maxSpeed = lastHorizontalSpeed * friction + maxAccel;

        if (swimming && protocol >= 393) {
            double swimBoost = 0.01 * Math.max(0, deltaY);
            maxSpeed += swimBoost;
        }
        return maxSpeed;
    }

    // === VERTICAL PREDICTION ===

    /**
     * Predicts the next tick's vertical velocity (deltaY) based on current state.
     *
     * <p>Priority chain (first match wins):
     * <ol>
     *   <li>Water: drag × 0.8 - 0.02</li>
     *   <li>Lava: drag × 0.5 - 0.02</li>
     *   <li>Climbing (ladder/vine): capped at 0.15</li>
     *   <li>Honey: capped at -0.5 (prevents fast falling through honey)</li>
     *   <li>Fall flying / Riptide: unchanged (physics handled client-side)</li>
     *   <li>Levitation: add 0.05 × amplifier</li>
     *   <li>Default: (deltaY - gravity) × 0.98</li>
     * </ol>
     *
     * <p>Slow Falling overrides gravity (0.01 instead of 0.08) in the default case.
     */
    public static double predictDeltaY(double currentExpectedDeltaY, boolean inWater, boolean inLava,
                                        boolean climbing, boolean onHoney, boolean hasSlowFalling,
                                        boolean hasLevitation, double levitationAmplifier,
                                        boolean isFallFlying, boolean hasRiptide) {
        double gravity = hasSlowFalling ? GRAVITY_SLOW_FALLING : PhysicsConstants.GRAVITY;

        if (inWater) {
            return currentExpectedDeltaY * WATER_DRAG_VERTICAL - WATER_GRAVITY_OFFSET;
        } else if (inLava) {
            return currentExpectedDeltaY * LAVA_DRAG_VERTICAL - LAVA_GRAVITY_OFFSET;
        } else if (climbing) {
            double result = currentExpectedDeltaY;
            if (currentExpectedDeltaY > CLIMB_MAX_DELTA_Y) result = CLIMB_MAX_DELTA_Y;
            return result;
        } else if (onHoney) {
            return Math.max(currentExpectedDeltaY, HONEY_MAX_DELTA_Y);
        } else if (isFallFlying || hasRiptide) {
            return currentExpectedDeltaY;
        } else if (hasLevitation) {
            return currentExpectedDeltaY + LEVITATION_STRENGTH * levitationAmplifier;
        } else {
            return (currentExpectedDeltaY - gravity) * PhysicsConstants.AIR_DRAG;
        }
    }

    // === FLUID DETECTION ===

    /** Checks if the player's feet are in a water-type block (water, waterlogged) */
    public static boolean checkInWater(WindfallPlayer player) {
        return player.isCachedInWater();
    }

    /** Checks if the player's feet are in a lava-type block */
    public static boolean checkInLava(WindfallPlayer player) {
        return player.isCachedInLava();
    }

    /** Checks if the block 0.1 below the player's feet is a honey block */
    public static boolean checkOnHoney(WindfallPlayer player) {
        return player.isCachedOnHoney();
    }

    // === POTION EFFECTS ===

    /**
     * Speed potion multiplier: 1.0 + 0.2 × level.
     * Caps at level V (5) to match vanilla behaviour.
     */
    public static double getSpeedPotionMultiplier(WindfallPlayer player) {
        return player.getCachedSpeedMultiplier();
    }

    /**
     * Slowness potion multiplier: 1.0 - 0.15 × level.
     * Caps at level IV (4). Uses "SLOW" substring to match both old ("SLOW") and new ("SLOWNESS") names.
     */
    public static double getSlownessPotionMultiplier(WindfallPlayer player) {
        return player.getCachedSlownessMultiplier();
    }

    /** Checks if the player has the Slow Falling potion effect active */
    public static boolean checkSlowFalling(WindfallPlayer player) {
        return player.isCachedHasSlowFalling();
    }

    /** Checks if the player has the Levitation potion effect active */
    public static boolean checkLevitation(WindfallPlayer player) {
        return player.isCachedHasLevitation();
    }

    /** Returns the levitation amplifier (1-based) for the upward boost calculation */
    public static double getLevitationAmplifier(WindfallPlayer player) {
        return player.getCachedLevitationAmplifier();
    }

    // === ENTITY STATE ===

    /**
     * Checks if the player is using a Riptide trident via cached state.
     * Cached on main thread to avoid reflection from Netty threads.
     */
    public static boolean checkRiptiding(WindfallPlayer player) {
        return player.isCachedHasRiptide();
    }

    /**
     * Checks if the player is gliding with an elytra via cached state.
     * Cached on main thread to avoid reflection from Netty threads.
     */
    public static boolean checkFallFlying(WindfallPlayer player) {
        return player.isCachedIsFallFlying();
    }
}
