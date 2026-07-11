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

@CheckData(name = "Simulation A", stableKey = "windfall.movement.simulation", decay = 0.01, setbackVl = 20)
public class SimulationCheck extends Check implements PacketCheck {

    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;
    private static final double JUMP_MOMENTUM = 0.42;
    private static final double WALK_SPEED = 0.102;
    private static final double SPRINT_MULT = 1.3;
    private static final double GROUND_FRICTION = 0.91;
    private static final double PLAYER_WIDTH = 0.6;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double MAX_SIMULATION_DEVIATION = 0.15;
    private static final int MIN_SAMPLES = 10;
    private static final double TOLERANCE = 0.06;

    private double expectedDeltaY;
    private int samples;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        boolean onGround = player.isOnGround();
        double deltaY = player.getDeltaY();
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();

        if (onGround) {
            expectedDeltaY = 0;
            samples = 0;
            return;
        }

        // Simple vertical prediction: (prevDeltaY - gravity) * airDrag
        double predictedDeltaY = (expectedDeltaY - GRAVITY) * AIR_DRAG;

        double verticalDeviation = Math.abs(deltaY - predictedDeltaY);
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (verticalDeviation > MAX_SIMULATION_DEVIATION && horizontalSpeed > 0.1) {
            samples++;
            if (samples >= MIN_SAMPLES) {
                increaseBuffer(player, 0.5 * (verticalDeviation / MAX_SIMULATION_DEVIATION));
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    samples = 0;
                }
            }
        } else {
            samples = Math.max(0, samples - 1);
            decreaseBuffer(player, 0.1);
        }

        expectedDeltaY = deltaY;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
