package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Reach A", stableKey = "windfall.combat.reach", decay = 0.05, setbackVl = 10, compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class ReachCheck extends Check implements PacketCheck {

    private static final double TOLERANCE = 0.1;

    private static final double PROTOCOL_MARGIN_LEGACY = 0.10;
    private static final double PROTOCOL_MARGIN_MODERN = 0.03;

    private static final double REACH_LEGACY = 4.0;
    private static final double REACH_1_9_BASE = 3.0;
    private static final double REACH_1_9_COOLDOWN_BONUS = 0.5;
    private static final double REACH_MODERN = 3.0;

    private static final double PLAYER_WIDTH = 0.6;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double PLAYER_SNEAK_HEIGHT_1_14 = 1.5;
    private static final double PLAYER_SNEAK_HEIGHT_LEGACY = 1.62;
    private static final double ENTITY_DEFAULT_SIZE = 0.25;

    private static final int ROLLING_WINDOW = 20;

    private static final ConcurrentHashMap<Integer, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();

    private static final class PlayerState {
        final ArrayDeque<Double> reachSamples = new ArrayDeque<>();
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    public static void trackSpawn(int entityId, EntityType type, double x, double y, double z) {
        trackedEntities.put(entityId, new TrackedEntity(type, x, y, z, System.currentTimeMillis()));
    }

    public static void trackMove(int entityId, double x, double y, double z) {
        trackedEntities.merge(entityId,
                new TrackedEntity(null, x, y, z, System.currentTimeMillis()),
                (old, fresh) -> new TrackedEntity(
                        old.type != null ? old.type : fresh.type,
                        fresh.x, fresh.y, fresh.z, fresh.timestamp));
    }

    public static void trackRemove(int entityId) {
        trackedEntities.remove(entityId);
    }

    public static void cleanup(long maxAgeMs) {
        long now = System.currentTimeMillis();
        trackedEntities.entrySet().removeIf(e -> now - e.getValue().timestamp > maxAgeMs);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PlayerState state = getState(player);
        int targetId = wrapper.getEntityId();

        double[] compensated = getCompensatedSamples(player, targetId);
        double[] targetBB;
        if (compensated != null) {
            TrackedEntity te = trackedEntities.get(targetId);
            double halfWidth = te != null && te.type == EntityTypes.PLAYER ? PLAYER_WIDTH / 2.0 : ENTITY_DEFAULT_SIZE / 2.0;
            double bbHeight = te != null && te.type == EntityTypes.PLAYER ? PLAYER_HEIGHT : ENTITY_DEFAULT_SIZE;
            targetBB = new double[]{
                    compensated[0] - halfWidth, compensated[1], compensated[2] - halfWidth,
                    compensated[0] + halfWidth, compensated[1] + bbHeight, compensated[2] + halfWidth
            };
        } else {
            targetBB = getEntityBoundingBox(targetId);
        }
        if (targetBB == null) return;

        double eyeX = player.getX();
        double eyeY = player.getY() + getEyeHeight(player);
        double eyeZ = player.getZ();

        double reach = calculateReachDistance(
                eyeX, eyeY, eyeZ,
                targetBB[0], targetBB[1], targetBB[2],
                targetBB[3], targetBB[4], targetBB[5]);

        state.reachSamples.addLast(reach);
        if (state.reachSamples.size() > ROLLING_WINDOW) {
            state.reachSamples.removeFirst();
        }

        double limit = getReachLimit(player);

        double pingTolerance = Math.min(player.getTransactionPing() * 0.001, 0.3);
        double protocolMargin = getProtocolMargin(player);
        double effectiveLimit = limit + TOLERANCE + pingTolerance + protocolMargin;

        if (reach > effectiveLimit) {
            flag(player);
            return;
        }

        double avgReach = state.reachSamples.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        if (avgReach > limit + 0.05 && state.reachSamples.size() >= ROLLING_WINDOW / 2) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double getProtocolMargin(WindfallPlayer player) {
        return player.getProtocolVersion() < 107 ? PROTOCOL_MARGIN_LEGACY : PROTOCOL_MARGIN_MODERN;
    }

    private double[] getCompensatedSamples(WindfallPlayer player, int entityId) {
        int rtt = player.getTransactionPing();
        int rewindTicks = Math.max(1, Math.min(rtt / 50, 3));
        return getEntityPositionAtTick(entityId, rewindTicks);
    }

    private double[] getEntityPositionAtTick(int entityId, int rewindTicks) {
        TrackedEntity te = trackedEntities.get(entityId);
        if (te == null) return null;
        long age = System.currentTimeMillis() - te.timestamp;
        long tickMs = 50L * rewindTicks;
        if (age > tickMs) {
            return new double[]{te.x, te.y, te.z};
        }
        return new double[]{te.x, te.y, te.z};
    }

    private double getReachLimit(WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        double baseReach = VersionPhysics.getMaxReach(protocol);

        if (VersionPhysics.hasAttackCooldown(protocol)) {
            int cooldown = player.getAttackCooldown();
            double cooldownModifier = Math.min(cooldown / 20.0, 1.0);
            return baseReach + (VersionPhysics.getCooldownReachBonus(protocol) * cooldownModifier);
        }

        return baseReach;
    }

    private double getEyeHeight(WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        return VersionPhysics.getPlayerEyeHeight(player.isSneaking(), protocol);
    }

    private double calculateReachDistance(double eyeX, double eyeY, double eyeZ,
                                          double bbMinX, double bbMinY, double bbMinZ,
                                          double bbMaxX, double bbMaxY, double bbMaxZ) {
        double closestX = clamp(eyeX, bbMinX, bbMaxX);
        double closestY = clamp(eyeY, bbMinY, bbMaxY);
        double closestZ = clamp(eyeZ, bbMinZ, bbMaxZ);

        double dx = eyeX - closestX;
        double dy = eyeY - closestY;
        double dz = eyeZ - closestZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double[] getEntityBoundingBox(int entityId) {
        TrackedEntity te = trackedEntities.get(entityId);
        if (te == null) return null;

        double halfWidth;
        double height;

        if (te.type == EntityTypes.PLAYER) {
            halfWidth = PLAYER_WIDTH / 2.0;
            height = PLAYER_HEIGHT;
        } else if (te.type != null) {
            halfWidth = ENTITY_DEFAULT_SIZE / 2.0;
            height = ENTITY_DEFAULT_SIZE;
        } else {
            halfWidth = PLAYER_WIDTH / 2.0;
            height = PLAYER_HEIGHT;
        }

        return new double[]{
                te.x - halfWidth, te.y, te.z - halfWidth,
                te.x + halfWidth, te.y + height, te.z + halfWidth
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class TrackedEntity {
        final EntityType type;
        final double x, y, z;
        final long timestamp;

        TrackedEntity(EntityType type, double x, double y, double z, long timestamp) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }
    }
}
