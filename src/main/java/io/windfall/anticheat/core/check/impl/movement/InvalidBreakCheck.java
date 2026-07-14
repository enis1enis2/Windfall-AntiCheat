package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Material;

/**
 * Detects invalid block-breaking attempts that are impossible in vanilla
 * survival mode. Specifically flags:
 * <ul>
 *   <li>Breaking air (no block at the target position).</li>
 *   <li>Breaking indestructible / protected blocks such as bedrock, barriers,
 *       end portals, command blocks, structure blocks, and jigsaw blocks.</li>
 * </ul>
 *
 * <p><b>Algorithm:</b> On every {@code START_DIGGING} packet the server-side
 * block type at the target coordinates is checked. If the block is air or
 * belongs to the protected-set, the player is immediately flagged with a
 * detailed reason logged for staff review.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Invalid Break A", stableKey = "windfall.movement.invalidbreak", decay = 0.02, setbackVl = 10)
public class InvalidBreakCheck extends Check implements PacketCheck {

    /**
     * Validates the target block on every START_DIGGING packet. Flags if the
     * block is air or an indestructible/protected material.
     *
     * @param player the player associated with this packet
     * @param event  the incoming digging packet
     */
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
            /** Indestructible / creative-only blocks that cannot be broken in survival. */
            if (name.equals("BEDROCK") || name.equals("BARRIER") || name.equals("END_PORTAL")
                    || name.equals("END_PORTAL_FRAME") || name.equals("COMMAND_BLOCK")
                    || name.equals("STRUCTURE_BLOCK") || name.equals("JIGSAW_BLOCK")) {
                flagDetail(player, "breaking protected block " + name);
                return;
            }
        } catch (Exception e) {
            WindfallPlugin.getInstance().getLogger().fine("InvalidBreakCheck: world-access exception — " + e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Flags the player and logs a descriptive warning with the specific reason
     * for the invalid break (air vs. protected block) for staff review.
     *
     * @param player the player to flag
     * @param detail human-readable description of the violation
     */
    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
        logger.warning("[Invalid Break A] " + player.getName() + ": " + detail);
    }
}
