package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Dual-ping sandwich system for precise client state tracking.
 *
 * <p>Sends two transaction packets per tick — one BEFORE state changes and one AFTER.
 * When the client responds to each ping, we know exactly which state changes it has seen.
 *
 * <p>This eliminates the "blink" bypass: cheats that send movement packets before
 * processing a state change (e.g., potion effect, block break) that the server applied.
 * With dual pings, we can distinguish whether the client had processed a specific
 * state change when it sent a movement packet.
 *
 * <p>Usage flow per tick:
 * <ol>
 *   <li>{@link #onTickStart(WindfallPlayer)} — sends first ping (client acknowledges pre-change state)</li>
 *   <li>Server processes state changes (block breaks, potion applications, etc.)</li>
 *   <li>{@link #onTickEnd(WindfallPlayer)} — sends second ping (client acknowledges post-change state)</li>
 *   <li>Client responds to both pings — we now know exactly which ticks' changes it saw</li>
 * </ol>
 *
 * @see LatencyCompensator for deferred world change application
 * @see SimulationEngine for multi-scenario prediction using confirmed state
 */
public final class PingPongManager {

    private final WindfallPlugin plugin;
    private final Map<UUID, PlayerPingState> playerStates = new ConcurrentHashMap<>();

    public PingPongManager(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends the first ping of the tick (pre-change state marker).
     * The client's response tells us it has seen all state up to this point.
     */
    public void onTickStart(WindfallPlayer player) {
        if (!player.isValid()) return;
        PlayerPingState state = getState(player);
        state.sendPing(plugin, player, true);
    }

    /**
     * Sends the second ping of the tick (post-change state marker).
     * The client's response tells us it has seen all state changes made this tick.
     */
    public void onTickEnd(WindfallPlayer player) {
        if (!player.isValid()) return;
        PlayerPingState state = getState(player);
        state.currentTick++;
        state.sendPing(plugin, player, false);
    }

    /**
     * Processes a client response to a transaction ping.
     * Updates the confirmed tick window and fires any pending callbacks.
     *
     * <p>RTT is derived from the send time recorded in the {@link PingRecord} rather than a
     * caller-supplied value — the packet listener has no way to know when the ping went out,
     * and guessing would make latency tracking meaningless.
     *
     * @param player the player who responded
     * @param id     the transaction ID echoed by the client
     * @return true if this was a ping-pong transaction (not a regular transaction)
     */
    public boolean processPingResponse(WindfallPlayer player, short id) {
        PlayerPingState state = playerStates.get(player.getUuid());
        if (state == null) return false;

        PingRecord record = state.pendingPings.remove(id);
        if (record == null) return false;

        long rttNanos = System.nanoTime() - record.sendTimeNanos;
        int rttMs = (int) (rttNanos / 1_000_000);

        if (record.isStartPing) {
            // Pre-change ping confirmed: client has seen state up to previous tick
            state.confirmedTick = Math.max(state.confirmedTick, record.tick - 1);
            state.lastPreChangeRtt = rttMs;
        } else {
            // Post-change ping confirmed: client has seen this tick's changes
            state.confirmedTick = Math.max(state.confirmedTick, record.tick);
            state.lastPostChangeRtt = rttMs;
        }

        // Publish the one-way latency estimate that lag compensation consumes.
        LatencyCompensator compensator = plugin.getLatencyCompensator();
        if (compensator != null) {
            compensator.updateLatency(player.getUuid(), getEstimatedLatencyMs(player));
        }

        fireConfirmedCallbacks(state);
        return true;
    }

    /**
     * Registers a callback to run once a specific tick is confirmed by the client.
     * Used by checks that need to defer logic until the client has processed a state change.
     *
     * <p>Callbacks are stored per tick and fired only once that exact tick is confirmed.
     * Firing every pending callback on any confirmation would run tick-5 logic as soon as
     * tick 1 was acknowledged.
     */
    public void onTickConfirmed(WindfallPlayer player, int tick, Runnable callback) {
        PlayerPingState state = getState(player);
        if (tick <= state.confirmedTick) {
            runCallback(callback);
            return;
        }
        state.tickCallbacks
                .computeIfAbsent(tick, k -> new ConcurrentLinkedQueue<>())
                .add(callback);
    }

    private void runCallback(Runnable callback) {
        try {
            callback.run();
        } catch (Exception e) {
            plugin.getLogger().warning("Windfall: ping-pong callback failed: " + e.getMessage());
        }
    }

    /**
     * Runs every callback registered for a tick the client has now confirmed.
     *
     * <p>Collects due keys first and removes them before invoking: {@code ConcurrentHashMap}
     * iteration order is unspecified, so breaking out on the first unconfirmed key could leave
     * older callbacks stranded.
     */
    private void fireConfirmedCallbacks(PlayerPingState state) {
        int confirmed = state.confirmedTick;
        for (Integer tick : new ArrayList<>(state.tickCallbacks.keySet())) {
            if (tick == null || tick > confirmed) continue;
            Queue<Runnable> callbacks = state.tickCallbacks.remove(tick);
            if (callbacks == null) continue;
            for (Runnable callback : callbacks) {
                runCallback(callback);
            }
        }
    }

    /**
     * Returns the latest tick number that the client has fully confirmed seeing.
     * All state changes up to this tick have been processed by the client.
     */
    public int getConfirmedTick(WindfallPlayer player) {
        PlayerPingState state = playerStates.get(player.getUuid());
        return state != null ? state.confirmedTick : 0;
    }

    /**
     * Returns true if the client has confirmed seeing all state changes up to the given tick.
     */
    public boolean isTickConfirmed(WindfallPlayer player, int tick) {
        return getConfirmedTick(player) >= tick;
    }

    /**
     * Returns the estimated one-way latency in milliseconds.
     * Used by LatencyCompensator to determine which world changes the client has seen.
     */
    public int getEstimatedLatencyMs(WindfallPlayer player) {
        PlayerPingState state = playerStates.get(player.getUuid());
        if (state == null) return 0;
        return (state.lastPreChangeRtt + state.lastPostChangeRtt) / 4; // RTT/2 ≈ one-way
    }

    /**
     * Returns the current tick being processed on the server.
     */
    public int getCurrentTick(WindfallPlayer player) {
        PlayerPingState state = playerStates.get(player.getUuid());
        return state != null ? state.currentTick : 0;
    }

    public void onPlayerQuit(UUID uuid) {
        playerStates.remove(uuid);
    }

    private PlayerPingState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerPingState());
    }

    /**
     * Per-player ping-pong tracking state.
     * Thread-safe via ConcurrentHashMap and ConcurrentLinkedQueue.
     */
    private static final class PlayerPingState {
        /** Current server tick counter for this player */
        volatile int currentTick;
        /** Latest tick the client has confirmed seeing */
        volatile int confirmedTick;
        /** RTT measurements from pre-change and post-change pings */
        volatile int lastPreChangeRtt;
        volatile int lastPostChangeRtt;
        /** Pending pings awaiting client response (transaction ID → record) */
        final Map<Short, PingRecord> pendingPings = new ConcurrentHashMap<>();
        /** Callbacks keyed by the tick they wait on — fired once that tick is confirmed */
        final Map<Integer, Queue<Runnable>> tickCallbacks = new ConcurrentHashMap<>();

        void sendPing(WindfallPlugin plugin, WindfallPlayer player, boolean isStartPing) {
            try {
                TransactionManager txMgr = plugin.getTransactionManager();
                short id = txMgr.sendPigPongTransaction(player);
                if (id >= 0) {
                    pendingPings.put(id, new PingRecord(currentTick, isStartPing, System.nanoTime()));
                }
            } catch (Exception e) {
                // Ping failure must not crash the tick
            }
        }
    }

    /**
     * Record of a single ping transaction awaiting client response.
     */
    private static final class PingRecord {
        final int tick;
        final boolean isStartPing;
        final long sendTimeNanos;

        PingRecord(int tick, boolean isStartPing, long sendTimeNanos) {
            this.tick = tick;
            this.isStartPing = isStartPing;
            this.sendTimeNanos = sendTimeNanos;
        }
    }
}
