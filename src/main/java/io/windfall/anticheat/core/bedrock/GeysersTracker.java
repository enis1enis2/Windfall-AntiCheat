package io.windfall.anticheat.core.bedrock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Minecraft 26.2+ geyser (volcano) eruptions near Bedrock players.
 * When a player is being pushed by a geyser column, movement checks
 * should relax their thresholds to avoid false positives.
 *
 * <p>Scans downward {@value #SCAN_DEPTH} blocks and upward {@value #SCAN_HEIGHT} blocks
 * from player position for active geyser columns (POTENT_SULFUR blocks in ERUPTING or
 * CONTINUOUS states). Scanning is limited to {@value #SCAN_RANGE_XZ} blocks horizontally.
 *
 * <p>Scan results are cached per-player and re-evaluated every {@value #TICK_INTERVAL} ticks
 * to avoid excessive block lookups. The {@link #getToleranceMultiplier} method returns a
 * 1.5x multiplier when the player is near geysers, which movement checks use to relax
 * their thresholds.
 *
 * <p>This tracker is version-gated: POTENT_SULFUR does not exist in Minecraft versions
 * before 26.2, so the check returns false immediately on older servers.
 *
 * @see BedrockInfo for player device information
 * @see GeyserManager for Geyser/Floodgate Bedrock detection
 */
public class GeysersTracker {

    /** Horizontal scan radius (blocks) around the player in X and Z directions */
    private static final int SCAN_RANGE_XZ = 2;
    /** How far below the player to scan for geyser source blocks */
    private static final int SCAN_DEPTH = 32;
    /** How far above the player to scan for active geyser columns */
    private static final int SCAN_HEIGHT = 4;
    /** Minimum ticks between re-scans to avoid excessive block lookups */
    private static final long TICK_INTERVAL = 1L;

    /** Thread-safe map of player UUID to their geyser proximity state */
    private final Map<UUID, GeyserState> playerStates = new ConcurrentHashMap<>();

    /**
     * Per-player cached geyser detection state to avoid re-scanning every packet.
     * Immutable — new instance created on each update, published atomically via ConcurrentHashMap.put().
     */
    private static final class GeyserState {
        /** Whether the player is currently within range of an active geyser */
        final boolean beingPushed;
        /** Last tick when the scan was performed (to enforce tick interval) */
        final long lastCheckTick;
        /** Number of active geyser blocks found near the player */
        final int geyserCount;

        GeyserState(boolean beingPushed, long lastCheckTick, int geyserCount) {
            this.beingPushed = beingPushed;
            this.lastCheckTick = lastCheckTick;
            this.geyserCount = geyserCount;
        }
    }

    /**
     * Check if a player is currently being pushed by a geyser eruption.
     * Returns true if any active geyser block is found within scan range.
     */
    public boolean isBeingPushed(Player player) {
        if (player == null) return false;

        UUID uuid = player.getUniqueId();
        GeyserState state = playerStates.get(uuid);

        if (state == null) {
            state = new GeyserState(false, 0, 0);
            playerStates.put(uuid, state);
        }

        long currentTick = player.getWorld().getFullTime();

        // Only re-scan every TICK_INTERVAL ticks
        if (currentTick - state.lastCheckTick < TICK_INTERVAL) {
            return state.beingPushed;
        }

        // Only active on 26.2+ — POTENT_SULFUR doesn't exist in older versions
        // We check by trying to find the material, which will be null on older servers
        try {
            Material potentSulfur = Material.matchMaterial("POTENT_SULFUR");
            if (potentSulfur == null) {
                playerStates.put(uuid, new GeyserState(false, currentTick, 0));
                return false;
            }
        } catch (Throwable e) {
            playerStates.put(uuid, new GeyserState(false, currentTick, 0));
            return false;
        }

        Location loc = player.getLocation();
        int playerX = loc.getBlockX();
        int playerY = loc.getBlockY();
        int playerZ = loc.getBlockZ();

        int geyserCount = 0;
        boolean beingPushed = false;

        for (int dx = -SCAN_RANGE_XZ; dx <= SCAN_RANGE_XZ; dx++) {
            for (int dz = -SCAN_RANGE_XZ; dz <= SCAN_RANGE_XZ; dz++) {
                for (int dy = -SCAN_DEPTH; dy <= SCAN_HEIGHT; dy++) {
                    Block block = player.getWorld().getBlockAt(
                        playerX + dx,
                        playerY + dy,
                        playerZ + dz
                    );

                    if (isGeyserBlock(block)) {
                        geyserCount++;
                        beingPushed = true;
                        playerStates.put(uuid, new GeyserState(true, currentTick, geyserCount));
                        return true;
                    }
                }
            }
        }

        playerStates.put(uuid, new GeyserState(false, currentTick, 0));
        return false;
    }

    /**
     * Check if a block is an active geyser eruption block.
     * POTENT_SULFUR blocks in ERUPTING or CONTINUOUS states.
     */
    private boolean isGeyserBlock(Block block) {
        if (block == null || block.getType() == Material.AIR) return false;

        Material material = block.getType();
        String name = material.name();

        // POTENT_SULFUR is the geyser block in 26.2+
        // Check for eruption states
        if (name.equals("POTENT_SULFUR")) {
            try {
                // Use reflection for BlockData API (1.13+) — not available in spigot-api 1.8
                java.lang.reflect.Method getBlockData = block.getClass().getMethod("getBlockData");
                Object blockData = getBlockData.invoke(block);
                java.lang.reflect.Method getAsString = blockData.getClass().getMethod("getAsString");
                String blockDataStr = ((String) getAsString.invoke(blockData)).toUpperCase();
                if (blockDataStr.contains("ERUPTING") || blockDataStr.contains("CONTINUOUS")) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Fallback: assume potential geyser if block exists
            }
        }

        return false;
    }

    /**
     * Remove player tracking data on disconnect.
     */
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }

    /**
     * Get the number of active geyser blocks near a player.
     */
    public int getGeyserCount(Player player) {
        GeyserState state = playerStates.get(player.getUniqueId());
        return state != null ? state.geyserCount : 0;
    }

    /**
     * Get tolerance multiplier when player is near geysers.
     * Returns 1.0 if not near any geyser, otherwise 1.5× to account
     * for the knockback-like push effect.
     */
    public double getToleranceMultiplier(Player player) {
        return isBeingPushed(player) ? 1.5 : 1.0;
    }
}
