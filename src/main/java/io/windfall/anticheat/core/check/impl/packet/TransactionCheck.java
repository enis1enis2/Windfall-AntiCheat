package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.WindfallPlugin;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects clients that skip, fabricate, or reorder transaction responses.
 *
 * <p>Transaction packets (Ping on 1.17+, WindowConfirmation on older versions) are
 * the server's primary mechanism for measuring client latency. A cheating client may:
 * <ul>
 *   <li><b>Skip transactions</b> &mdash; ignore server packets to reduce overhead</li>
 *   <li><b>Fabricate responses</b> &mdash; send fake transaction IDs to manipulate ping</li>
 *   <li><b>Reorder responses</b> &mdash; respond out-of-order, causing incorrect RTT</li>
 * </ul>
 *
 * <p><b>Detection algorithm:</b>
 * <ol>
 *   <li>Count skipped transactions (sent but never responded to) per player</li>
 *   <li>Count unknown responses (IDs not matching any pending transaction)</li>
 *   <li>Flag when skipped + unknown exceeds threshold within the window</li>
 * </ol>
 *
 * <p><b>Thresholds:</b>
 * <ul>
 *   <li>{@value #SKIP_THRESHOLD} skipped transactions per window &mdash; indicates client ignoring pings</li>
 *   <li>{@value #UNKNOWN_THRESHOLD} unknown responses per window &mdash; indicates fabricated IDs</li>
 * </ul>
 *
 * <p>Protocol-aware: detects both {@code Pong} (1.17+) and {@code WindowConfirmation} (pre-1.17)
 * response packets. Setback at VL 15, decay 0.005/tick.
 *
 * @see TransactionManager for the underlying transaction tracking
 * @see PingPongManager for dual-ping sandwich system
 */
@CheckData(
    name = "Transaction A",
    stableKey = "windfall.packet.transaction",
    decay = 0.005,
    setbackVl = 15,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.3
)
public class TransactionCheck extends Check implements PacketCheck {

    /** Number of skipped transactions within the window before flagging */
    private static final int SKIP_THRESHOLD = 10;

    /** Number of unknown (fabricated) responses within the window before flagging */
    private static final int UNKNOWN_THRESHOLD = 5;

    /** Window duration in milliseconds for accumulating skip/unknown counts */
    private static final long WINDOW_MS = 5000;

    /**
     * Per-player state for tracking transaction health metrics.
     */
    private static final class PlayerState {
        /** Number of skipped transactions in the current window */
        int skippedInWindow;
        /** Number of unknown responses in the current window */
        int unknownInWindow;
        /** Start timestamp of the current window */
        long windowStart;
        /** Last known pending count from TransactionManager */
        int lastPendingCount;
    }

    /** Thread-safe map of player UUID to their transaction check state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        // Detect transaction response packets (Pong on 1.17+, WindowConfirmation on older)
        if (type == PacketType.Play.Client.PONG) {
            handleTransactionResponse(player, event);
        } else if (type == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            handleTransactionResponse(player, event);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Handles an incoming transaction response packet.
     * The response ID is extracted and compared against pending transactions
     * via TransactionManager. If the ID doesn't match, it's counted as unknown.
     *
     * @param player the player who sent the response
     * @param event  the packet event
     */
    private void handleTransactionResponse(WindfallPlayer player, PacketReceiveEvent event) {
        WindfallPlugin plugin = WindfallPlugin.getInstance();
        if (plugin == null) return;

        TransactionManager txManager = plugin.getTransactionManager();
        if (txManager == null) return;

        short responseId;
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
            responseId = (short) pong.getId();
        } else {
            WrapperPlayClientWindowConfirmation confirm = new WrapperPlayClientWindowConfirmation(event);
            responseId = confirm.getActionId();
        }

        // Check if this response matched a pending transaction
        int prevUnknown = txManager.getUnknownResponses();
        txManager.processTransaction(player, responseId);
        int newUnknown = txManager.getUnknownResponses();

        // If unknown count increased, this was a fabricated response
        if (newUnknown > prevUnknown) {
            PlayerState state = getState(player);
            resetWindowIfNeeded(state);
            state.unknownInWindow++;
            evaluatePlayer(player, state);
        }
    }

    /**
     * Called externally by the tick loop to check for skipped transactions.
     * Compares current pending count against the last known value to detect
     * transactions that were sent but never responded to.
     *
     * @param player the player to check
     */
    public void onTick(WindfallPlayer player) {
        WindfallPlugin plugin = WindfallPlugin.getInstance();
        if (plugin == null) return;

        TransactionManager txManager = plugin.getTransactionManager();
        if (txManager == null) return;

        PlayerState state = getState(player);
        resetWindowIfNeeded(state);

        int currentPending = txManager.getPendingCount(player.getUuid());

        // If pending count decreased, transactions were responded to (good)
        // If pending count stayed the same or increased across multiple ticks, they may be skipped
        if (state.lastPendingCount > 0 && currentPending >= state.lastPendingCount) {
            state.skippedInWindow++;
            txManager.incrementSkippedTransactions();
        }

        state.lastPendingCount = currentPending;
        evaluatePlayer(player, state);
    }

    /**
     * Evaluates the player's transaction health and flags if thresholds exceeded.
     *
     * @param player the player to evaluate
     * @param state  the player's current state
     */
    private void evaluatePlayer(WindfallPlayer player, PlayerState state) {
        int totalAnomalies = state.skippedInWindow + state.unknownInWindow;

        if (state.skippedInWindow >= SKIP_THRESHOLD) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
                state.skippedInWindow = 0;
                state.unknownInWindow = 0;
            }
        } else if (state.unknownInWindow >= UNKNOWN_THRESHOLD) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
                state.skippedInWindow = 0;
                state.unknownInWindow = 0;
            }
        } else if (totalAnomalies == 0) {
            decreaseBuffer(player, 0.2);
        }
    }

    /**
     * Resets the sliding window if it has expired.
     *
     * @param state the player's state
     */
    private void resetWindowIfNeeded(PlayerState state) {
        long now = System.currentTimeMillis();
        if (state.windowStart == 0 || now - state.windowStart > WINDOW_MS) {
            state.skippedInWindow = 0;
            state.unknownInWindow = 0;
            state.windowStart = now;
        }
    }
}
