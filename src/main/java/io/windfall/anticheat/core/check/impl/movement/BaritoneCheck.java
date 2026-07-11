package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Baritone A", stableKey = "windfall.movement.baritone", decay = 0.01, setbackVl = 20)
public class BaritoneCheck extends Check implements PacketCheck {

    private static final double STRAIGHT_LINE_TOLERANCE = 0.02;
    private static final int MIN_STRAIGHT_TICKS = 20;
    private static final double PERFECT_PATH_THRESHOLD = 0.95;
    private static final int PATH_SAMPLE_SIZE = 40;

    private int straightTicks;
    private double lastDeltaX;
    private double lastDeltaZ;
    private int perfectPathSegments;
    private int totalSegments;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed < 0.05) {
            straightTicks = 0;
            decreaseBuffer(player, 0.1);
            return;
        }

        double lastSpeed = Math.sqrt(lastDeltaX * lastDeltaX + lastDeltaZ * lastDeltaZ);

        // Detect perfectly straight movement (Baritone path following)
        if (lastSpeed > 0.05 && horizontalSpeed > 0.05) {
            double angle = Math.abs(Math.atan2(deltaZ, deltaX) - Math.atan2(lastDeltaZ, lastDeltaX));
            if (angle > Math.PI) angle = 2 * Math.PI - angle;

            if (angle < STRAIGHT_LINE_TOLERANCE) {
                straightTicks++;
            } else {
                straightTicks = 0;
            }

            totalSegments++;
            // Consistent speed between ticks = machine-generated path
            double speedDiff = Math.abs(horizontalSpeed - lastSpeed);
            if (speedDiff < 0.005) {
                perfectPathSegments++;
            }
        }

        if (straightTicks > MIN_STRAIGHT_TICKS) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
                straightTicks = 0;
            }
        }

        if (totalSegments >= PATH_SAMPLE_SIZE) {
            double perfectRatio = (double) perfectPathSegments / totalSegments;
            if (perfectRatio > PERFECT_PATH_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.2);
            }
            totalSegments = 0;
            perfectPathSegments = 0;
        }

        lastDeltaX = deltaX;
        lastDeltaZ = deltaZ;
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
