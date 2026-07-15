package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Defers world state changes based on player latency.
 *
 * <p>Two parallel tracking systems:
 * <ol>
 *   <li><b>Timestamp-based</b>: Queues block changes and applies them after the player's
 *       latency window passes — used for CompensatedWorld updates.</li>
 *   <li><b>Tick-based</b>: Records typed {@link WorldChange} objects tagged with server ticks —
 *       used by {@link SimulationEngine} to replay unconfirmed changes.</li>
 * </ol>
 *
 * <p>This design prevents false positives when:
 * <ul>
 *   <li>A block is broken under a falling player — they don't see it immediately</li>
 *   <li>A piston pushes blocks — the client processes the update after network delay</li>
 *   <li>A potion effect expires — the client applies it after receiving the update</li>
 *   <li>Server sends knockback velocity — the client applies it after the update</li>
 * </ul>
 *
 * <p>Thread safety: all maps and queues use ConcurrentHashMap/ConcurrentLinkedQueue.
 * All writes happen on the main thread (tick), reads from both the main thread and
 * Netty packet threads.
 *
 * @see PingPongManager for precise client state confirmation
 * @see CompensatedWorld for the per-player world cache that receives deferred changes
 * @see SimulationEngine for multi-scenario movement prediction using unconfirmed changes
 */
public final class LatencyCompensator {

    /** Maximum time (ms) to keep a deferred change before force-applying */
    private static final long MAX_DEFER_MS = 2000;

    /** Maximum number of ticks to retain for tick-based tracking (prevents unbounded growth) */
    private static final int MAX_TICK_HISTORY = 100;

    // === TIMESTAMP-BASED TRACKING (CompensatedWorld) ===

    /** Per-player deferred block changes, ordered by timestamp */
    private final Map<UUID, Queue<BlockChange>> deferredChanges = new ConcurrentHashMap<>();

    /** Per-player CompensatedWorld instances */
    private final Map<UUID, CompensatedWorld> worldMap = new ConcurrentHashMap<>();

    /** Per-player one-way latency estimate in milliseconds */
    private final Map<UUID, Integer> latencyCache = new ConcurrentHashMap<>();

    // === TICK-BASED TRACKING (SimulationEngine) ===

    /** Per-player world changes indexed by server tick number */
    private final Map<UUID, Map<Integer, List<WorldChange>>> tickChanges = new ConcurrentHashMap<>();

    /**
     * Records a block change for both latency-compensated application and tick-based simulation.
     *
     * <p>The change is:
     * <ul>
     *   <li>Queued with a timestamp for CompensatedWorld application after latency window</li>
     *   <li>Tagged with the current tick for SimulationEngine unconfirmed-change replay</li>
     * </ul>
     *
     * @param uuid     the player UUID
     * @param x        block X coordinate
     * @param y        block Y coordinate
     * @param z        block Z coordinate
     * @param material the new block material
     * @param tick     the current server tick number
     * @param world    the world the change occurred in
     */
    public void onBlockChange(UUID uuid, int x, int y, int z, Material material, int tick, World world) {
        // Timestamp-based: for CompensatedWorld application
        long timestamp = System.currentTimeMillis();
        BlockChange change = new BlockChange(x, y, z, material, timestamp);

        Queue<BlockChange> queue = deferredChanges.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>());
        queue.add(change);

        // Prevent unbounded queue growth from stuck players
        while (queue.size() > 1000) {
            queue.poll();
        }

