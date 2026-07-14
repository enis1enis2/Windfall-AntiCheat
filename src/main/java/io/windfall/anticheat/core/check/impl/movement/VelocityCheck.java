package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.platform.FoliaCompat;
import io.windfall.anticheat.core.platform.PurpurCompat;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Detects knockback (velocity) rejection — clients that receive an entity velocity packet but
 * fail to reflect the expected movement in their subsequent position updates.
 *
 * <p>Algorithm:
 * <ol>
 *   <li><b>Capture</b>: When the server sends an {@code ENTITY_VELOCITY} packet to the player,
 *       the raw velocity vector is decoded (divided by 8000.0 as per the MC protocol) and queued.</li>
 *   <li><b>Apply physics</b>: On the next movement packet, the pending velocity is consumed and
 *       run through {@link #applyVersionAwarePostVelocityPhysics} which applies ground friction,
 *       air drag, water drag, climbing slowdown, and Purpur custom knockback adjustments.</li>
 *   <li><b>Compare</b>: The expected post-physics horizontal and vertical distances are compared
 *       to the player's actual deltas. The combined ratio (average of horizontal and vertical)
 *       indicates how much of the knockback was honored.</li>
 * </ol>
 *
 * <p>Detection thresholds (adjusted for bedrock, legacy versions, and nearby walls):
 * <ul>
 *   <li>{@code combinedRatio &lt; 0.5 / tolerance} → blatant rejection, high buffer increase</li>
 *   <li>{@code combinedRatio &lt; 0.8 / tolerance} → partial rejection, gradual buffer increase</li>
 *   <li>{@code combinedRatio ≥ 0.8 / tolerance} → legitimate, buffer decreases</li>
 * </ul>
 *
 * <p>Tolerance is widened for bedrock clients (+10%), legacy protocol versions (+20%), and when
 * the player is near a solid wall (+30%) to avoid false positives from legitimate block-collision
 * knockback absorption.
 *
 * @see PhysicsConstants for vanilla friction/drag values
 * @see PurpurCompat for custom knockback support
 */
@CheckData(name = "Velocity A", stableKey = "windfall.movement.velocity", decay = 0.01, setbackVl = 30,
    compat = {CompatFlag.PURPUR_KB_DEPENDENT,CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.3)
public class VelocityCheck extends Check implements PacketCheck {

    /** Minimum velocity magnitude to consider — filters out negligible knockback (noise threshold) */
    private static final double MIN_VELOCITY_THRESHOLD = 0.005;
    /** Maximum queued velocity packets per player — prevents memory abuse from packet spam */
    private static final int MAX_PENDING_VELOCITIES = 5;

    /** Horizontal knockback strength on pre-1.9 clients */
    private static final double KB_HORIZONTAL_PRE_1_9 = 0.4;
    /** Horizontal knockback strength on 1.9+ when airborne */
    private static final double KB_HORIZONTAL_1_9_AIRBORNE = 0.28;
    /** Knockback multiplier when the attacker is sprinting */
    private static final double KB_SPRINT_MULTIPLIER = 1.5;
    /** Vertical knockback component */
    private static final double KB_VERTICAL = 0.4;

    private static final class PlayerState {
        final ConcurrentLinkedDeque<PendingVelocity> pendingVelocities = new ConcurrentLinkedDeque<>();
        boolean velocityActive;
        double expectedDeltaX;
        double expectedDeltaY;
        double expectedDeltaZ;
        int velocityAge;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Returns the per-player velocity check state.
     *
     * @param player the player to retrieve state for
     * @return the player's {@link PlayerState}, creating one if absent
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** Clears per-player state on disconnect to prevent memory leaks. */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Captures outgoing entity velocity packets sent to the player.
     *
     * <p>The raw velocity vector is decoded from the MC protocol format (divided by 8000.0)
     * and queued for later comparison when the player's next movement packet arrives.
     * Negligible velocities (below {@value MIN_VELOCITY_THRESHOLD}) are ignored.
     *
     * @param player the player receiving the velocity packet
     * @param event  the outgoing packet event
     */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity wrapper = new WrapperPlayServerEntityVelocity(event);
        int targetEntityId = wrapper.getEntityId();

        int selfEntityId = player.getPlayer().getEntityId();
        if (targetEntityId != selfEntityId) return;

        /**
         * MC protocol encodes velocity as fixed-point integers (× 8000).
         * Divide back to get blocks/tick.
         */
        com.github.retrooper.packetevents.util.Vector3d vel = wrapper.getVelocity();
        double velX = vel.x / 8000.0;
        double velY = vel.y / 8000.0;
        double velZ = vel.z / 8000.0;

        if (Math.abs(velX) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velY) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velZ) < MIN_VELOCITY_THRESHOLD) {
            return;
        }

        PlayerState state = getState(player);
        /** Enforce queue size limit to prevent memory abuse from packet spam */
        while (state.pendingVelocities.size() >= MAX_PENDING_VELOCITIES) {
            state.pendingVelocities.removeFirst();
        }
        state.pendingVelocities.addLast(new PendingVelocity(velX, velY, velZ, System.currentTimeMillis()));
    }

    /**
     * Processes incoming movement packets to compare actual movement against expected knockback.
     *
     * <p>On the first movement packet after a velocity is queued, the velocity is consumed and
     * run through version-aware post-velocity physics. Subsequent packets compare the player's
     * actual deltas against the expected post-physics deltas. The combined ratio determines
     * whether the knockback was legitimately honored.
     *
     * @param player the player who sent the movement packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);

        /**
         * If velocity is not yet active and there are pending velocities, activate the first one.
         * The velocity times out if not consumed within (500ms + ping) to handle missed packets.
         * Uses pollFirst() for atomic poll to avoid TOCTOU race on the deque.
         */
        if (!state.velocityActive) {
            PendingVelocity pv;
            while ((pv = state.pendingVelocities.peekFirst()) != null) {
                long age = System.currentTimeMillis() - pv.receivedAt;
                long timeout = 500L + player.getTransactionPing();
                if (age > timeout) {
                    state.pendingVelocities.pollFirst();
                    continue;
                }
                state.pendingVelocities.pollFirst();
                state.velocityActive = true;

                /** Apply one tick of post-velocity physics to get expected deltas */
                double[] postDrag = applyVersionAwarePostVelocityPhysics(pv.velX, pv.velY, pv.velZ, player);
                state.expectedDeltaX = postDrag[0];
                state.expectedDeltaY = postDrag[1];
                state.expectedDeltaZ = postDrag[2];
                state.velocityAge = 0;
                return;
            }
            return;
        }

        if (!state.velocityActive) return;

        state.velocityAge++;

        double actualDeltaX = player.getDeltaX();
        double actualDeltaZ = player.getDeltaZ();
        double actualDeltaY = player.getDeltaY();

        /** Pythagorean horizontal distance for expected and actual movement */
        double expectedHorizontalDist = Math.sqrt(state.expectedDeltaX * state.expectedDeltaX + state.expectedDeltaZ * state.expectedDeltaZ);
        double actualHorizontalDist = Math.sqrt(actualDeltaX * actualDeltaX + actualDeltaZ * actualDeltaZ);

        /** If expected velocity is negligible, nothing to compare — reset */
        if (expectedHorizontalDist < MIN_VELOCITY_THRESHOLD && Math.abs(state.expectedDeltaY) < MIN_VELOCITY_THRESHOLD) {
            state.velocityActive = false;
            resetBuffer(player);
            return;
        }

        /**
         * horizontalRatio = actual horizontal distance / expected horizontal distance.
         * A ratio of 1.0 means the knockback was fully honored; 0.0 means fully rejected.
         */
        double horizontalRatio;
        if (expectedHorizontalDist > MIN_VELOCITY_THRESHOLD) {
            horizontalRatio = actualHorizontalDist / expectedHorizontalDist;
        } else {
            horizontalRatio = actualHorizontalDist < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        /**
         * verticalRatio = |actual deltaY| / |expected deltaY|.
         * Same logic as horizontal but for the Y axis.
         */
        double verticalRatio;
        if (Math.abs(state.expectedDeltaY) > MIN_VELOCITY_THRESHOLD) {
            verticalRatio = Math.abs(actualDeltaY) / Math.abs(state.expectedDeltaY);
        } else {
            verticalRatio = Math.abs(actualDeltaY) < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        /** Combined ratio: average of horizontal and vertical knockback honor */
        double combinedRatio = (horizontalRatio + verticalRatio) / 2.0;

        state.velocityActive = false;

        /** Stale velocity (received more than 5 ticks ago) — unreliable, skip */
        if (state.velocityAge > 5) {
            resetBuffer(player);
            return;
        }

        /**
         * Tolerance adjustments for different client/platform conditions:
         * - Bedrock: +10% (different movement precision)
         * - Legacy protocol versions: +20% (older rounding behavior)
         * - Near wall: +30% (block collision absorbs knockback legitimately)
         */
        double tolerance = 1.0;
        if (player.isBedrock()) {
            tolerance = 1.10;
        }
        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);
        if (bracket == VersionBracket.LEGACY) {
            tolerance *= 1.2;
        }

        if (isNearWall(player)) {
            tolerance *= 1.3;
        }

        /** Adjusted thresholds — divide by tolerance to widen the acceptable range */
        double adjustedThreshold05 = 0.5 / tolerance;
        double adjustedThreshold08 = 0.8 / tolerance;

        if (combinedRatio < adjustedThreshold05) {
            /** Blatant rejection: less than 50% of knockback honored */
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (combinedRatio < adjustedThreshold08) {
            /** Partial rejection: 50-80% of knockback honored */
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    /**
     * Applies one tick of post-velocity physics to determine the expected movement after
     * the knockback is processed by the server.
     *
     * <p>Accounts for:
     * <ul>
     *   <li>Purpur custom knockback adjustments</li>
     *   <li>Ground friction (0.91)</li>
     *   <li>Air drag (0.98) + gravity (0.08) for airborne players</li>
     *   <li>Water drag for swimming players</li>
     *   <li>Climbing slowdown (0.2× horizontal, capped vertical)</li>
     *   <li>Air acceleration contribution for airborne players</li>
     * </ul>
     *
     * @param velX    raw knockback X velocity (blocks/tick)
     * @param velY    raw knockback Y velocity (blocks/tick)
     * @param velZ    raw knockback Z velocity (blocks/tick)
     * @param player  the player receiving the knockback
     * @return double array of [expectedDeltaX, expectedDeltaY, expectedDeltaZ] after physics
     */
    private double[] applyVersionAwarePostVelocityPhysics(double velX, double velY, double velZ, WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        double gravity = PhysicsConstants.GRAVITY;
        double airDrag = PhysicsConstants.AIR_DRAG;
        double groundFriction = PhysicsConstants.GROUND_FRICTION;

        PurpurCompat purpur = WindfallPlugin.getInstance().getPurpurCompat();
        if (purpur.isCustomKnockbackEnabled()) {
            velX = purpur.adjustHorizontalKB(velX);
            velZ = purpur.adjustVerticalKB(velZ);
        }

        double newDeltaX = velX * groundFriction;
        double newDeltaZ = velZ * groundFriction;

        double newDeltaY;
        if (player.isSwimming()) {
            double waterDrag = protocol >= 393 ? PhysicsConstants.WATER_DRAG : 0.8;
            newDeltaY = velY * waterDrag - 0.02;
        } else if (player.isClimbing()) {
            newDeltaY = Math.max(velY, -0.15);
            newDeltaX *= 0.2;
            newDeltaZ *= 0.2;
        } else {
            newDeltaY = (velY - gravity) * airDrag;
        }

        double airAcceleration = player.getHorizontalSpeed() * 0.026;
        if (!player.isOnGround()) {
            double maxPostVelHorizontal = Math.sqrt(newDeltaX * newDeltaX + newDeltaZ * newDeltaZ);
            if (maxPostVelHorizontal > MIN_VELOCITY_THRESHOLD) {
                double accelContribution = Math.min(airAcceleration, maxPostVelHorizontal * 0.1);
                newDeltaX += (player.getDeltaX() > 0 ? 1 : -1) * accelContribution;
                newDeltaZ += (player.getDeltaZ() > 0 ? 1 : -1) * accelContribution;
            }
        }

        return new double[]{newDeltaX, newDeltaY, newDeltaZ};
    }

    /**
     * Checks if the incoming packet is a player movement update.
     *
     * @param event the incoming packet event
     * @return true if the packet is a flying, position, or position-and-rotation packet
     */
    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    /** Holds a pending knockback velocity to be consumed on the next movement packet. */
    private static final class PendingVelocity {
        final double velX, velY, velZ;
        final long receivedAt;

        PendingVelocity(double velX, double velY, double velZ, long receivedAt) {
            this.velX = velX;
            this.velY = velY;
            this.velZ = velZ;
            this.receivedAt = receivedAt;
        }
    }

    /**
     * Checks if the player is adjacent to solid blocks that could absorb knockback.
     *
     * <p>Scans a 3×3 horizontal ring around the player at three height levels (y-1, y, y+1).
     * If any adjacent block is solid, the knockback tolerance is widened to avoid false positives
     * from legitimate wall-collision physics.
     *
     * <p>Thread-safe on Folia: only accesses world data if the current thread owns the player's
     * region chunk, otherwise returns false to avoid concurrency issues.
     *
     * @param player the player to check
     * @return true if a solid block is found in the adjacent ring
     */
    private boolean isNearWall(WindfallPlayer player) {
        org.bukkit.World world = player.getPlayer().getWorld();
        FoliaCompat folia = WindfallPlugin.getInstance().getFoliaCompat();
        if (folia.isFolia() && !folia.isOwnedByCurrentRegion(player.getPlayer())) {
            return false;
        }
        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (world.getBlockAt(px + dx, py - 1, pz + dz).getType().isSolid()) return true;
                if (world.getBlockAt(px + dx, py, pz + dz).getType().isSolid()) return true;
                if (world.getBlockAt(px + dx, py + 1, pz + dz).getType().isSolid()) return true;
            }
        }
        return false;
    }
}
