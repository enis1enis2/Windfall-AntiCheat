package io.windfall.anticheat.core.physics;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.util.MaterialUtils;

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

    public static boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    // === HORIZONTAL SPEED ===

    public static double calculateHorizontalSpeed(double deltaX, double deltaZ) {
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    public static double calculateBaseSpeed(boolean sprinting, boolean sneaking,
                                             double speedMultiplier, double slownessMultiplier) {
        double speed = PhysicsConstants.PLAYER_WALK_SPEED;
        if (sprinting) speed *= PhysicsConstants.PLAYER_SPRINT_MULTIPLIER;
        if (sneaking) speed *= PhysicsConstants.PLAYER_CROUCH_MULTIPLIER;
        speed *= speedMultiplier;
        speed *= slownessMultiplier;
        return speed;
    }

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

        if (swimming && protocol >= 393) {
            double swimBoost = 0.01 * 0.0; // deltaY must be supplied by caller for swim boost
            maxSpeed += swimBoost;
        }
        return maxSpeed;
    }

    // Overload that includes deltaY for swim boost calculation
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

    public static boolean checkInWater(WindfallPlayer player) {
        try {
            org.bukkit.Material mat = player.getPlayer().getLocation().getBlock().getType();
            return MaterialUtils.isWater(mat);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkInLava(WindfallPlayer player) {
        try {
            org.bukkit.Material mat = player.getPlayer().getLocation().getBlock().getType();
            return MaterialUtils.isLava(mat);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkOnHoney(WindfallPlayer player) {
        try {
            org.bukkit.Material mat = player.getPlayer().getLocation().subtract(0, 0.1, 0).getBlock().getType();
            return MaterialUtils.isHoney(mat);
        } catch (Exception e) {
            return false;
        }
    }

    // === POTION EFFECTS ===

    public static double getSpeedPotionMultiplier(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType().getName().toUpperCase().contains(POTION_SPEED)) {
                    int level = Math.min(effect.getAmplifier() + 1, SPEED_POTION_MAX_LEVEL);
                    return 1.0 + (SPEED_POTION_MULT * level);
                }
            }
        } catch (Exception ignored) {}
        return 1.0;
    }

    public static double getSlownessPotionMultiplier(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                String typeName = effect.getType().getName().toUpperCase();
                if (typeName.contains(POTION_SLOWNESS_OLD)) {
                    int level = Math.min(effect.getAmplifier() + 1, SLOWNESS_POTION_MAX_LEVEL);
                    return 1.0 - (SLOWNESS_POTION_MULT * level);
                }
            }
        } catch (Exception ignored) {}
        return 1.0;
    }

    public static boolean checkSlowFalling(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType().getName().toUpperCase().contains(POTION_SLOW_FALLING)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean checkLevitation(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType().getName().toUpperCase().contains(POTION_LEVITATION)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static double getLevitationAmplifier(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType().getName().toUpperCase().contains(POTION_LEVITATION)) {
                    return effect.getAmplifier() + 1;
                }
            }
        } catch (Exception ignored) {}
        return 1.0;
    }

    // === ENTITY STATE ===

    public static boolean checkRiptiding(WindfallPlayer player) {
        try {
            java.lang.reflect.Method m = player.getPlayer().getClass().getMethod("isRiptiding");
            return (Boolean) m.invoke(player.getPlayer());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkFallFlying(WindfallPlayer player) {
        try {
            java.lang.reflect.Method m = player.getPlayer().getClass().getMethod("isGliding");
            return (Boolean) m.invoke(player.getPlayer());
        } catch (Exception e) {
            return false;
        }
    }
}
