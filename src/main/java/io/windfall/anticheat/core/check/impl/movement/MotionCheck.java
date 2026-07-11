package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Motion A", stableKey = "windfall.movement.motion", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class MotionCheck extends Check implements PacketCheck {

    private static final double MAX_PLAYER_SPEED = 1.0;
    private static final double MAX_VERTICAL_SPEED = 1.5;
    private static final double GROUND_MAX_SPEED = 0.28;
    private static final double SPRINT_GROUND_MAX = 0.36;
    private static final int MIN_HIGH_SPEED_TICKS = 3;

    private int highSpeedTicks;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        double deltaX = player.getDeltaX();
        double deltaY = player.getDeltaY();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double verticalSpeed = Math.abs(deltaY);

        boolean onGround = player.isOnGround();

        double maxHorizontal = onGround
                ? (player.isSprinting() ? SPRINT_GROUND_MAX : GROUND_MAX_SPEED)
                : MAX_PLAYER_SPEED;

        if (horizontalSpeed > maxHorizontal * 1.2) {
            highSpeedTicks++;
            if (highSpeedTicks >= MIN_HIGH_SPEED_TICKS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    highSpeedTicks = 0;
                }
            }
        } else {
            highSpeedTicks = Math.max(0, highSpeedTicks - 1);
            decreaseBuffer(player, 0.1);
        }

        if (verticalSpeed > MAX_VERTICAL_SPEED && !onGround) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }
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
