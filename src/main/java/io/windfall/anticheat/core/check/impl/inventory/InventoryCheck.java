package io.windfall.anticheat.core.check.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects inhuman inventory click speeds and unauthorized creative inventory access.
 *
 * <p>Detection strategy (two independent checks):
 * <ul>
 *   <li><b>Fast-click burst detection</b> &mdash; when two consecutive clicks occur within
 *       {@value #CLICK_WINDOW_MS}ms, increments a rapid-click counter. If more than 5 rapid
 *       clicks accumulate, buffer increases by 1.0 (flags at buffer >3.0). Detects autoclickers
 *       that manipulate inventory at inhuman speeds.</li>
 *   <li><b>Per-second rate limit</b> &mdash; tracks total clicks per 1-second window. If more than
 *       {@value #MAX_CLICKS_PER_SECOND} clicks occur in a second, flags immediately. This is a
 *       hard safety net for sustained automated inventory manipulation.</li>
 *   <li><b>Creative mode validation</b> &mdash; flags if a {@code CREATIVE_INVENTORY_ACTION} packet
 *       is received in a non-creative game mode (impossible without hacks).</li>
 * </ul>
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value #CLICK_WINDOW_MS}ms minimum between clicks for rapid-click detection</li>
 *   <li>5 rapid clicks required before buffer increases</li>
 *   <li>{@value #MAX_CLICKS_PER_SECOND} hard cap per second</li>
 * </ul>
 *
 * <p>Setback at VL 15, decay 0.02/tick (fast decay). Uses RELAX_ON_MISMATCH with 1.3x multiplier.
 *
 * @see ChestStealerCheck for chest-specific click validation
 * @see CreativeCheck for creative inventory rate limiting
 * @see ExploitCheck for invalid slot/window ID detection
 */
@CheckData(name = "Inventory A", stableKey = "windfall.inventory.inventory", decay = 0.02, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class InventoryCheck extends Check implements PacketCheck {

    /** Hard cap on clicks per second — exceeded triggers immediate flag */
    private static final int MAX_CLICKS_PER_SECOND = 20;
    /** Minimum interval (ms) between clicks for rapid-click burst detection */
    private static final long CLICK_WINDOW_MS = 50;

    /**
     * Per-player state tracking inventory click rates.
     */
    private static final class PlayerState {
        /** Timestamp of the last click for rapid-click interval detection */
        long lastClickTime;
        /** Count of rapid clicks within the burst detection window */
        int clickCount;
        /** Number of clicks in the current 1-second window */
        int clicksThisSecond;
        /** Start time of the current 1-second rate window */
        long secondStart;
    }

    /** Thread-safe map of player UUID to their inventory click state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player inventory click state.
     *
     * @param player the player to get state for
     * @return the player's state
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming packets for inventory click speed validation.
     * Handles CLICK_WINDOW for rate limiting and CREATIVE_INVENTORY_ACTION for mode validation.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            handleClickWindow(player, event);
        } else if (type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            handleCreativeSlot(player, event);
        }
    }

    /** {@inheritDoc} No outgoing packet processing needed for inventory checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Processes window click packets for rapid-click and per-second rate limiting.
     * Rapid-click detection: if two consecutive clicks are within {@value #CLICK_WINDOW_MS}ms,
     * the rapid-click counter increments. Counter resets if the gap exceeds the window.
     * Per-second counter resets every 1000ms.
     *
     * @param player the player who clicked
     * @param event  the packet event
     */
    private void handleClickWindow(WindfallPlayer player, PacketReceiveEvent event) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        if (now - state.lastClickTime < CLICK_WINDOW_MS) {
            state.clickCount++;
            if (state.clickCount > 5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.clickCount = 0;
        }
        state.lastClickTime = now;

        if (now - state.secondStart > 1000) {
            state.clicksThisSecond = 0;
            state.secondStart = now;
        }
        state.clicksThisSecond++;
        if (state.clicksThisSecond > MAX_CLICKS_PER_SECOND) {
            flag(player);
        }
    }

    /**
     * Validates creative inventory action packets — flags if the player is not in Creative mode.
     * This is impossible for legitimate clients and indicates a hacked client.
     *
     * @param player the player who sent the packet
     * @param event  the packet event
     */
    private void handleCreativeSlot(WindfallPlayer player, PacketReceiveEvent event) {
        if (!player.getPlayer().getGameMode().equals(org.bukkit.GameMode.CREATIVE)) {
            flag(player);
        }
    }
}
