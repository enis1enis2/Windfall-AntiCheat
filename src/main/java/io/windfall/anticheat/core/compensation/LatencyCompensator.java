package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Defers world state changes based on player latency.
 *
 * <p>When a block is broken or placed, the change is queued with a timestamp.
 * For each player, only changes older than their one-way latency are applied
 * to their CompensatedWorld. This prevents false positives when:
 * <ul>
 *   <li>A block is broken under a falling player — they don't see it immediately</li>
 *   <li>A piston pushes blocks — the client processes the update after network delay</li>
 *   <li>A potion effect expires — the client applies it after receiving the update</li>
 * </ul>
 *
 * <p>Thread safety: all maps and queues use ConcurrentHashMap/ConcurrentLinkedQueue.
 * Block changes are processed on the main thread (tick), which is the same thread
 * that reads them for check processing.
 *
 * @see PingPongManager for precise client state confirmation
 * @see CompensatedWorld for the per-player world cache that receives deferred changes
 */
public final class LatencyCompensator {

    /** Maximum time (ms) to keep a deferred change before force-applying */
    private static final long MAX_DEFER_MS = 2000;

    /** Per-player deferred block changes, ordered by timestamp */
    private final Map<UUID, Queue<BlockChange>> deferredChanges = new ConcurrentHashMap<>();

    /** Per-player CompensatedWorld instances */
    private final Map<UUID, CompensatedWorld> worldMap = new ConcurrentHashMap<>();

    /** Per-player one-way latency estimate in milliseconds */
    private final Map<UUID, Integer> latencyCache = new ConcurrentHashMap<>();

    /**
     * Records a block change for latency-compensated application.
     * The change will be applied to the player's CompensatedWorld once their
     * latency window has passed.
     *
     * @param uuid   the player UUID
     * @param x      block X coordinate
     * @param y      block Y coordinate
     * @param z      block Z coordinate
     * @param material the new block material
     * @param world  the world the change occurred in
     */
    public void onBlockChange(UUID uuid, int x, int y, int z, Material material, World world) {
        long timestamp = System.currentTimeMillis();
        BlockChange change = new BlockChange(x, y, z, material, timestamp);

        Queue<BlockChange> queue = deferredChanges.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>());
        queue.add(change);

        // Prevent unbounded queue growth from stuck players
        while (queue.size() > 1000) {
            queue.poll();
        }
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

    public void onPlayerQuit(UUID uuid) {
        deferredChanges.remove(uuid);
        worldMap.remove(uuid);
        latencyCache.remove(uuid);
    }

    /**
     * Represents a single deferred block change.
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
