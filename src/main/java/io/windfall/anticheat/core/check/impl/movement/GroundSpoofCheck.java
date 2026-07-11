package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Ground Spoof A", stableKey = "windfall.movement.groundspoof", decay = 0.01, setbackVl = 20)
public class GroundSpoofCheck extends Check implements PacketCheck {

    private static final double MIN_AIR_TIME_FOR_GROUND = 3.0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_FALSE_GROUND_FLAGS = 5;
    private static final double FALLING_VELOCITY_THRESHOLD = 0.3;
    private static final double MIN_FALL_DISTANCE = 2.0;

    private int falseGroundCount;
    private int airTicks;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        boolean claimsGround = player.isOnGround();
        double deltaY = player.getDeltaY();
        boolean isFalling = deltaY < -FALLING_VELOCITY_THRESHOLD;
        double fallDistance = player.getLastY() - player.getY();

        if (!claimsGround) {
            airTicks++;
            return;
        }

        // Player claims ground but is falling fast — spoofed
        if (isFalling && fallDistance > MIN_FALL_DISTANCE) {
            falseGroundCount++;
            if (falseGroundCount >= MIN_FALSE_GROUND_FLAGS) {
                flag(player);
                resetBuffer(player);
                falseGroundCount = 0;
            }
            return;
        }

        // Player was airborne for extended period then suddenly claims ground
        if (airTicks > MIN_AIR_TIME_FOR_GROUND * TICKS_PER_SECOND) {
            // Falling very fast then instant ground claim is suspicious
            if (Math.abs(deltaY) > 0.5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        airTicks = 0;
        falseGroundCount = Math.max(0, falseGroundCount - 1);
        decreaseBuffer(player, 0.1);
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
