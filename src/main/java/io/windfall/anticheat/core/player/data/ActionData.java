package io.windfall.anticheat.core.player.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Tracks block-level actions to provide exemptions for movement checks.
 *
 * <p>Monitors block placement, block breaking, piston pushes, and server-side block
 * updates under the player's feet. Movement checks use these tick counters to
 * temporarily widen tolerance after world changes that can cause legitimate
 * desync between client and server positions.
 *
 * <p>Key exemption methods:
 * <ul>
 *   <li>{@link #hasRecentPistonUpdate(int)} — piston pushed a block near the player</li>
 *   <li>{@link #hasRecentBlockUpdateUnder(int)} — server sent a block change under the player</li>
 *   <li>{@link #hasRecentConfirmedUnderPlace(int)} — a placed block was confirmed under the player</li>
 *   <li>{@link #hasRecentConfirmedUnderBreak(int)} — a broken block was confirmed under the player</li>
 * </ul>
 *
 * <p>Thread safety: tick counters are plain ints updated only from the main thread
 * via packet callbacks. Checks read these from Netty threads, but stale reads
 * are acceptable (worst case: one extra tick of tolerance).
 *
 * @see WindfallPlayer for per-player state
 */
public class ActionData {

    private static final int MAX_TICKS = 1000;

    // Tick counters — reset to 0 when the event occurs, incremented each tick
    private int sincePistonUpdateTicks = MAX_TICKS;
    private int sinceBlockUpdateUnderTicks = MAX_TICKS;
    private int sinceConfirmedUnderPlaceTicks = MAX_TICKS;
    private int sinceConfirmedUnderBreakTicks = MAX_TICKS;
    private int sinceBlockPlaceAttemptTicks = MAX_TICKS;

    private final WindfallPlayer player;

    public ActionData(WindfallPlayer player) {
        this.player = player;
    }

    // ========================================================================
    // Public query methods — called by movement checks for exemptions
    // ========================================================================

    /**
     * Returns true if a piston-related block update occurred near the player
     * within the given number of ticks.
     *
     * @param ticks maximum age in ticks to consider
     * @return true if a piston update happened recently
     */
    public boolean hasRecentPistonUpdate(int ticks) {
        return sincePistonUpdateTicks <= ticks;
    }

    /**
     * Returns true if a server-side block change under the player occurred
     * within the given number of ticks.
     *
     * @param ticks maximum age in ticks to consider
     * @return true if a block update happened recently
     */
    public boolean hasRecentBlockUpdateUnder(int ticks) {
        return sinceBlockUpdateUnderTicks <= ticks;
    }

    /**
     * Returns true if a block placement under the player was confirmed
     * (server accepted it) within the given number of ticks.
     *
     * @param ticks maximum age in ticks to consider
     * @return true if a confirmed under-place happened recently
     */
    public boolean hasRecentConfirmedUnderPlace(int ticks) {
        return sinceConfirmedUnderPlaceTicks <= ticks;
    }

    /**
     * Returns true if a block break under the player was confirmed
     * (server removed the block) within the given number of ticks.
     *
     * @param ticks maximum age in ticks to consider
     * @return true if a confirmed under-break happened recently
     */
    public boolean hasRecentConfirmedUnderBreak(int ticks) {
        return sinceConfirmedUnderBreakTicks <= ticks;
    }

    /**
     * Returns true if the player attempted to place a block (sent the packet)
     * within the given number of ticks, regardless of server confirmation.
     *
     * @param ticks maximum age in ticks to consider
     * @return true if a block place attempt happened recently
     */
    public boolean hasRecentBlockPlaceAttempt(int ticks) {
        return sinceBlockPlaceAttemptTicks <= ticks;
    }

    // ========================================================================
    // Packet processing — called from PacketListener
    // ========================================================================

    /**
     * Processes incoming packets to detect client-side block actions.
     *
     * @param event the incoming packet event
     */
    public void processReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            handleBlockPlace(event);
        } else if (type == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.PLAYER_DIGGING) {
            handleBlockDig(event);
        }
    }

    /**
     * Processes outgoing packets to detect server-side block changes.
     *
     * @param event the outgoing packet event
     */
    public void processSend(PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.BLOCK_CHANGE) {
            handleBlockChange(event);
        }
        // MULTI_BLOCK_CHANGE is not handled due to API differences across protocol versions
    }

    /**
     * Increments all tick counters. Called once per server tick from CheckManager.
     */
    public void tick() {
        sincePistonUpdateTicks = Math.min(sincePistonUpdateTicks + 1, MAX_TICKS);
        sinceBlockUpdateUnderTicks = Math.min(sinceBlockUpdateUnderTicks + 1, MAX_TICKS);
        sinceConfirmedUnderPlaceTicks = Math.min(sinceConfirmedUnderPlaceTicks + 1, MAX_TICKS);
        sinceConfirmedUnderBreakTicks = Math.min(sinceConfirmedUnderBreakTicks + 1, MAX_TICKS);
        sinceBlockPlaceAttemptTicks = Math.min(sinceBlockPlaceAttemptTicks + 1, MAX_TICKS);
    }

    // ========================================================================
    // Internal handlers
    // ========================================================================

    private void handleBlockPlace(PacketReceiveEvent event) {
        try {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            Vector3i blockPos = wrapper.getBlockPosition();
            if (blockPos == null) return;

            sinceBlockPlaceAttemptTicks = 0;

            if (isBlockUnderPlayer(blockPos.getY())) {
                sinceConfirmedUnderPlaceTicks = 0;
            }
        } catch (Exception ignored) {
        }
    }

    private void handleBlockDig(PacketReceiveEvent event) {
        try {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = wrapper.getAction();
            if (action != DiggingAction.FINISHED_DIGGING) return;

            Vector3i blockPos = wrapper.getBlockPosition();
            if (blockPos == null) return;

            if (isBlockUnderPlayer(blockPos.getY())) {
                sinceConfirmedUnderBreakTicks = 0;
            }
        } catch (Exception ignored) {
        }
    }

    private void handleBlockChange(PacketSendEvent event) {
        try {
            WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
            Vector3i blockPos = wrapper.getBlockPosition();
            if (blockPos == null) return;

            int blockId = wrapper.getBlockId();

            if (isPistonBlock(blockId)) {
                sincePistonUpdateTicks = 0;
            }

            if (isBlockUnderPlayer(blockPos.getY())) {
                sinceBlockUpdateUnderTicks = 0;
            }
        } catch (Exception ignored) {
        }
    }

    // ========================================================================
    // Utility methods
    // ========================================================================

    /**
     * Checks if a block Y-coordinate is at or below the player's feet.
     */
    private boolean isBlockUnderPlayer(int blockY) {
        return blockY <= (int) Math.floor(player.getY());
    }

    /**
     * Returns true if the given block ID corresponds to a piston block.
     * Uses legacy numeric IDs (pre-1.13): 29=sticky_piston, 33=piston,
     * 34=piston_head, 36=moving_piston.
     */
    private static boolean isPistonBlock(int blockId) {
        return blockId == 29 || blockId == 33 || blockId == 34 || blockId == 36;
    }
}
