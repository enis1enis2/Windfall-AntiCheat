package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Fly A", stableKey = "windfall.movement.fly", decay = 0.01, setbackVl = 15)
public class FlightCheck extends Check implements PacketCheck {

    private static final double GRAVITY_DEFAULT = 0.08;
    private static final double GRAVITY_SLOW_FALLING = 0.01;
    private static final double AIR_DRAG = 0.98;
    private static final double JUMP_MOMENTUM = 0.42;

    private static final double WATER_DRAG_VERTICAL = 0.8;
    private static final double WATER_GRAVITY_OFFSET = 0.02;
    private static final double LAVA_DRAG_VERTICAL = 0.5;
    private static final double LAVA_GRAVITY_OFFSET = 0.02;

    private static final double WEB_VERTICAL_FACTOR = 0.25;
    private static final double HONEY_MAX_DELTA_Y = -0.5;
    private static final double CLIMB_MAX_DELTA_Y = 0.15;

    private static final double VERTICAL_TOLERANCE = 0.05;
    private static final int HOVER_TICK_THRESHOLD = 20;
    private static final double HOVER_DELTA_THRESHOLD = 0.005;
    private static final double NO_FALL_VELOCITY_THRESHOLD = 0.5;
    private static final double NO_FALL_DISTANCE = 3.0;

    private static final double LEVITATION_STRENGTH = 0.05;

    private double expectedDeltaY;
    private int hoverTicks;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        boolean currentOnGround = player.isOnGround();
        boolean lastGround = player.isLastOnGround();
        double deltaY = player.getDeltaY();
        double y = player.getY();
        double lastY = player.getLastY();

        boolean inWater = checkInWater(player);
        boolean inLava = checkInLava(player);
        boolean onHoney = checkOnHoney(player);
        boolean climbing = player.isClimbing();
        boolean hasSlowFalling = checkSlowFalling(player);
        boolean hasLevitation = checkLevitation(player);
        boolean hasRiptide = checkRiptiding(player);
        boolean isFallFlying = checkFallFlying(player);

        if (currentOnGround) {
            expectedDeltaY = 0;
            hoverTicks = 0;
            return;
        }

        if (lastGround && !currentOnGround) {
            if (deltaY >= JUMP_MOMENTUM - 0.01 && deltaY <= JUMP_MOMENTUM + 0.15) {
                expectedDeltaY = JUMP_MOMENTUM;
            } else if (Math.abs(deltaY) < 0.01) {
                expectedDeltaY = 0;
            }
        }

        double gravity = hasSlowFalling ? GRAVITY_SLOW_FALLING : GRAVITY_DEFAULT;

        double predictedDeltaY;
        if (inWater) {
            predictedDeltaY = expectedDeltaY * WATER_DRAG_VERTICAL - WATER_GRAVITY_OFFSET;
        } else if (inLava) {
            predictedDeltaY = expectedDeltaY * LAVA_DRAG_VERTICAL - LAVA_GRAVITY_OFFSET;
        } else if (climbing) {
            predictedDeltaY = deltaY;
            if (deltaY > CLIMB_MAX_DELTA_Y) {
                predictedDeltaY = CLIMB_MAX_DELTA_Y;
            }
        } else if (onHoney) {
            predictedDeltaY = Math.max(expectedDeltaY, HONEY_MAX_DELTA_Y);
        } else if (isFallFlying || hasRiptide) {
            predictedDeltaY = expectedDeltaY;
        } else if (hasLevitation) {
            double levitationAmplifier = getLevitationAmplifier(player);
            predictedDeltaY = expectedDeltaY + LEVITATION_STRENGTH * levitationAmplifier;
        } else {
            predictedDeltaY = (expectedDeltaY - gravity) * AIR_DRAG;
        }

        double verticalDelta = deltaY - predictedDeltaY;
        boolean verticalDeviation = Math.abs(verticalDelta) > VERTICAL_TOLERANCE
                && Math.abs(deltaY) > 0.01;

