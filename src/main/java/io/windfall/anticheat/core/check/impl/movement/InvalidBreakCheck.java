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
import org.bukkit.Material;

/**
 * Detects invalid block breaking: breaking air, breaking bedrock, or
 * breaking blocks that cannot be broken in survival.
 */
@CheckData(name = "Invalid Break A", stableKey = "windfall.movement.invalidbreak", decay = 0.02, setbackVl = 10)
public class InvalidBreakCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        int x = wrapper.getBlockPosition().getX();
        int y = wrapper.getBlockPosition().getY();
        int z = wrapper.getBlockPosition().getZ();

        try {
            Material type = player.getPlayer().getWorld().getBlockAt(x, y, z).getType();

            if (type == Material.AIR) {
                flagDetail(player, "breaking air at " + x + "," + y + "," + z);
                return;
            }

            String name = type.name();
            if (name.equals("BEDROCK") || name.equals("BARRIER") || name.equals("END_PORTAL")
                    || name.equals("END_PORTAL_FRAME") || name.equals("COMMAND_BLOCK")
                    || name.equals("STRUCTURE_BLOCK") || name.equals("JIGSAW_BLOCK")) {
                flagDetail(player, "breaking protected block " + name);
                return;
            }
        } catch (Exception e) {
            // World access failed — skip
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
        logger.warning("[Invalid Break A] " + player.getName() + ": " + detail);
    }
}
