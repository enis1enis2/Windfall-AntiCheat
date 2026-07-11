package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects players placing blocks from distances exceeding vanilla reach.
 * Uses the same 4.5+0.5 tolerance as FarBreakCheck for consistency.
 */
@CheckData(name = "Far Place A", stableKey = "windfall.movement.farplace", decay = 0.01, setbackVl = 15)
public class FarPlaceCheck extends Check implements PacketCheck {

    private static final double MAX_REACH = 5.0;
    private static final double TOLERANCE = 0.3;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        var position = wrapper.getBlockPosition();

        int blockX = position.getX();
        int blockY = position.getY();
        int blockZ = position.getZ();

        double centerX = blockX + 0.5;
        double centerY = blockY + 0.5;
        double centerZ = blockZ + 0.5;

        double distance = Math.sqrt(
            Math.pow(player.getX() - centerX, 2) +
            Math.pow((player.getY() + player.getHeight() / 2.0) - centerY, 2) +
            Math.pow(player.getZ() - centerZ, 2)
        );

        if (distance > MAX_REACH + TOLERANCE) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (distance > MAX_REACH) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
