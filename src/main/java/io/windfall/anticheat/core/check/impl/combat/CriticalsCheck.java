package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Criticals A", stableKey = "windfall.combat.criticals", decay = 0.01, setbackVl = 10)
public class CriticalsCheck extends Check implements PacketCheck {

    private static final double MIN_DELTA_Y_CRITICAL = 0.11;
    private static final double MAX_DELTA_Y_CRITICAL = 0.5;

    private int attacksSinceGround;
    private int consecutiveInvalid;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttack(player);
            }
        } else if (isMovementPacket(type)) {
            handleMovement(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleAttack(WindfallPlayer player) {
        if (player.isOnGround()) {
            consecutiveInvalid = Math.max(0, consecutiveInvalid - 1);
            return;
        }

        double deltaY = player.getDeltaY();
        boolean validCritMotion = deltaY > MIN_DELTA_Y_CRITICAL && deltaY < MAX_DELTA_Y_CRITICAL;

        if (!validCritMotion && deltaY >= -0.01) {
            consecutiveInvalid++;
            if (consecutiveInvalid >= 4) {
                flagWithSetback(player);
                consecutiveInvalid = 0;
            }
        } else {
            consecutiveInvalid = Math.max(0, consecutiveInvalid - 1);
        }
    }

    private void handleMovement(WindfallPlayer player) {
        if (player.isOnGround()) {
            attacksSinceGround = 0;
        } else {
            attacksSinceGround++;
        }
    }

    private boolean isMovementPacket(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
