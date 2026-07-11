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

@CheckData(name = "NoFall A", stableKey = "windfall.movement.nofall", decay = 0.01, setbackVl = 15)
public class NoFallCheck extends Check implements PacketCheck {

    private static final double MIN_FALL_VELOCITY = 0.3;
    private static final double MIN_FALL_DISTANCE = 2.0;
    private static final int MAX_CONSECUTIVE = 5;

    private int consecutiveNoFall;
    private double maxFallDistance;
    private double maxFallVelocity;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        boolean onGround = player.isOnGround();
        double deltaY = player.getDeltaY();
        double fallDistance = player.getLastY() - player.getY();

        // All three conditions must be true: fast fall + distance + claiming ground
        if (deltaY < -MIN_FALL_VELOCITY && fallDistance > MIN_FALL_DISTANCE && onGround) {
            consecutiveNoFall++;

            if (fallDistance > maxFallDistance) maxFallDistance = fallDistance;
            if (Math.abs(deltaY) > maxFallVelocity) maxFallVelocity = Math.abs(deltaY);

            // Require 5 consecutive violations to avoid lag-spike false positives
        if (consecutiveNoFall >= MAX_CONSECUTIVE) {
                flag(player);
                consecutiveNoFall = 0;
                maxFallDistance = 0;
                maxFallVelocity = 0;
            }
        } else {
            consecutiveNoFall = Math.max(0, consecutiveNoFall - 1);
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
