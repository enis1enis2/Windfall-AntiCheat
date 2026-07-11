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

@CheckData(name = "Step A", stableKey = "windfall.movement.step", decay = 0.01, setbackVl = 15)
public class StepCheck extends Check implements PacketCheck {

    private static final double MAX_STEP_HEIGHT_SNEAK = 0.6;
    private static final double MAX_STEP_HEIGHT_LADDER = 2.0;
    private static final double STEP_TOLERANCE = 0.05;
    private static final int STEP_BUFFER_FLAG_THRESHOLD = 3;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        // Both ticks must be on ground — otherwise it's a jump, not a step
        if (player.isOnGround() && player.isLastOnGround()) {
            double deltaY = player.getY() - player.getLastY();

            if (deltaY <= 0 || deltaY < STEP_TOLERANCE) return;

            double maxHeight = getMaxStepHeight(player);

            if (deltaY > maxHeight + STEP_TOLERANCE) {
                double overshoot = deltaY - maxHeight;

                // Large overshoots = blatant hacks, immediate flag
                if (overshoot > 0.3) {
                    flag(player);
                    return;
                }

                increaseBuffer(player, overshoot * 5.0);
                if (getBuffer(player) > STEP_BUFFER_FLAG_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double getMaxStepHeight(WindfallPlayer player) {
        if (player.isClimbing()) return MAX_STEP_HEIGHT_LADDER;
        if (player.isSneaking()) return MAX_STEP_HEIGHT_SNEAK;
        int protocol = player.getProtocolVersion();
        return VersionPhysics.getStepHeight(protocol);
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
