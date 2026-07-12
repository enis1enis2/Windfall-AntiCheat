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
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Bad Packets A", stableKey = "windfall.packet.bad", decay = 0.0, setbackVl = 5, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class BadPacketsCheck extends Check implements PacketCheck {

    private static final double MAX_Y_MODERN = 400.0;
    private static final double MAX_Y_LEGACY = 256.0;
    private static final double MIN_Y_MODERN = -64.0;
    private static final double MIN_Y_LEGACY = 0.0;
    private static final int MAX_ATTACKS_PER_TICK = 20;
    private static final int DUPLICATE_THRESHOLD = 10;

    private static final class PlayerState {
        int lastPacketTypeHash;
        long lastMovementPacketTime;
        long lastTransactionTime;
        long lastAttackPacketTime;
        double lastPosX;
        double lastPosY;
        double lastPosZ;
        float lastRotYaw;
        float lastRotPitch;
        int duplicateCount;
        int attackCountThisTick;
        long currentTickStart;
        boolean loggedIn;
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
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        if (now - state.currentTickStart > 50) {
            state.attackCountThisTick = 0;
            state.currentTickStart = now;
        }

        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_ROTATION) {
            state.lastMovementPacketTime = now;

            if (!state.loggedIn) {
                flagDetail(player, "movement before login complete");
                return;
            }
        }

        if (type == PacketType.Play.Client.PLAYER_POSITION) {
            handlePosition(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handlePositionAndRotation(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_ROTATION) {
            handleRotation(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_FLYING) {
            handleFlying(player, event);
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteractEntity(player, event, now, state);
        }

        int typeHash = type.hashCode();
        if (typeHash == state.lastPacketTypeHash && isMovementType(type)) {
            // duplicate packet type in sequence is normal, but track it
        }
        state.lastPacketTypeHash = typeHash;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            PlayerState state = getState(player);
            state.lastTransactionTime = System.currentTimeMillis();
        }
    }

    public void onLoginComplete(WindfallPlayer player) {
        getState(player).loggedIn = true;
    }

    public void onDisconnect(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.loggedIn = false;
        state.lastPacketTypeHash = 0;
        state.lastMovementPacketTime = 0;
        state.lastTransactionTime = 0;
        state.lastAttackPacketTime = 0;
        state.duplicateCount = 0;
        state.attackCountThisTick = 0;
        state.currentTickStart = 0;
    }

    private void handlePosition(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientPlayerPosition wrapper = new WrapperPlayClientPlayerPosition(event);
        var pos = wrapper.getPosition();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        validateCoordinates(player, x, y, z);
        checkDuplicate(player, x, y, z, 0, 0, state);
    }

    private void handlePositionAndRotation(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
        var pos = wrapper.getPosition();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        validateCoordinates(player, x, y, z);
        validateRotation(player, yaw, pitch);
        checkDuplicate(player, x, y, z, pitch, yaw, state);
    }

    private void handleRotation(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
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

    private void handleInteractEntity(WindfallPlayer player, PacketReceiveEvent event, long now, PlayerState state) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();

        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            state.attackCountThisTick++;
            state.lastAttackPacketTime = now;

            if (state.attackCountThisTick > MAX_ATTACKS_PER_TICK) {
                flagDetail(player, "auto-clicker detected: " + state.attackCountThisTick + " attacks/tick");
            }
        }
    }

    private void validateCoordinates(WindfallPlayer player, double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            flagDetail(player, "NaN/Infinite coordinates, kicking");
            player.getPlayer().kickPlayer("[Windfall] Invalid position data");
            return;
        }

        int protocol = player.getProtocolVersion();
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

    private void checkDuplicate(WindfallPlayer player, double x, double y, double z, float pitch, float yaw, PlayerState state) {
        double epsilon = 0.00001;
        if (Math.abs(x - state.lastPosX) < epsilon
                && Math.abs(y - state.lastPosY) < epsilon
                && Math.abs(z - state.lastPosZ) < epsilon
                && Math.abs(yaw - state.lastRotYaw) < epsilon
                && Math.abs(pitch - state.lastRotPitch) < epsilon) {
            state.duplicateCount++;
            if (state.duplicateCount > DUPLICATE_THRESHOLD) {
                increaseBuffer(player, 0.1);
            }
        } else {
            state.duplicateCount = Math.max(0, state.duplicateCount - 1);
        }

        state.lastPosX = x;
        state.lastPosY = y;
        state.lastPosZ = z;
        state.lastRotYaw = yaw;
        state.lastRotPitch = pitch;
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
}
