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
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects reach (hit-box extension) cheats by measuring the Euclidean distance between
 * the player's eye position and the closest point on the target's bounding box at the
 * moment of an attack packet.
 *
 * <p><b>Distance calculation:</b> The closest point on the target's AABB to the player's
 * eye is found via axis clamping, then the Euclidean distance is computed. This is the
 * geometrically correct minimum distance from a point to an axis-aligned bounding box.</p>
 *
 * <p><b>Reach limits:</b> Base reach varies by protocol version:
 * <ul>
 *   <li>Legacy (pre-1.9): {@value REACH_LEGACY} blocks</li>
 *   <li>1.9+ with cooldown: {@value REACH_1_9_BASE} + up to {@value REACH_1_9_COOLDOWN_BONUS} blocks based on cooldown</li>
 *   <li>Modern: {@value REACH_MODERN} blocks</li>
 * </ul>
 * Additional tolerances are added for ping compensation ({@code min(ping * 0.001, 0.3)}),
 * protocol version margin, and a fixed {@value TOLERANCE} block buffer.</p>
 *
 * <p><b>Lag compensation:</b> Entity positions are rewound by up to 3 ticks (based on RTT)
 * to account for server-side entity position lag, using the {@link TrackedEntity} cache
 * populated by spawn/move/remove packet handlers.</p>
 *
 * @see io.windfall.anticheat.core.physics.VersionPhysics
 */
