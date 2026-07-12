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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;

/**
 * Detects players who break blocks faster than vanilla survival allows.
 * Measures the elapsed wall-clock time between {@code START_DIGGING} and
 * {@code FINISHED_DIGGING} packets and compares it against a per-material
 * vanilla break-time table.
 *
 * <p><b>Algorithm:</b> On START_DIGGING the block type and timestamp are
 * recorded. On FINISHED_DIGGING the elapsed time is compared to
 * {@link #getVanillaBreakTime(Material)} * {@link #MAX_FAST_BREAK_MULTIPLIER}.
 * If the player breaks the block in less than 85% of the vanilla time,
 * the buffer increases. Three consecutive fast breaks trigger a flag.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Fast Break A", stableKey = "windfall.movement.fastbreak", decay = 0.02, setbackVl = 20)
public class FastBreakCheck extends Check implements PacketCheck {

    /**
     * A block break is flagged only if the elapsed time is less than this
     * fraction of the vanilla break time. 0.85 allows 15% tolerance for
     * latency and rounding.
     */
    private static final double MAX_FAST_BREAK_MULTIPLIER = 0.85;

    /** Buffer level at which the player is flagged after consecutive fast breaks. */
    private static final int BUFFER_THRESHOLD = 3;

    /** Per-player state tracking the in-progress block break. */
    private static final class PlayerState {
        /** {@link System#currentTimeMillis()} when START_DIGGING was received. */
        long breakStartTime;
        /** Whether a break is currently in progress (START received, not yet FINISHED). */
        boolean breaking;
        /** X coordinate of the block being broken. */
        int blockX;
        /** Y coordinate of the block being broken. */
        int blockY;
        /** Z coordinate of the block being broken. */
        int blockZ;
        /** Material of the block at the break start, used for vanilla-time lookup. */
        Material blockType;

        /** Resets the break state back to idle. */
        void reset() {
            breaking = false;
            breakStartTime = 0;
            blockType = null;
        }
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Processes PLAYER_DIGGING packets to track break start, cancel, and finish
     * events. Compares actual break duration against the vanilla baseline.
     *
     * @param player the player associated with this packet
     * @param event  the incoming digging packet
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();
        PlayerState state = getState(player);

        if (action == DiggingAction.START_DIGGING) {
            state.breaking = true;
            state.breakStartTime = System.currentTimeMillis();
            state.blockX = wrapper.getBlockPosition().getX();
            state.blockY = wrapper.getBlockPosition().getY();
            state.blockZ = wrapper.getBlockPosition().getZ();
            try {
                state.blockType = player.getPlayer().getWorld()
                    .getBlockAt(state.blockX, state.blockY, state.blockZ).getType();
            } catch (Exception e) {
                /** Fallback to stone if world access fails — conservative default. */
                state.blockType = Material.STONE;
            }
        } else if (action == DiggingAction.CANCELLED_DIGGING) {
            state.reset();
        } else if (action == DiggingAction.FINISHED_DIGGING) {
            if (!state.breaking || state.blockType == null) {
                state.reset();
                return;
            }

            long elapsed = System.currentTimeMillis() - state.breakStartTime;
            double vanillaTime = getVanillaBreakTime(state.blockType);
            /** Maximum allowed break time in milliseconds (vanilla * 0.85 tolerance). */
            double maxAllowed = vanillaTime * MAX_FAST_BREAK_MULTIPLIER * 1000.0;

            if (elapsed < maxAllowed && vanillaTime > 0) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 1.0);
            }
            state.reset();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Returns the approximate vanilla break time (in seconds) for a given material
     * using an unenchanted diamond pickaxe. Values are approximate and cover common
     * block types. Unrecognised solid blocks default to 1.0 s; non-solid to 0.5 s.
     *
     * @param material the block material to look up
     * @return vanilla break time in seconds
     */
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
}
