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

public final class TransactionManager {

    private final WindfallPlugin plugin;
    private final Map<UUID, TransactionState> playerTransactions = new ConcurrentHashMap<>();

    public TransactionManager(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendTransaction(WindfallPlayer player) {
        if (!player.isValid()) return;

        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());

        short id = state.nextTransactionId();
        long sendTime = System.nanoTime();

        state.pendingTransactions.add(new PendingTransaction(id, sendTime));

        // 1.17+ (protocol 756) uses Ping packets; older versions use WindowConfirmation
        try {
            if (player.getProtocolVersion() >= 756) {
                WrapperPlayServerPing ping = new WrapperPlayServerPing((int) id);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), ping);
            } else {
                WrapperPlayServerWindowConfirmation confirm = new WrapperPlayServerWindowConfirmation(
                        0, id, false);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlayer(), confirm);
            }
        } catch (Exception ignored) {}
    }

    public void processTransaction(WindfallPlayer player, short id) {
        TransactionState state = playerTransactions.get(player.getUuid());
        if (state == null) return;

        long receiveTime = System.nanoTime();
        PendingTransaction matched = null;

        // Linear scan is fine — pending queue never exceeds ~5 entries per player
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
            // NanoTime / 1_000_000 gives millisecond-precision ping
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

    public void addCallback(WindfallPlayer player, short transactionId, Runnable callback) {
        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());
        state.callbacks.put(transactionId, callback);
    }

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
        } catch (Exception ignored) {}

        return id;
    }

    public void onPlayerQuit(UUID uuid) {
        playerTransactions.remove(uuid);
    }

    private static final class PendingTransaction {
        final short id;
        final long sendTime;

        PendingTransaction(short id, long sendTime) {
            this.id = id;
            this.sendTime = sendTime;
        }
    }

    private static final class TransactionState {
        final Queue<PendingTransaction> pendingTransactions = new ConcurrentLinkedQueue<>();
        final Map<Short, Runnable> callbacks = new ConcurrentHashMap<>();
        final AtomicInteger transactionCounter = new AtomicInteger(0);

        // Mask to 15 bits — transaction IDs are shorts, sign bit must stay clear
        short nextTransactionId() {
            return (short) (transactionCounter.incrementAndGet() & 0x7FFF);
        }
    }
}
