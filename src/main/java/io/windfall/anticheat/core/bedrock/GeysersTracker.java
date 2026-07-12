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
 * Scans downward 32 blocks from player position for active geyser columns
 * (POTENT_SULFUR blocks in ERUPTING or CONTINUOUS states).
 */
public class GeysersTracker {

    private static final int SCAN_RANGE_XZ = 2;
    private static final int SCAN_DEPTH = 32;
    private static final int SCAN_HEIGHT = 4;
    private static final long TICK_INTERVAL = 1L;

    private final Map<UUID, GeyserState> playerStates = new ConcurrentHashMap<>();

    private static final class GeyserState {
        volatile boolean beingPushed;
        volatile long lastCheckTick;
        volatile int geyserCount;

        GeyserState() {
            this.beingPushed = false;
            this.lastCheckTick = 0;
            this.geyserCount = 0;
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
            state = new GeyserState();
            playerStates.put(uuid, state);
        }

        long currentTick = player.getWorld().getFullTime();

        // Only re-scan every TICK_INTERVAL ticks
        if (currentTick - state.lastCheckTick < TICK_INTERVAL) {
            return state.beingPushed;
        }

        state.lastCheckTick = currentTick;
        state.beingPushed = false;
        state.geyserCount = 0;

        // Only active on 26.2+ — POTENT_SULFUR doesn't exist in older versions
        // We check by trying to find the material, which will be null on older servers
        try {
            Material potentSulfur = Material.matchMaterial("POTENT_SULFUR");
            if (potentSulfur == null) return false;
        } catch (Throwable e) {
            return false;
        }

        Location loc = player.getLocation();
        int playerX = loc.getBlockX();
        int playerY = loc.getBlockY();
        int playerZ = loc.getBlockZ();

        for (int dx = -SCAN_RANGE_XZ; dx <= SCAN_RANGE_XZ; dx++) {
            for (int dz = -SCAN_RANGE_XZ; dz <= SCAN_RANGE_XZ; dz++) {
                for (int dy = -SCAN_DEPTH; dy <= SCAN_HEIGHT; dy++) {
                    Block block = player.getWorld().getBlockAt(
                        playerX + dx,
                        playerY + dy,
                        playerZ + dz
                    );

                    if (isGeyserBlock(block)) {
                        state.geyserCount++;
                        state.beingPushed = true;
                        return true;
                    }
                }
            }
        }

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
                Object blockData = block.getState().getData();
                String blockDataStr = blockData.toString().toUpperCase();

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
