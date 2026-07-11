package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;

@CheckData(name = "Velocity A", stableKey = "windfall.movement.velocity", decay = 0.01, setbackVl = 30)
public class VelocityCheck extends Check implements PacketCheck {

    private static final double VELOCITY_DRAG_HORIZONTAL = 0.91;
    private static final double VELOCITY_DRAG_VERTICAL = 0.98;
    private static final double GRAVITY = 0.08;
    private static final double MIN_VELOCITY_THRESHOLD = 0.005;
    private static final int MAX_PENDING_VELOCITIES = 5;

    private static final double GROUND_FRICTION = 0.91;
    private static final double WEB_FRICTION = 0.25;
    private static final double WATER_FRICTION = 0.8;
    private static final double LAVA_FRICTION = 0.5;
    private static final double ICE_FRICTION = 0.98;
    private static final double SOUL_SAND_FRICTION = 0.6;
    private static final double HONEY_FRICTION = 0.4;
    private static final double LADDER_FRICTION = 0.2;

    private final ArrayDeque<PendingVelocity> pendingVelocities = new ArrayDeque<>();

    private boolean velocityActive;
    private double expectedDeltaX;
    private double expectedDeltaY;
    private double expectedDeltaZ;
    private double rawVelocityX;
    private double rawVelocityY;
    private double rawVelocityZ;
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
            rawVelocityX = pv.velX;
            rawVelocityY = pv.velY;
            rawVelocityZ = pv.velZ;
            pendingVelocities.removeFirst();

            double[] postDrag = applyPostVelocityPhysics(rawVelocityX, rawVelocityY, rawVelocityZ, player);
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

        if (combinedRatio < 0.5) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (combinedRatio < 0.8) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    private double[] applyPostVelocityPhysics(double velX, double velY, double velZ, WindfallPlayer player) {
        double horizontalFriction = getEffectiveHorizontalFriction(player);
        double verticalFriction = VELOCITY_DRAG_VERTICAL;

        double newDeltaX = velX * horizontalFriction;
        double newDeltaZ = velZ * horizontalFriction;

        double newDeltaY;
        if (player.isSwimming()) {
            newDeltaY = velY * WATER_FRICTION - 0.02;
        } else if (player.isClimbing()) {
            newDeltaY = Math.max(velY, -0.15);
            newDeltaX *= LADDER_FRICTION;
            newDeltaZ *= LADDER_FRICTION;
        } else {
            newDeltaY = (velY - GRAVITY) * verticalFriction;
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

    private double getEffectiveHorizontalFriction(WindfallPlayer player) {
        if (player.isOnGround()) {
            return GROUND_FRICTION;
        }
        return VELOCITY_DRAG_HORIZONTAL;
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
}
