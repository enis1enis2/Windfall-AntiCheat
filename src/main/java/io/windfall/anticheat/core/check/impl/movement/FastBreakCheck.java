package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Material;

/**
 * Detects players breaking blocks faster than vanilla allows.
 * Tracks break start/abort/dig times and compares against vanilla block hardness.
 */
@CheckData(name = "Fast Break A", stableKey = "windfall.movement.fastbreak", decay = 0.02, setbackVl = 20)
public class FastBreakCheck extends Check implements PacketCheck {

    private static final double MAX_FAST_BREAK_MULTIPLIER = 0.85;
    private static final int BUFFER_THRESHOLD = 3;

    private long breakStartTime;
    private boolean breaking;
    private int buffer;
    private int blockX, blockY, blockZ;
    private Material blockType;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action == DiggingAction.START_DIGGING) {
            breaking = true;
            breakStartTime = System.currentTimeMillis();
            blockX = wrapper.getBlockPosition().getX();
            blockY = wrapper.getBlockPosition().getY();
            blockZ = wrapper.getBlockPosition().getZ();
            try {
                blockType = player.getPlayer().getWorld()
                    .getBlockAt(blockX, blockY, blockZ).getType();
            } catch (Exception e) {
                blockType = Material.STONE;
            }
        } else if (action == DiggingAction.CANCELLED_DIGGING) {
            reset();
        } else if (action == DiggingAction.FINISHED_DIGGING) {
            if (!breaking || blockType == null) {
                reset();
                return;
            }

            long elapsed = System.currentTimeMillis() - breakStartTime;
            double vanillaTime = getVanillaBreakTime(blockType);
            double maxAllowed = vanillaTime * MAX_FAST_BREAK_MULTIPLIER * 1000.0;

            if (elapsed < maxAllowed && vanillaTime > 0) {
                buffer++;
                if (buffer >= BUFFER_THRESHOLD) {
                    flag(player);
                    buffer = 0;
                }
            } else {
                buffer = Math.max(0, buffer - 1);
            }
            reset();
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double getVanillaBreakTime(Material material) {
        String name = material.name();
        if (name.equals("OBSIDIAN") || name.equals("END_PORTAL_FRAME")) return 50.0;
        if (name.equals("ENCHANTING_TABLE") || name.equals("ANVIL")) return 5.0;
        if (name.equals("IRON_ORE") || name.equals("DEEPSLATE_IRON_ORE")) return 3.0;
        if (name.equals("DIAMOND_ORE") || name.equals("DEEPSLATE_DIAMOND_ORE")) return 5.0;
        if (name.equals("EMERALD_ORE") || name.equals("DEEPSLATE_EMERALD_ORE")) return 5.0;
        if (name.equals("STONE") || name.equals("COBBLESTONE") || name.equals("DEEPSLATE")) return 1.5;
        if (name.equals("DIRT") || name.equals("GRASS_BLOCK") || name.equals("SAND")) return 0.5;
        if (name.equals("WOOD") || name.contains("PLANKS") || name.contains("_LOG")) return 2.0;
        if (material.isBlock() && material.isSolid()) return 1.0;
        return 0.5;
    }

    private void reset() {
        breaking = false;
        breakStartTime = 0;
        blockType = null;
    }
}