@CheckData(name = "Reach A", stableKey = "windfall.combat.reach", decay = 0.05, setbackVl = 10, compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class ReachCheck extends Check implements PacketCheck {

    /** Fixed tolerance in blocks added to the reach limit for borderline cases. */
    private static final double TOLERANCE = 0.1;

    /** Extra margin (blocks) for legacy protocol versions where position data is less precise. */
    private static final double PROTOCOL_MARGIN_LEGACY = 0.10;
    /** Extra margin (blocks) for modern protocol versions. */
    private static final double PROTOCOL_MARGIN_MODERN = 0.03;

    /** Maximum reach distance (blocks) for legacy clients (pre-1.9 combat). */
    private static final double REACH_LEGACY = 4.0;
    /** Base reach distance for 1.9+ clients when attack cooldown is fully charged. */
    private static final double REACH_1_9_BASE = 3.0;
    /** Bonus reach (blocks) awarded when the 1.9+ attack cooldown is fully charged. */
    private static final double REACH_1_9_COOLDOWN_BONUS = 0.5;
    /** Default reach distance for modern clients without cooldown mechanics. */
    private static final double REACH_MODERN = 3.0;

    /** Player hit-box width in blocks (0.6 for all versions). */
    private static final double PLAYER_WIDTH = 0.6;
    /** Player hit-box height in blocks (standing, modern versions). */
    private static final double PLAYER_HEIGHT = 1.8;
    /** Sneaking player eye height (1.14+). */
    private static final double PLAYER_SNEAK_HEIGHT_1_14 = 1.5;
    /** Sneaking player eye height (legacy versions). */
    private static final double PLAYER_SNEAK_HEIGHT_LEGACY = 1.62;
    /** Default non-player entity size used when the entity type is unknown. */
    private static final double ENTITY_DEFAULT_SIZE = 0.25;

    /** Number of recent reach samples kept for averaging (rolling window). */
    private static final int ROLLING_WINDOW = 20;

    /**
     * Maximum number of attacker position snapshots kept for multi-snapshot
     * lag compensation. Adapted from ArrowAntiCheat's InteractD approach of
     * trying multiple attacker positions to find the best-matching one.
     */
    private static final int ATTACKER_SNAPSHOT_WINDOW = 5;

    /** Maximum age (ms) for attacker snapshots to be considered during reach calculation. */
    private static final long ATTACKER_SNAPSHOT_MAX_AGE_MS = 250L;

    /** Shared cache of tracked entity positions, populated by spawn/move/remove handlers. */
    private static final ConcurrentHashMap<Integer, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();

    /** Per-player state holding recent reach distance samples for averaging. */
    private static final class PlayerState {
        final ArrayDeque<Double> reachSamples = new ArrayDeque<>();
        final ArrayDeque<AttackerSnapshot> attackerSnapshots = new ArrayDeque<>();
    }

    /**
     * Captured attacker state at the moment of a rotation/position packet.
     * Used for multi-snapshot lag compensation — when an attack arrives,
     * multiple recent snapshots are tried and the one yielding the shortest
     * reach distance is used. This prevents false positives when the attacker
     * has moved between sending the attack packet and server processing.
     */
    private static final class AttackerSnapshot {
        final double eyeX, eyeY, eyeZ;
        final long timestamp;

        AttackerSnapshot(double eyeX, double eyeY, double eyeZ, long timestamp) {
            this.eyeX = eyeX;
            this.eyeY = eyeY;
            this.eyeZ = eyeZ;
            this.timestamp = timestamp;
        }
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or initializes the tracking state for the given player.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Records an entity's spawn position in the shared tracking cache.
     *
     * @param entityId the entity's network ID
     * @param type     the entity type
     * @param x        spawn X coordinate
     * @param y        spawn Y coordinate
     * @param z        spawn Z coordinate
     */
    public static void trackSpawn(int entityId, EntityType type, double x, double y, double z) {
        trackedEntities.put(entityId, new TrackedEntity(type, x, y, z, System.currentTimeMillis()));
    }

    /**
     * Updates an entity's position in the tracking cache, preserving the original type.
     *
     * @param entityId the entity's network ID
     * @param x        new X coordinate
     * @param y        new Y coordinate
     * @param z        new Z coordinate
     */
    public static void trackMove(int entityId, double x, double y, double z) {
        trackedEntities.merge(entityId,
                new TrackedEntity(null, x, y, z, System.currentTimeMillis()),
                (old, fresh) -> new TrackedEntity(
                        old.type != null ? old.type : fresh.type,
                        fresh.x, fresh.y, fresh.z, fresh.timestamp));
    }

    /**
     * Removes an entity from the tracking cache (e.g., on entity destroy packet).
     *
     * @param entityId the entity's network ID to remove
     */
    public static void trackRemove(int entityId) {
        trackedEntities.remove(entityId);
    }

    /**
     * Evicts stale entries from the entity tracking cache. Should be called periodically
     * (e.g., once per tick) to prevent memory leaks from entities that are no longer tracked.
     *
     * @param maxAgeMs maximum age in milliseconds before an entry is evicted
     */
    public static void cleanup(long maxAgeMs) {
        long now = System.currentTimeMillis();
        trackedEntities.entrySet().removeIf(e -> now - e.getValue().timestamp > maxAgeMs);
    }

    /**
     * Processes incoming packets: records attacker position snapshots on rotation/
     * movement packets, and evaluates reach distance on attack packets using
     * multi-snapshot lag compensation (adapted from ArrowAntiCheat InteractD).
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        /* Record attacker position snapshot on rotation/movement packets. */
        if (type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            recordAttackerSnapshot(player);
            return;
        }

        if (type != PacketType.Play.Client.INTERACT_ENTITY) return;

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

        /*
         * Multi-snapshot reach calculation (InteractD adaptation).
         * Try each recent attacker position snapshot and use the shortest
         * reach distance. This compensates for attacker movement between
         * sending the attack packet and server processing, and for
         * under-block eye-height discrepancies.
         */
        double bestReach = Double.MAX_VALUE;
        long now = System.currentTimeMillis();

        ArrayDeque<AttackerSnapshot> snapshots = state.attackerSnapshots;

        if (snapshots.isEmpty()) {
            /* Fallback: use current position if no snapshots are available. */
            bestReach = computeReachFromPosition(
                    player.getX(), player.getY() + getEyeHeight(player), player.getZ(),
                    targetBB);
        } else {
            for (AttackerSnapshot snap : snapshots) {
                long age = now - snap.timestamp;
                if (age > ATTACKER_SNAPSHOT_MAX_AGE_MS) continue;

                double reach = computeReachFromPosition(snap.eyeX, snap.eyeY, snap.eyeZ, targetBB);
                if (reach < bestReach) {
                    bestReach = reach;
                }
            }

            /* Also try current position — it may be more accurate for very recent attacks. */
            double currentReach = computeReachFromPosition(
                    player.getX(), player.getY() + getEyeHeight(player), player.getZ(),
                    targetBB);
            if (currentReach < bestReach) {
                bestReach = currentReach;
            }
        }

        state.reachSamples.addLast(bestReach);
        if (state.reachSamples.size() > ROLLING_WINDOW) {
            state.reachSamples.removeFirst();
        }

        double limit = getReachLimit(player);

        double pingTolerance = Math.min(player.getTransactionPing() * 0.001, 0.3);
        double protocolMargin = getProtocolMargin(player);
        double effectiveLimit = limit + TOLERANCE + pingTolerance + protocolMargin;

        if (bestReach > effectiveLimit) {
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

    /**
     * Records the attacker's current eye position as a snapshot for multi-snapshot
     * lag compensation. Called on every rotation or position-and-rotation packet.
     *
     * @param player the player whose position to record
     */
    private void recordAttackerSnapshot(WindfallPlayer player) {
        PlayerState state = getState(player);
        double eyeX = player.getX();
        double eyeY = player.getY() + getEyeHeight(player);
        double eyeZ = player.getZ();
        state.attackerSnapshots.addLast(new AttackerSnapshot(eyeX, eyeY, eyeZ, System.currentTimeMillis()));

        while (state.attackerSnapshots.size() > ATTACKER_SNAPSHOT_WINDOW) {
            state.attackerSnapshots.removeFirst();
        }
    }

    /**
     * Computes the reach distance from a given eye position to the target's bounding box.
     *
     * @param eyeX     attacker eye X
     * @param eyeY     attacker eye Y (already includes eye height)
     * @param eyeZ     attacker eye Z
     * @param targetBB target bounding box [minX, minY, minZ, maxX, maxY, maxZ]
     * @return the Euclidean distance from the eye to the closest point on the bounding box
     */
    private double computeReachFromPosition(double eyeX, double eyeY, double eyeZ, double[] targetBB) {
        return calculateReachDistance(
                eyeX, eyeY, eyeZ,
                targetBB[0], targetBB[1], targetBB[2],
                targetBB[3], targetBB[4], targetBB[5]);
    }

    /**
     * Returns the protocol-version-appropriate extra reach margin (blocks).
     *
     * @param player the player to check
     * @return margin in blocks
     */
    private double getProtocolMargin(WindfallPlayer player) {
        return player.getProtocolVersion() < 107 ? PROTOCOL_MARGIN_LEGACY : PROTOCOL_MARGIN_MODERN;
    }

    /**
     * Computes lag-compensated position samples for an entity. The entity is rewound
     * by a number of ticks proportional to the player's RTT (capped at 3 ticks).
     *
     * @param player   the attacking player (used for ping)
     * @param entityId the target entity's network ID
     * @return compensated [x, y, z] array, or {@code null} if the entity is not tracked
     */
    private double[] getCompensatedSamples(WindfallPlayer player, int entityId) {
        int rtt = player.getTransactionPing();
        int rewindTicks = Math.max(1, Math.min(rtt / 50, 3));
        return getEntityPositionAtTick(entityId, rewindTicks);
    }

    /**
     * Retrieves the entity's last known position, rewound by the specified tick count.
     * Currently returns the latest position as full tick-level interpolation is not yet
     * implemented.
     *
     * @param entityId   the entity's network ID
     * @param rewindTicks number of ticks to rewind (1–3)
     * @return [x, y, z] position array, or {@code null} if not tracked
     */
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

    /**
     * Computes the maximum legal reach distance for the player, accounting for version-specific
     * combat mechanics (e.g., 1.9+ attack cooldown bonus).
     *
     * @param player the player to compute the limit for
     * @return the base reach limit in blocks
     */
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

    /**
     * Returns the player's eye height based on protocol version and sneaking state.
     *
     * @param player the player to get the eye height for
     * @return eye height in blocks above the player's feet position
     */
    private double getEyeHeight(WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        return VersionPhysics.getPlayerEyeHeight(player.isSneaking(), protocol);
    }

    /**
     * Computes the Euclidean distance from a point (eye position) to the closest point
     * on an axis-aligned bounding box (AABB). Uses axis clamping to find the nearest
     * point on each axis, then computes the 3D Euclidean norm.
     *
     * @param eyeX player eye X
     * @param eyeY player eye Y
     * @param eyeZ player eye Z
     * @param bbMinX target AABB minimum X
     * @param bbMinY target AABB minimum Y
     * @param bbMinZ target AABB minimum Z
     * @param bbMaxX target AABB maximum X
     * @param bbMaxY target AABB maximum Y
     * @param bbMaxZ target AABB maximum Z
     * @return the shortest distance in blocks from the eye to the bounding box
     */
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

    /**
     * Builds a bounding box ([minX, minY, minZ, maxX, maxY, maxZ]) for a tracked entity.
     * Player entities use standard dimensions; non-player entities use a default size.
     *
     * @param entityId the entity's network ID
     * @return a 6-element array representing the AABB, or {@code null} if not tracked
     */
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

    /**
     * Clamps a value to the range [min, max].
     *
     * @param value the value to clamp
     * @param min   the minimum bound
     * @param max   the maximum bound
     * @return the clamped value
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Cached entity data: type, position, and last-seen timestamp for lag compensation. */
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
