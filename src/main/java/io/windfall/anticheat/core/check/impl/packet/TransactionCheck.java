package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
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

    /** Floor for the response deadline, in ms — covers a local/LAN client on a laggy server */
    private static final long MIN_SKIP_TIMEOUT_MS = 2000L;

    /** Fixed slack added on top of 3× measured ping, in ms */
    private static final long SKIP_TIMEOUT_BASE_MS = 1000L;

    /**
     * Per-player state for tracking transaction health metrics.
     *
     * <p>Fields are volatile because this state is written from the packet thread
     * ({@link #handleTransactionResponse}) and read/mutated from the tick thread
     * ({@link #onTick}).
     */
    private static final class PlayerState {
        /** Number of skipped transactions in the current window */
        volatile int skippedInWindow;
        /** Number of unknown responses in the current window */
        volatile int unknownInWindow;
        /** Start timestamp of the current window */
        volatile long windowStart;
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
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Records the outcome of a client transaction response.
     *
     * <p>Called by {@link io.windfall.anticheat.core.check.CheckManager} once
     * {@link TransactionManager} has matched the echoed ID, so a fabricated or reordered
     * response is attributed to the player who actually sent it.
     *
     * @param player  the responding player
     * @param matched false if the echoed ID matched no pending transaction
     */
    public void onTransactionResponse(WindfallPlayer player, boolean matched) {
        if (matched) return;

        PlayerState state = getState(player);
        resetWindowIfNeeded(state);
        state.unknownInWindow++;
        evaluatePlayer(player, state);
    }

    /**
     * Called by the tick loop to detect skipped transactions.
     * Sweeps the player's pending queue and counts any transaction that has outlived its
     * response deadline.
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

        /*
         * A transaction counts as skipped only once it is past its own response deadline.
         * Sweeping the pending queue by send time is exact; comparing the pending count between
         * ticks would flag every healthy client, because a response still in flight legitimately
         * keeps the count flat or rising on any given tick.
         */
        state.skippedInWindow += txManager.sweepTimedOutTransactions(
                player.getUuid(), getSkipTimeoutMs(player));
        evaluatePlayer(player, state);
    }

    /**
     * Response deadline for this player's transactions.
     *
     * <p>Generous enough to absorb ordinary latency, a lag spike, or a GC pause without flagging,
     * but short enough that a client genuinely ignoring transactions is caught. Scaled off the
     * player's measured ping so high-latency connections are not punished for being slow, and
     * widened for Bedrock controllers whose input thread can stall.
     */
    private long getSkipTimeoutMs(WindfallPlayer player) {
        int ping = player.getTransactionPing();
        long base = Math.max(MIN_SKIP_TIMEOUT_MS, (long) (ping * 3) + SKIP_TIMEOUT_BASE_MS);
        return player.isBedrock() ? base * 2 : base;
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
