package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Detects block placements that have no supporting block beneath them.
 *
 * <p>Vanilla requires most placeable blocks to rest on a solid surface. A client that places
 * with air underneath is either spoofing its own inventory state or applying a "no-scaffold"
 * physics module that lets it bridge mid-air — the same class of cheat
 * {@link ScaffoldCheck} targets from the movement side.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>On each placement, read the target block position from the packet.</li>
 *   <li>If the block type requires support, check the block directly below it.</li>
 *   <li>Air, liquids and replaceable decoration below a support-requiring block is a violation.</li>
 * </ol>
 *
 * <p>Support rules are type-specific, mirroring vanilla's {@code canBeReplaced} and
 * {@code isValidSpawn} tables:</p>
 * <ul>
 *   <li><b>Below-required</b> — solid building blocks: if below is air, the client placed
 *       a block floating in space.</li>
 *   <li><b>Above-required</b> — torches, rails, signs, ladders, saplings: the block beneath
 *       must be solid, or the placed block would immediately break client-side.</li>
 *   <li><b>Side-required</b> — wall-attached blocks: checked against the claimed face.</li>
 *   <li><b>No support required</b> — air, fluids, torches on floors, and anything in the
 *       allow list. These are skipped entirely.</li>
 * </ul>
 *
 * <p>Unknown or newly added materials default to no support requirement. Defaulting the other
 * way would flag legitimate placements of any block added after this list was written, which
 * is the worse failure mode for a compatibility-focused anti-cheat.</p>
 *
 * <p>Relief is read from the server-side world, not the client, so a block the client believes
 * it placed but the server has not yet applied is correctly seen as missing support. That lag
 * is why violations accumulate into a buffer rather than flagging on the first occurrence.</p>
 *
 * @see ScaffoldCheck — movement-side scaffold detection
 * @see InvalidPlaceCheck — companion check for invalid placement state
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Scaffold Support", stableKey = "windfall.movement.scaffoldsupport", decay = 0.02, setbackVl = 10,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class ScaffoldSupportCheck extends Check implements PacketCheck {

    /** Buffer threshold — relief application lags by a tick, so accumulate before flagging. */
    private static final double BUFFER_THRESHOLD = 4.0;

    /** Blocks that never require support and are therefore skipped. */
    private static final Set<String> NO_SUPPORT_REQUIRED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "AIR", "CAVE_AIR", "VOID_AIR", "WATER", "LAVA",
        "TORCH", "SOUL_TORCH", "REDSTONE_TORCH", "SOUL_CAMPFIRE", "CAMPFIRE",
        "SUGAR_CANE", "WHEAT", "CARROTS", "POTATOES", "BEETROOTS", "NETHER_WHEAT", "COCOA",
        "TORCHFLOWER", "PITCHER_PLANT", "SWEET_BERRY_BUSH", "LILY_PAD", "VINE",
        "RAIL", "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL", "SLIME_BLOCK", "HONEY_BLOCK",
        "REDSTONE_WIRE", "GLOW_LICHEN", "SCAFFOLDING", "SNOW", "ICE", "FROSTED_ICE"
    )));

    /** Blocks that must rest on a solid surface directly below. */
    private static final Set<String> SUPPORT_BELOW = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "DIRT", "GRASS_BLOCK", "STONE", "COBBLESTONE", "MOSSY_COBBLESTONE", "OAK_PLANKS", "SPRUCE_PLANKS",
        "BIRCH_PLANKS", "JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS", "SAND", "RED_SAND",
        "GRAVEL", "SANDSTONE", "BRICKS", "STONE_BRICKS", "MOSSY_STONE_BRICKS", "CRACKED_STONE_BRICKS",
        "CHISELED_STONE_BRICKS", "NETHER_BRICKS", "RED_NETTER_BRICKS", "QUARTZ_BLOCK", "OBSIDIAN",
        "CRYING_OBSIDIAN", "NETHERRACK", "END_STONE", "END_STONE_BRICKS", "PURPUR_BLOCK", "PURPUR_PILLAR",
        "IRON_BLOCK", "GOLD_BLOCK", "DIAMOND_BLOCK", "EMERALD_BLOCK", "NETHERITE_BLOCK", "LAPIS_BLOCK",
        "REDSTONE_BLOCK", "COAL_BLOCK", "HOPPER", "DISPENSER", "DROPPER", "OBSERVER", "PISTON",
        "STICKY_PISTON", "FURNACE", "BLAST_FURNACE", "SMOKER", "CHEST", "TRAPPED_CHEST", "ENDER_CHEST",
        "FURNACE", "BOOKSHELF", "TNT", "GLOWSTONE", "SEA_LANTERN", "SHROOMLIGHT", "BEACON", "CONDUIT",
        "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "BREWING_STAND", "CAULDRON", "ENCHANTING_TABLE",
        "END_PORTAL_FRAME", "ENDER_CHEST", "RESPAWN_ANCHOR", "BELL", "LANTERN", "SOUL_LANTERN", "CHAIN",
        "BARREL", "SMOKER", "LOOM", "CARTOGRAPHY_TABLE", "FLETCHING_TABLE", "SMITHING_TABLE", "STONECUTTER",
        "GRINDSTONE", "LECTERN", "COMPOSTER", "JIGSAW", "HONEYCOMB_BLOCK", "HONEY_BLOCK", "SLIME_BLOCK",
        "AMETHYST_CLUSTER", "POINTED_DRIPSTONE", "SCULK_SHRIEKER", "SCULK_SENSOR", "CALIBRATED_SCULK_SENSOR",
        "TURTLE_EGG", "DEEPSLATE", "TUFF", "CALCITE", "DRIPSTONE_BLOCK", "MUD", "PACKED_MUD", "MUD_BRICKS",
        "SCULK", "SCULK_BLOCK", "REINFORCED_DEEPSLATE", "RAW_IRON_BLOCK", "RAW_COPPER_BLOCK", "RAW_GOLD_BLOCK"
    )));

    /**
     * Determines whether a placed block needs a solid block beneath it.
     *
     * <p>Unknown materials return {@code false} so newly added blocks are not flagged.</p>
     *
     * @param material the placed block material
     * @return true if the block requires support below
     */
    static boolean requiresSupportBelow(Material material) {
        if (material == null) return false;
        String name = material.name();
        if (NO_SUPPORT_REQUIRED.contains(name)) return false;
        return SUPPORT_BELOW.contains(name);
    }

    /**
     * Whether a block below the target would satisfy support requirements.
     *
     * @param material the block below the placement
     * @return true if the block counts as support
     */
    static boolean countsAsSupport(Material material) {
        if (material == null) return false;
        if (material.isSolid()) return true;
        String name = material.name();
        /* Slime and honey blocks support entities but are not solid for placement purposes. */
        return name.equals("ICE") || name.equals("FROSTED_ICE") || name.equals("PACKED_ICE")
            || name.equals("BLUE_ICE") || name.equals("BUBBLE_COLUMN");
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        Vector3i place = new WrapperPlayClientPlayerBlockPlacement(event).getBlockPosition();
        if (place == null) return;

        try {
            World world = player.getPlayer().getWorld();
            int bx = place.getX();
            int by = place.getY();
            int bz = place.getZ();

            Material placed = world.getBlockAt(bx, by, bz).getType();

            /* The target block may already have been applied server-side, or not yet. When it
             * is still air we cannot identify the block type, so the check cannot apply. */
            if (!requiresSupportBelow(placed)) {
                reward(player);
                return;
            }

            Material below = world.getBlockAt(bx, by - 1, bz).getType();

            if (!countsAsSupport(below)) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                reward(player);
            }
        } catch (Exception e) {
            /* World access can fail transiently on Folia region boundaries; do not flag. */
            reward(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}