package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@CheckData(name = "Bad Packets A", stableKey = "windfall.packet.bad", decay = 0.0, setbackVl = 5)
public class BadPacketsCheck extends Check implements PacketCheck {

    private static final double MAX_Y_MODERN = 400.0;
    private static final double MAX_Y_LEGACY = 256.0;
    private static final double MIN_Y_MODERN = -64.0;
    private static final double MIN_Y_LEGACY = 0.0;
    private static final int MAX_ATTACKS_PER_TICK = 20;
    private static final int DUPLICATE_THRESHOLD = 10;

    private int lastPacketTypeHash;
    private long lastMovementPacketTime;
    private long lastTransactionTime;
    private long lastAttackPacketTime;

    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private float lastRotYaw;
    private float lastRotPitch;

    private int duplicateCount;
    private int attackCountThisTick;
    private long currentTickStart;

    private boolean loggedIn;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();

        // Reset attack counter every tick for auto-clicker detection
        if (now - currentTickStart > 50) {
            attackCountThisTick = 0;
            currentTickStart = now;
        }

        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_ROTATION) {
            lastMovementPacketTime = now;

            // Movement before LOGIN_SUCCESS should never happen — protocol violation
            if (!loggedIn) {
                flagDetail(player, "movement before login complete");
                return;
            }
        }

        if (type == PacketType.Play.Client.PLAYER_POSITION) {
            handlePosition(player, event);
        } else if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handlePositionAndRotation(player, event);
        } else if (type == PacketType.Play.Client.PLAYER_ROTATION) {
            handleRotation(player, event);
        } else if (type == PacketType.Play.Client.PLAYER_FLYING) {
            handleFlying(player, event);
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteractEntity(player, event, now);
        }

        int typeHash = type.hashCode();
        if (typeHash == lastPacketTypeHash && isMovementType(type)) {
            // duplicate packet type in sequence is normal, but track it
        }
        lastPacketTypeHash = typeHash;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            lastTransactionTime = System.currentTimeMillis();
        }
    }

    public void onLoginComplete(WindfallPlayer player) {
        this.loggedIn = true;
    }

    public void onDisconnect(WindfallPlayer player) {
        this.loggedIn = false;
        resetState();
    }

    private void handlePosition(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientPlayerPosition wrapper = new WrapperPlayClientPlayerPosition(event);
        var pos = wrapper.getPosition();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        validateCoordinates(player, x, y, z);
        checkDuplicate(player, x, y, z, 0, 0);
    }

    private void handlePositionAndRotation(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
        var pos = wrapper.getPosition();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        validateCoordinates(player, x, y, z);
        validateRotation(player, yaw, pitch);
        checkDuplicate(player, x, y, z, pitch, yaw);
    }

    private void handleRotation(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        validateRotation(player, yaw, pitch);

        if (player.getHorizontalSpeed() > 0.15 && !player.isOnGround()) {
            double dx = player.getX() - player.getLastX();
            double dz = player.getZ() - player.getLastZ();
            double horizontalMovement = Math.sqrt(dx * dx + dz * dz);
            if (horizontalMovement > 0.5) {
                increaseBuffer(player, 0.2);
                if (getBuffer(player) > 2.0) {
                    flagDetail(player, "rotation-only during fast movement, position desync");
                    resetBuffer(player);
                }
            }
        }
    }

    private void handleFlying(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
        boolean onGround = wrapper.isOnGround();

        if (player.isFlying() && !player.isServerOnGround() && !onGround) {
            // flying packet while not on ground but player is in creative/spectator - allow
        }
    }

    private void handleInteractEntity(WindfallPlayer player, PacketReceiveEvent event, long now) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();

        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            attackCountThisTick++;
            lastAttackPacketTime = now;

            if (attackCountThisTick > MAX_ATTACKS_PER_TICK) {
                flagDetail(player, "auto-clicker detected: " + attackCountThisTick + " attacks/tick");
            }
        }
    }

    // NaN/Infinite coordinates cause server exceptions — kick immediately
    private void validateCoordinates(WindfallPlayer player, double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            flagDetail(player, "NaN/Infinite coordinates, kicking");
            player.getPlayer().kickPlayer("[Windfall] Invalid position data");
            return;
        }

        int protocol = player.getProtocolVersion();
        // 64 blocks above max height — tolerance for entity tracking packets
        double maxY = VersionPhysics.getMaxWorldHeight(protocol) + 64;
        double minY = VersionPhysics.getMinWorldHeight(protocol);

        if (y > maxY || y < minY) {
            flagDetail(player, "Y out of bounds: " + y);
        }
    }

    private void validateRotation(WindfallPlayer player, float yaw, float pitch) {
        if (Float.isNaN(yaw) || Float.isNaN(pitch)
                || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
            flagDetail(player, "NaN/Infinite rotation");
            return;
        }

        if (yaw < -180.0f || yaw > 180.0f) {
            flagDetail(player, "Yaw out of range: " + yaw);
        }

        if (pitch < -90.0f || pitch > 90.0f) {
            flagDetail(player, "Pitch out of range: " + pitch);
        }
    }

    // Epsilon-based duplicate detection catches position spam from hacked clients
    private void checkDuplicate(WindfallPlayer player, double x, double y, double z, float pitch, float yaw) {
        double epsilon = 0.00001;
        if (Math.abs(x - lastPosX) < epsilon
                && Math.abs(y - lastPosY) < epsilon
                && Math.abs(z - lastPosZ) < epsilon
                && Math.abs(yaw - lastRotYaw) < epsilon
                && Math.abs(pitch - lastRotPitch) < epsilon) {
            duplicateCount++;
            if (duplicateCount > DUPLICATE_THRESHOLD) {
                increaseBuffer(player, 0.1);
            }
        } else {
            duplicateCount = Math.max(0, duplicateCount - 1);
        }

        lastPosX = x;
        lastPosY = y;
        lastPosZ = z;
        lastRotYaw = yaw;
        lastRotPitch = pitch;
    }

    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }

    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
        logger.warning("[Bad Packets A] " + player.getName() + ": " + detail);
    }

    private void resetState() {
        lastPacketTypeHash = 0;
        lastMovementPacketTime = 0;
        lastTransactionTime = 0;
        lastAttackPacketTime = 0;
        duplicateCount = 0;
        attackCountThisTick = 0;
        currentTickStart = 0;
    }
}
