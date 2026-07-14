package io.windfall.anticheat.core.compensation;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.Queue;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages server-to-client transaction packets for ping measurement and action compensation.
 *
 * <p>Transactions are used to measure round-trip time (RTT) between the server and client.
 * The server sends a transaction packet with a unique ID and records the send time. When the
 * client echoes it back, the RTT is calculated and stored on the player for compensation.
 *
 * <p>Protocol handling:
 * <ul>
 *   <li><b>1.17+ (protocol 756+)</b> &mdash; uses {@code Ping} packets (WrapperPlayServerPing)</li>
 *   <li><b>Pre-1.17</b> &mdash; uses {@code WindowConfirmation} packets with window ID 0</li>
 * </ul>
 *
 * <p>Features:
 * <ul>
 *   <li><b>Ping measurement</b> &mdash; stores millisecond-precision ping on {@link WindfallPlayer}</li>
 *   <li><b>Callbacks</b> &mdash; attach callbacks to specific transaction IDs for action compensation
 *       (e.g., execute code after the client has confirmed receipt)</li>
 *   <li><b>Thread-safe</b> &mdash; uses ConcurrentHashMap and ConcurrentLinkedQueue for safe
 *       multi-threaded access from the packet events library</li>
 * </ul>
 *
 * <p>Transaction IDs are masked to 15 bits (0x7FFF) to keep them positive within the short range,
 * as the sign bit must stay clear for Minecraft protocol compatibility.
 *
 * @see WindfallPlayer#setTransactionPing(int) for where computed ping is stored
 */
public final class TransactionManager {

    /** The Windfall plugin instance for accessing player data and logging */
    private final WindfallPlugin plugin;
    /** Thread-safe map of player UUID to their transaction tracking state */
    private final Map<UUID, TransactionState> playerTransactions = new ConcurrentHashMap<>();

    /**
     * Creates a new TransactionManager.
     *
     * @param plugin the Windfall plugin instance
     */
    public TransactionManager(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a transaction packet to the player and records the send time.
     * Uses Ping packets for 1.17+ clients, WindowConfirmation for older versions.
     *
     * @param player the player to send the transaction to
     */
    public void sendTransaction(WindfallPlayer player) {
        if (!player.isValid()) return;

        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());

        short id = state.nextTransactionId();
        long sendTime = System.nanoTime();

        state.pendingTransactions.add(new PendingTransaction(id, sendTime));

        /* 1.17+ (protocol 756) uses Ping packets; older versions use WindowConfirmation.
         * WindowConfirmation uses window ID 0 and the transaction ID as the confirmation number. */
        try {
            if (player.getProtocolVersion() >= 756) {
                WrapperPlayServerPing ping = new WrapperPlayServerPing((int) id);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), ping);
            } else {
                WrapperPlayServerWindowConfirmation confirm = new WrapperPlayServerWindowConfirmation(
                        0, id, false);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), confirm);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Windfall: Failed to send transaction packet: " + e.getMessage());
        }
    }

    /**
     * Processes an incoming transaction response from the client.
     * Matches the response ID against pending transactions, computes RTT, and
     * executes any registered callbacks.
     *
     * @param player the player who responded
     * @param id     the transaction ID echoed by the client
     */
    public void processTransaction(WindfallPlayer player, short id) {
        TransactionState state = playerTransactions.get(player.getUuid());
        if (state == null) return;

        long receiveTime = System.nanoTime();
        PendingTransaction matched = null;

        /* Linear scan is fine — pending queue never exceeds ~5 entries per player.
         * A matched transaction is removed; unmatched ones are returned to the queue. */
        Queue<PendingTransaction> remaining = new ConcurrentLinkedQueue<>();
        while (!state.pendingTransactions.isEmpty()) {
            PendingTransaction tx = state.pendingTransactions.poll();
            if (tx.id == id) {
                matched = tx;
            } else {
                remaining.add(tx);
            }
        }
        state.pendingTransactions.addAll(remaining);

        if (matched != null) {
            /* Convert nanosecond-precision RTT to milliseconds for the player's ping value.
             * NanoTime / 1_000_000 gives millisecond-precision ping */
            long pingNanos = receiveTime - matched.sendTime;
            player.setTransactionPing((int) (pingNanos / 1_000_000));
        }

        Runnable callback = state.callbacks.remove(id);
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers a callback to be executed when a specific transaction ID is confirmed by the client.
     *
     * @param player        the player to register the callback for
     * @param transactionId the transaction ID to wait for
     * @param callback      the action to execute on confirmation
     */
    public void addCallback(WindfallPlayer player, short transactionId, Runnable callback) {
        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());
        state.callbacks.put(transactionId, callback);
    }

    /**
     * Sends a transaction and registers a callback for when the client confirms it.
     * Combines {@link #sendTransaction} and {@link #addCallback} into a single atomic operation.
     *
     * @param player   the player to send the transaction to
     * @param callback the action to execute when the client confirms receipt
     * @return the transaction ID that was sent
     */
    public short sendTransactionWithCallback(WindfallPlayer player, Runnable callback) {
        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());

        short id = state.nextTransactionId();
        long sendTime = System.nanoTime();

        state.pendingTransactions.add(new PendingTransaction(id, sendTime));
        state.callbacks.put(id, callback);

        try {
            if (player.getProtocolVersion() >= 756) {
                WrapperPlayServerPing ping = new WrapperPlayServerPing((int) id);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), ping);
            } else {
                WrapperPlayServerWindowConfirmation confirm = new WrapperPlayServerWindowConfirmation(
                        0, id, false);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), confirm);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Windfall: Failed to send transaction packet: " + e.getMessage());
        }

        return id;
    }

    /**
     * Cleans up transaction state when a player disconnects.
     * Prevents memory leaks from orphaned pending transactions and callbacks.
     *
     * @param uuid the UUID of the player who disconnected
     */
    public void onPlayerQuit(UUID uuid) {
        playerTransactions.remove(uuid);
    }

    /**
     * Represents a pending transaction awaiting client confirmation.
     * Stores the transaction ID and the send timestamp (in nanoseconds) for RTT calculation.
     */
    private static final class PendingTransaction {
        final short id;
        final long sendTime;

        PendingTransaction(short id, long sendTime) {
            this.id = id;
            this.sendTime = sendTime;
        }
    }

    /**
     * Per-player transaction tracking state.
     * Thread-safe via ConcurrentHashMap and ConcurrentLinkedQueue.
     */
    private static final class TransactionState {
        final Queue<PendingTransaction> pendingTransactions = new ConcurrentLinkedQueue<>();
        final Map<Short, Runnable> callbacks = new ConcurrentHashMap<>();
        final AtomicInteger transactionCounter = new AtomicInteger(0);

        /**
     * Generates the next transaction ID, masked to 15 bits to keep it positive.
     * Transaction IDs must be non-negative shorts in the Minecraft protocol.
     *
     * @return the next transaction ID in range [0, 32767]
     */
    // Mask to 15 bits — transaction IDs are shorts, sign bit must stay clear
    short nextTransactionId() {
            return (short) (transactionCounter.incrementAndGet() & 0x7FFF);
        }
    }
}
