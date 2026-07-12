package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects automated chest stealing (inventory manipulation at inhuman speeds).
 *
 * <p>Detection strategy:
 * <ul>
 *   <li><b>Per-window click limit</b> &mdash; flags immediately if more than {@value #MAX_CLICKS_PER_WINDOW}
 *       clicks occur in a single window session (hard cap, no buffer needed)</li>
 *   <li><b>Fast-click burst detection</b> &mdash; tracks clicks in a {@value #FAST_CLICK_WINDOW_MS}ms sliding
 *       window. Flags if more than {@value #FAST_CLICK_THRESHOLD} clicks in that window (buffer +1.0, flags at buffer >5.0)</li>
 *   <li><b>Short session detection</b> &mdash; when a window closes within {@value #WINDOW_TIMEOUT_MS}ms of opening
 *       and more than {@value #MAX_ITEMS_PER_SECOND} items were clicked, it suggests automated looting
 *       (buffer +0.5, flags at buffer >3.0)</li>
 * </ul>
 *
 * <p>Tracks window open/close via {@code OPEN_WINDOW} (server-to-client) and {@code CLOSE_WINDOW} (client-to-server).
 * Buffer decreases by 0.05/tick for gradual recovery when click rates are normal.
 *
 * <p>Setback at VL 15, decay rate 0.01/tick.
 *
 * @see InventoryCheck for general inventory click speed validation
 * @see ExploitCheck for invalid slot/window ID detection
 */
@CheckData(name = "Chest Stealer A", stableKey = "windfall.packet.cheststealer", decay = 0.01, setbackVl = 15)
public class ChestStealerCheck extends Check implements PacketCheck {

    /** Hard cap on total clicks allowed per window session — exceeds this triggers immediate flag */
    private static final int MAX_CLICKS_PER_WINDOW = 40;
    /** Maximum time (ms) for a window to be considered a "short session" (auto-looting heuristic) */
    private static final long WINDOW_TIMEOUT_MS = 3000;
    /** Maximum clicks allowed in the fast-click sliding window before buffer increases */
    private static final int FAST_CLICK_THRESHOLD = 6;
    /** Duration of the fast-click detection window in milliseconds */
    private static final long FAST_CLICK_WINDOW_MS = 500;
    /** Maximum items that can be legitimately moved per second in a short session */
    private static final int MAX_ITEMS_PER_SECOND = 15;

    /**
     * Per-player state tracking window session and click timing.
     */
    private static final class PlayerState {
        /** Number of clicks in the current window session */
        int clicksThisWindow;
        /** Timestamp when the current window was opened */
        long windowOpenTime;
        /** Whether a window is currently open (set by OPEN_WINDOW, cleared by CLOSE_WINDOW) */
        boolean windowOpen;
        /** Timestamps of recent clicks for burst detection (sliding window) */
        final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();
    }

    /** Thread-safe map of player UUID to their chest stealing state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player chest stealing state.
     *
     * @param player the player to get state for
     * @return the player's state
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** {@inheritDoc} Clears player state to prevent memory leaks on disconnect */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Handles incoming packets: CLICK_WINDOW for click rate tracking, CLOSE_WINDOW for
     * short-session detection.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            handleClick(player, now, state);
        } else if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            handleClose(player, now, state);
        }
    }

    /**
     * Tracks window open events from the server to initialize session state.
     *
     * @param player the player who received the packet
     * @param event  the outgoing packet event
     */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            PlayerState state = getState(player);
            state.windowOpen = true;
            state.windowOpenTime = System.currentTimeMillis();
            state.clicksThisWindow = 0;
        }
    }

    /**
     * Processes a window click: checks per-window click cap and fast-click burst rate.
     * Evicts click timestamps older than {@value #FAST_CLICK_WINDOW_MS}ms from the sliding window.
     *
     * @param player the player who clicked
     * @param now    current system time in milliseconds
     * @param state  the player's check state
     */
    private void handleClick(WindfallPlayer player, long now, PlayerState state) {
        if (!state.windowOpen) return;

        state.clicksThisWindow++;
        state.clickTimestamps.addLast(now);
        /* Evict click timestamps outside the burst detection window */
        while (!state.clickTimestamps.isEmpty() && now - state.clickTimestamps.peekFirst() > FAST_CLICK_WINDOW_MS) {
            state.clickTimestamps.removeFirst();
        }

        if (state.clicksThisWindow > MAX_CLICKS_PER_WINDOW) {
            flag(player);
            return;
        }

        if (state.clickTimestamps.size() > FAST_CLICK_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.05);
        }
    }

    /**
     * Handles window close events. Detects short-session auto-looting: if the window was open
     * for less than {@value #WINDOW_TIMEOUT_MS}ms but more than {@value #MAX_ITEMS_PER_SECOND}
     * items were clicked, flags for potential chest stealer.
     *
     * @param player the player who closed the window
     * @param now    current system time in milliseconds
     * @param state  the player's check state
     */
    private void handleClose(WindfallPlayer player, long now, PlayerState state) {
        if (!state.windowOpen) return;

        long windowDuration = now - state.windowOpenTime;
        if (windowDuration < WINDOW_TIMEOUT_MS && state.clicksThisWindow > MAX_ITEMS_PER_SECOND) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.windowOpen = false;
        state.clicksThisWindow = 0;
    }
}
