package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Speed A", stableKey = "windfall.movement.speed", decay = 0.01, setbackVl = 20)
public class SpeedCheck extends Check implements PacketCheck {

    private static final double BASE_WALK_SPEED = 0.102;
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double SNEAK_MULTIPLIER = 0.3;
    private static final double GROUND_FRICTION = 0.91;
    private static final double GROUND_ACCEL_FACTOR = 0.16277136;
    private static final double AIR_ACCEL_FACTOR = 0.026;
    private static final double AIR_FRICTION = 0.91;
    private static final double SPEED_TOLERANCE = 1.05;
    private static final double PRE_1_18_2_THRESHOLD = 0.03;
    private static final double MIN_SPEED_FLAG_BUFFER = 3.0;

    private static final double SPEED_POTION_MULT = 0.20;
    private static final double SLOWNESS_POTION_MULT = 0.15;
    private static final int SPEED_POTION_MAX_LEVEL = 5;
    private static final int SLOWNESS_POTION_MAX_LEVEL = 4;

    private static final double ICE_FRICTION = 0.98;
    private static final double SOUL_SAND_FRICTION = 0.6;
    private static final double HONEY_FRICTION = 0.4;

    private double maxObservedSpeed;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double actualSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (actualSpeed > maxObservedSpeed) {
            maxObservedSpeed = actualSpeed;
        }

        if (actualSpeed < PRE_1_18_2_THRESHOLD && player.getProtocolVersion() < 757) {
            decreaseBuffer(player, 0.1);
            return;
        }

        if (actualSpeed < 0.005) {
            decreaseBuffer(player, 0.05);
            return;
        }

        double lastHorizontalSpeed = Math.sqrt(
                (player.getLastX() - player.getLastLastX()) * (player.getLastX() - player.getLastLastX())
                + (player.getLastZ() - player.getLastLastZ()) * (player.getLastZ() - player.getLastLastZ()));

        double baseSpeed = calculateBaseSpeed(player);
        double maxSpeed = calculateMaxHorizontalSpeed(baseSpeed, lastHorizontalSpeed, player);

        if (actualSpeed > maxSpeed * SPEED_TOLERANCE) {
            double exceedRatio = actualSpeed / maxSpeed;
            if (exceedRatio > 2.0) {
                flag(player);
                resetBuffer(player);
            } else {
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

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double calculateBaseSpeed(WindfallPlayer player) {
        double speed = BASE_WALK_SPEED;

        if (player.isSprinting()) {
            speed *= SPRINT_MULTIPLIER;
        }
        if (player.isSneaking()) {
            speed *= SNEAK_MULTIPLIER;
        }

        speed *= getSpeedPotionMultiplier(player);
        speed *= getSlownessPotionMultiplier(player);

        return speed;
    }

    private double calculateMaxHorizontalSpeed(double baseSpeed, double lastHorizontalSpeed, WindfallPlayer player) {
        boolean onGround = player.isOnGround();
        boolean inWeb = player.isClimbing() && !player.isSwimming();
        double friction;

        if (onGround) {
            friction = getBlockFriction(player);
        } else {
            friction = AIR_FRICTION;
        }

        if (inWeb) {
            friction = Math.min(friction, 0.25);
        }

        double maxAccel;
        if (onGround) {
            double accelFactor = GROUND_ACCEL_FACTOR / (friction * friction * friction);
            maxAccel = baseSpeed * accelFactor;
        } else {
            maxAccel = baseSpeed * AIR_ACCEL_FACTOR;
        }

        if (player.isSwimming()) {
            maxAccel *= 0.9;
        }

        double maxSpeed = lastHorizontalSpeed * friction + maxAccel;

        if (player.isSwimming() && player.getProtocolVersion() >= 393) {
            double swimBoost = 0.01 * Math.max(0, player.getDeltaY());
            maxSpeed += swimBoost;
        }

        return maxSpeed;
    }

    private double getBlockFriction(WindfallPlayer player) {
        return GROUND_FRICTION;
    }

    private double getSpeedPotionMultiplier(WindfallPlayer player) {
        try {
            java.util.Collection<org.bukkit.potion.PotionEffect> potionEffects = player.getPlayer().getActivePotionEffects();
            for (org.bukkit.potion.PotionEffect effect : potionEffects) {
                if (effect.getType().getName().equals("SPEED")) {
                    int amplifier = effect.getAmplifier();
                    int level = Math.min(amplifier + 1, SPEED_POTION_MAX_LEVEL);
                    return 1.0 + (SPEED_POTION_MULT * level);
                }
            }
        } catch (Exception ignored) {
        }
        return 1.0;
    }

    private double getSlownessPotionMultiplier(WindfallPlayer player) {
        try {
            java.util.Collection<org.bukkit.potion.PotionEffect> potionEffects = player.getPlayer().getActivePotionEffects();
            for (org.bukkit.potion.PotionEffect effect : potionEffects) {
                String typeName = effect.getType().getName();
                if (typeName.equals("SLOW") || typeName.equals("SLOWNESS")) {
                    int amplifier = effect.getAmplifier();
                    int level = Math.min(amplifier + 1, SLOWNESS_POTION_MAX_LEVEL);
                    return 1.0 - (SLOWNESS_POTION_MULT * level);
                }
            }
        } catch (Exception ignored) {
        }
        return 1.0;
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    public double getMaxObservedSpeed() {
        return maxObservedSpeed;
    }
}
