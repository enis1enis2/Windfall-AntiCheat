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
import io.windfall.anticheat.core.platform.PurpurCompat;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.ArrayDeque;

@CheckData(name = "Velocity A", stableKey = "windfall.movement.velocity", decay = 0.01, setbackVl = 30,
    compat = {CompatFlag.PURPUR_KB_DEPENDENT,CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.3)
public class VelocityCheck extends Check implements PacketCheck {

    private static final double MIN_VELOCITY_THRESHOLD = 0.005;
    private static final int MAX_PENDING_VELOCITIES = 5;

    // Pre-1.9 knockback: full KB applies to airborne entities
    private static final double KB_HORIZONTAL_PRE_1_9 = 0.4;
    // 1.9+: airborne horizontal KB reduced
    private static final double KB_HORIZONTAL_1_9_AIRBORNE = 0.28;
    // Sprint multiplier
    private static final double KB_SPRINT_MULTIPLIER = 1.5;
    // Vertical knockback
    private static final double KB_VERTICAL = 0.4;

    private final ArrayDeque<PendingVelocity> pendingVelocities = new ArrayDeque<>();

    private boolean velocityActive;
    private double expectedDeltaX;
    private double expectedDeltaY;
    private double expectedDeltaZ;
    private int velocityAge;

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity wrapper = new WrapperPlayServerEntityVelocity(event);
        int targetEntityId = wrapper.getEntityId();

        int selfEntityId = player.getPlayer().getEntityId();
        if (targetEntityId != selfEntityId) return;

        com.github.retrooper.packetevents.util.Vector3d vel = wrapper.getVelocity();
        double velX = vel.x / 8000.0;
        double velY = vel.y / 8000.0;
        double velZ = vel.z / 8000.0;

        if (Math.abs(velX) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velY) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velZ) < MIN_VELOCITY_THRESHOLD) {
            return;
        }

        while (pendingVelocities.size() >= MAX_PENDING_VELOCITIES) {
            pendingVelocities.removeFirst();
        }
        pendingVelocities.addLast(new PendingVelocity(velX, velY, velZ, System.currentTimeMillis()));
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        if (!velocityActive && !pendingVelocities.isEmpty()) {
            PendingVelocity pv = pendingVelocities.peekFirst();
            if (pv == null) return;

            long age = System.currentTimeMillis() - pv.receivedAt;
            long timeout = 500L + player.getTransactionPing();
            if (age > timeout) {
                pendingVelocities.removeFirst();
                return;
            }

            velocityActive = true;
            pendingVelocities.removeFirst();

            double[] postDrag = applyVersionAwarePostVelocityPhysics(pv.velX, pv.velY, pv.velZ, player);
            expectedDeltaX = postDrag[0];
            expectedDeltaY = postDrag[1];
            expectedDeltaZ = postDrag[2];
            velocityAge = 0;
            return;
        }

        if (!velocityActive) return;

        velocityAge++;

        double actualDeltaX = player.getDeltaX();
        double actualDeltaZ = player.getDeltaZ();
        double actualDeltaY = player.getDeltaY();

        double expectedHorizontalDist = Math.sqrt(expectedDeltaX * expectedDeltaX + expectedDeltaZ * expectedDeltaZ);
        double actualHorizontalDist = Math.sqrt(actualDeltaX * actualDeltaX + actualDeltaZ * actualDeltaZ);

        if (expectedHorizontalDist < MIN_VELOCITY_THRESHOLD && Math.abs(expectedDeltaY) < MIN_VELOCITY_THRESHOLD) {
            velocityActive = false;
            resetBuffer(player);
            return;
        }

        double horizontalRatio;
        if (expectedHorizontalDist > MIN_VELOCITY_THRESHOLD) {
            horizontalRatio = actualHorizontalDist / expectedHorizontalDist;
        } else {
            horizontalRatio = actualHorizontalDist < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        double verticalRatio;
        if (Math.abs(expectedDeltaY) > MIN_VELOCITY_THRESHOLD) {
            verticalRatio = Math.abs(actualDeltaY) / Math.abs(expectedDeltaY);
        } else {
            verticalRatio = Math.abs(actualDeltaY) < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        double combinedRatio = (horizontalRatio + verticalRatio) / 2.0;

        velocityActive = false;

        if (velocityAge > 5) {
            resetBuffer(player);
            return;
        }

        // Version-aware tolerance: Bedrock gets wider tolerance, version gap gets wider tolerance
        double tolerance = 1.0;
        if (player.isBedrock()) {
            tolerance = 1.10;
        }
        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);
        if (bracket == VersionBracket.LEGACY) {
            tolerance *= 1.2; // 1.8 KB formula differs significantly
        }

        // Wall collision tolerance: when knockback pushes player into a wall,
        // the client zeroes the perpendicular velocity component (Grim d2727a4 fix)
        if (isNearWall(player)) {
            tolerance *= 1.3;
        }

        double adjustedThreshold05 = 0.5 / tolerance;
        double adjustedThreshold08 = 0.8 / tolerance;

        if (combinedRatio < adjustedThreshold05) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (combinedRatio < adjustedThreshold08) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    private double[] applyVersionAwarePostVelocityPhysics(double velX, double velY, double velZ, WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        double gravity = PhysicsConstants.GRAVITY;
        double airDrag = PhysicsConstants.AIR_DRAG;
        double groundFriction = PhysicsConstants.GROUND_FRICTION;

        // Purpur custom knockback adjustment
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

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

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

    // Checks if the player is adjacent to a solid block — when knockback pushes
    // into a wall, the client zeroes the perpendicular velocity component
    private boolean isNearWall(WindfallPlayer player) {
        org.bukkit.World world = player.getPlayer().getWorld();
        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (world.getBlockAt(px + dx, py, pz + dz).getType().isSolid()) return true;
                if (world.getBlockAt(px + dx, py + 1, pz + dz).getType().isSolid()) return true;
            }
        }
        return false;
    }
}
