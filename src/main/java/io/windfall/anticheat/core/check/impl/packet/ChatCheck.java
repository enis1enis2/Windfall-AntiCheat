package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects chat flooding and spam via rate limiting.
 *
 * <p>Detection strategy uses two independent sliding windows:
 * <ul>
 *   <li><b>Per-minute window</b> &mdash; tracks all chat messages within the last 60 seconds.
 *       Flags if more than {@value #MAX_CHAT_PER_MINUTE} messages/minute (buffer +2.0, flags at buffer >3.0)</li>
 *   <li><b>Burst window</b> &mdash; tracks messages within a {@value #CHAT_BURST_WINDOW_MS}ms window.
 *       Flags if more than {@value #MAX_CHAT_BURST} messages in that window (buffer +1.0, flags at buffer >5.0)</li>
 * </ul>
 *
 * <p>The per-minute check is stricter (lower threshold) because sustained spam is more disruptive
 * than short bursts. Buffer decreases by 0.2/tick when below all thresholds for gradual recovery.
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value #MAX_CHAT_PER_SECOND} messages per second (unused, subsumed by burst check)</li>
 *   <li>{@value #MAX_CHAT_PER_MINUTE} messages per 60 seconds</li>
 *   <li>{@value #MAX_CHAT_BURST} messages in a {@value #CHAT_BURST_WINDOW_MS}ms window</li>
 * </ul>
 *
 * <p>Setback at VL 15, decay rate 0.01/tick. Uses RELAX_ON_MISMATCH for compatibility.
 *
 * @see Check for base violation/buffer system
 */
@CheckData(name = "Chat A", stableKey = "windfall.packet.chat", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class ChatCheck extends Check implements PacketCheck {

    /** Messages per second threshold (informational, burst window is more effective) */
    private static final int MAX_CHAT_PER_SECOND = 5;
    /** Maximum chat messages allowed in a 60-second sliding window before flagging */
    private static final int MAX_CHAT_PER_MINUTE = 60;
    /** Duration of the burst detection window in milliseconds */
    private static final long CHAT_BURST_WINDOW_MS = 2000;
    /** Maximum chat messages allowed in the burst window before flagging */
    private static final int MAX_CHAT_BURST = 4;

    /**
     * Per-player state holding sliding window timestamps for rate limit detection.
     */
    private static final class PlayerState {
        /** Timestamps of all chat messages within the last 60 seconds (per-minute check) */
        final ArrayDeque<Long> chatTimestamps = new ArrayDeque<>();
        /** Timestamps of chat messages within the burst window (burst check) */
        final ArrayDeque<Long> chatBurstWindow = new ArrayDeque<>();
    }

    /** Thread-safe map of player UUID to their chat rate-limiting state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player chat state.
     *
     * @param player the player to get state for
     * @return the player's chat state
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
     * Processes incoming chat messages and applies rate-limit checks.
     * Only processes {@code CHAT_MESSAGE} packets; all other packet types are ignored.
     *
     * <p>Sliding window cleanup runs on each message: timestamps older than 60s (minute window)
     * or {@value #CHAT_BURST_WINDOW_MS}ms (burst window) are evicted from the front of their deque.
     *
     * @param player the player who sent the chat message
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CHAT_MESSAGE) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.chatTimestamps.addLast(now);
        state.chatBurstWindow.addLast(now);

        /* Evict timestamps older than 60 seconds from the per-minute sliding window */
        while (!state.chatTimestamps.isEmpty() && now - state.chatTimestamps.peekFirst() > 60000) {
            state.chatTimestamps.removeFirst();
        }
        /* Evict timestamps older than the burst window duration */
        while (!state.chatBurstWindow.isEmpty() && now - state.chatBurstWindow.peekFirst() > CHAT_BURST_WINDOW_MS) {
            state.chatBurstWindow.removeFirst();
        }

        int chatsPerMinute = state.chatTimestamps.size();
        int chatsInBurst = state.chatBurstWindow.size();

        if (chatsPerMinute > MAX_CHAT_PER_MINUTE) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (chatsInBurst > MAX_CHAT_BURST) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
    }

    /** {@inheritDoc} No outgoing packet processing needed for chat checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
