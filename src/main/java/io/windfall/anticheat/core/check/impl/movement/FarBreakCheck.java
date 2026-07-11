package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects players breaking blocks from distances exceeding vanilla reach.
 * Vanilla maximum reach is ~4.5 blocks; this check uses 5.0 as tolerance.
 */
@CheckData(name = "Far Break A", stableKey = "windfall.movement.farbreak", decay = 0.01, setbackVl = 15)
public class FarBreakCheck extends Check implements PacketCheck {

    private static final double MAX_REACH = 5.0;
    private static final double TOLERANCE = 0.3;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action != DiggingAction.START_DIGGING && action != DiggingAction.FINISHED_DIGGING) {
            return;
        }

        int blockX = wrapper.getBlockPosition().getX();
        int blockY = wrapper.getBlockPosition().getY();
        int blockZ = wrapper.getBlockPosition().getZ();

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