        if (verticalDeviation && !isFallFlying && !hasRiptide && !hasLevitation) {
            handleHoverDetection(player, deltaY);

            if (deltaY > 0 && expectedDeltaY <= 0 && !hasLevitation && !hasRiptide && !isFallFlying) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                double deviationRatio = Math.abs(verticalDelta) / Math.max(Math.abs(predictedDeltaY), 0.001);
                if (deviationRatio > 2.0) {
                    flag(player);
                    resetBuffer(player);
                } else {
                    increaseBuffer(player, 0.3 * Math.min(deviationRatio, 2.0));
                    if (getBuffer(player) > 5.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
            hoverTicks = Math.max(0, hoverTicks - 1);
        }

        handleNoFall(player, currentOnGround, deltaY, lastY, y);

        expectedDeltaY = deltaY;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleHoverDetection(WindfallPlayer player, double deltaY) {
        double yMoved = Math.abs(deltaY);
        boolean inWater = checkInWater(player);
        boolean inLava = checkInLava(player);
        boolean climbing = player.isClimbing();

        if (yMoved < HOVER_DELTA_THRESHOLD && !inWater && !inLava && !climbing) {
            hoverTicks++;
            if (hoverTicks > HOVER_TICK_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                    hoverTicks = 0;
                }
            }
        } else {
            hoverTicks = Math.max(0, hoverTicks - 1);
        }
    }

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

    private double getLevitationAmplifier(WindfallPlayer player) {
        try {
            java.util.Collection<org.bukkit.potion.PotionEffect> effects =
                player.getPlayer().getActivePotionEffects();
            for (org.bukkit.potion.PotionEffect effect : effects) {
                if (effect.getType().getName().equals("LEVITATION")) {
                    return effect.getAmplifier() + 1;
                }
            }
        } catch (Exception ignored) {
        }
        return 1.0;
    }

    private boolean checkInWater(WindfallPlayer player) {
        try {
            int protocol = player.getProtocolVersion();
            org.bukkit.Material mat = player.getPlayer().getLocation().getBlock().getType();
            if (VersionPhysics.hasNewFluidSystem(protocol)) {
                return mat == org.bukkit.Material.WATER;
            }
            String name = mat.name();
            return name.contains("STATIONARY_WATER") || name.equals("WATER");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkInLava(WindfallPlayer player) {
        try {
            int protocol = player.getProtocolVersion();
            org.bukkit.Material mat = player.getPlayer().getLocation().getBlock().getType();
            if (VersionPhysics.hasNewFluidSystem(protocol)) {
                return mat == org.bukkit.Material.LAVA;
            }
            String name = mat.name();
            return name.contains("STATIONARY_LAVA") || name.equals("LAVA");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkOnHoney(WindfallPlayer player) {
        try {
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial("HONEY_BLOCK");
            if (mat == null) return false;
            return player.getPlayer().getLocation().subtract(0, 0.1, 0).getBlock().getType() == mat;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkSlowFalling(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType().getName().equals("SLOW_FALLING")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // SLOW_FALLING doesn't exist on this version
        }
        return false;
    }

    private boolean checkLevitation(WindfallPlayer player) {
        try {
            for (org.bukkit.potion.PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType().getName().equals("LEVITATION")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // LEVITATION doesn't exist on this version
        }
        return false;
    }

    private boolean checkRiptiding(WindfallPlayer player) {
        try {
            java.lang.reflect.Method m = player.getPlayer().getClass().getMethod("isRiptiding");
            return (Boolean) m.invoke(player.getPlayer());
        } catch (Exception e) {
            // isRiptiding() doesn't exist before 1.13
            return false;
        }
    }

    private boolean checkFallFlying(WindfallPlayer player) {
        try {
            java.lang.reflect.Method m = player.getPlayer().getClass().getMethod("isGliding");
            return (Boolean) m.invoke(player.getPlayer());
        } catch (Exception e) {
            // isGliding() doesn't exist before 1.9
            return false;
        }
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