        // Tick-based: for SimulationEngine unconfirmed-change replay
        recordTickChange(uuid, tick, WorldChange.blockBreak(tick, x, y, z));
    }

    /**
     * Records a block change with tick tracking (overload for callers without a tick parameter).
     * Uses tick 0 as a sentinel — tick-based simulation will skip changes with tick 0.
     */
    public void onBlockChange(UUID uuid, int x, int y, int z, Material material, World world) {
        onBlockChange(uuid, x, y, z, material, 0, world);
    }

    /**
     * Records a typed world change for tick-based simulation.
     * Does NOT affect the timestamp-based CompensatedWorld queue.
     * Used for velocity, potion effects, and piston shifts that don't need
     * latency-deferred CompensatedWorld application.
     *
     * @param uuid   the player UUID
     * @param tick   the current server tick number
     * @param change the world change to record
     */
    public void recordChange(UUID uuid, int tick, WorldChange change) {
        recordTickChange(uuid, tick, change);
    }

    /**
     * Returns all world changes between confirmedTick+1 and currentTick (exclusive).
     * These are changes the server has applied but the client may not have seen yet.
     * Used by {@link SimulationEngine} to build multi-scenario simulations.
     *
     * @param uuid          the player UUID
     * @param confirmedTick the latest tick the client has confirmed seeing
     * @param currentTick   the current server tick
     * @return list of unconfirmed changes (may be empty)
     */
    public List<WorldChange> getUnconfirmedChanges(UUID uuid, int confirmedTick, int currentTick) {
        Map<Integer, List<WorldChange>> playerTicks = tickChanges.get(uuid);
        if (playerTicks == null || playerTicks.isEmpty()) return java.util.Collections.emptyList();

        List<WorldChange> result = new ArrayList<>();
        for (int t = confirmedTick + 1; t < currentTick; t++) {
            List<WorldChange> changes = playerTicks.get(t);
            if (changes != null) {
                result.addAll(changes);
            }
        }
        return result;
    }

    /**
     * Updates the latency estimate for a player.
     * Called by PingPongManager when a ping response is received.
     *
     * @param uuid the player UUID
     * @param latencyMs one-way latency in milliseconds
     */
    public void updateLatency(UUID uuid, int latencyMs) {
        latencyCache.put(uuid, latencyMs);
    }

    /**
     * Processes deferred block changes for a player.
     * Applies all changes older than the player's latency to their CompensatedWorld.
     * Must be called on the main thread during tick processing.
     *
     * @param uuid   the player UUID
     * @param player the WindfallPlayer (for latency lookup)
     */
    public void processDeferredChanges(UUID uuid, WindfallPlayer player) {
        Queue<BlockChange> queue = deferredChanges.get(uuid);
        if (queue == null || queue.isEmpty()) return;

        int latencyMs = latencyCache.getOrDefault(uuid, player.getTransactionPing());
        long cutoffTime = System.currentTimeMillis() - latencyMs;

        CompensatedWorld world = worldMap.get(uuid);
        if (world == null) return;

        Queue<BlockChange> remaining = new ConcurrentLinkedQueue<>();
        while (!queue.isEmpty()) {
            BlockChange change = queue.poll();
            if (change.timestamp <= cutoffTime || System.currentTimeMillis() - change.timestamp > MAX_DEFER_MS) {
                // Latency window passed or change is too old — apply it
                world.onBlockChange(change.x, change.y, change.z, change.material);
            } else {
                remaining.add(change);
            }
        }
        queue.addAll(remaining);
    }

    /**
     * Checks if a block at the given position has been broken (air)
     * in the player's compensated world.
     * Used by movement checks to verify ground state.
     */
    public boolean isBlockBroken(UUID uuid, int x, int y, int z) {
        CompensatedWorld world = worldMap.get(uuid);
        if (world == null) return false;
        Material mat = world.getBlock(x, y, z);
        return mat == null || mat == Material.AIR;
    }

    /**
     * Gets or creates the CompensatedWorld for a player.
     */
    public CompensatedWorld getWorld(UUID uuid, World bukkitWorld) {
        return worldMap.computeIfAbsent(uuid, k -> new CompensatedWorld(bukkitWorld));
    }

    /**
     * Returns the number of pending deferred changes for a player.
     * Used for diagnostics and memory monitoring.
     */
    public int getPendingChangeCount(UUID uuid) {
        Queue<BlockChange> queue = deferredChanges.get(uuid);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Prunes tick-based tracking data older than {@value MAX_TICK_HISTORY} ticks.
     * Called periodically to prevent unbounded memory growth.
     *
     * @param currentTick the current server tick
     */
    public void pruneTickHistory(int currentTick) {
        int cutoff = currentTick - MAX_TICK_HISTORY;
        for (Map<Integer, List<WorldChange>> playerTicks : tickChanges.values()) {
            playerTicks.keySet().removeIf(t -> t < cutoff);
        }
    }

    public void onPlayerQuit(UUID uuid) {
        deferredChanges.remove(uuid);
        worldMap.remove(uuid);
        latencyCache.remove(uuid);
        tickChanges.remove(uuid);
    }

    // === INTERNAL ===

    private void recordTickChange(UUID uuid, int tick, WorldChange change) {
        if (tick <= 0) return; // Sentinel tick — skip
        Map<Integer, List<WorldChange>> playerTicks =
            tickChanges.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        List<WorldChange> changes = playerTicks.computeIfAbsent(tick, k -> new ArrayList<>());
        changes.add(change);
    }

    /**
     * Represents a single deferred block change (timestamp-based).
     */
    private static final class BlockChange {
        final int x, y, z;
        final Material material;
        long timestamp;

        BlockChange(int x, int y, int z, Material material, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
            this.timestamp = timestamp;
        }
    }
}
