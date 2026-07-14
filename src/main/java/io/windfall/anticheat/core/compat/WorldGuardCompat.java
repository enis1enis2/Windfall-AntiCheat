package io.windfall.anticheat.core.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * WorldGuard integration for region-based exemptions.
 *
 * <p>Uses reflection to avoid compile-time dependencies on WorldEdit/WorldGuard APIs
 * which may not be available at compile time (spigot-api 1.8) and may change across
 * WorldGuard versions.
 *
 * <p>When WorldGuard is installed, Windfall can check whether players are inside
 * WorldGuard regions. This allows server owners to create safe zones where
 * anti-cheat checks are relaxed.
 *
 * @see org.bukkit.Bukkit#getPluginManager() for plugin detection
 */
public final class WorldGuardCompat {

    private final Object worldGuardPlugin;
    private boolean available = true;

    private WorldGuardCompat(Object worldGuardPlugin) {
        this.worldGuardPlugin = worldGuardPlugin;
    }

    /**
     * Attempts to load WorldGuard integration via reflection.
     *
     * @return WorldGuardCompat instance, or null if WorldGuard is not installed
     */
    public static WorldGuardCompat load() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (plugin == null || !plugin.isEnabled()) return null;

            // Verify WorldGuard classes are available
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return new WorldGuardCompat(plugin);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if a player is in any WorldGuard region at their current location.
     *
     * @param player the player to check
     * @return true if the player is in at least one WorldGuard region
     */
    public boolean isInRegion(Player player) {
        if (!available || worldGuardPlugin == null) return false;
        try {
            Location loc = player.getLocation();
            return queryRegions(player, loc);
        } catch (Exception e) {
            available = false;
            return false;
        }
    }

    /**
     * Checks if a specific location is in any WorldGuard region.
     *
     * @param x world X coordinate
     * @param y world Y coordinate
     * @param z world Z coordinate
     * @return true if the location is in at least one WorldGuard region
     */
    public boolean isInRegion(int x, int y, int z) {
        if (!available || worldGuardPlugin == null) return false;
        try {
            org.bukkit.World world = Bukkit.getWorlds().get(0);
            if (world == null) return false;
            Location loc = new Location(world, x, y, z);
            return queryRegions(null, loc);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a player is in any WorldGuard region at the specified location.
     *
     * @param player   the player
     * @param location the location to check
     * @return true if the location is in at least one WorldGuard region
     */
    public boolean isInRegion(Player player, Location location) {
        if (!available || worldGuardPlugin == null) return false;
        try {
            return queryRegions(player, location);
        } catch (Exception e) {
            available = false;
            return false;
        }
    }

    /**
     * Reflective query against WorldGuard's RegionContainer/RegionQuery API.
     * Avoids compile-time dependency on WorldEdit's BukkitAdapter.
     */
    private boolean queryRegions(Player player, Location loc) throws Exception {
        // WorldGuard.getInstance().getPlatform().getRegionContainer()
        Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
        Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
        Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

        // Get the WorldEdit world for this Bukkit world
        Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object weWorld = bukkitAdapter.getMethod("adapt", org.bukkit.World.class)
            .invoke(null, loc.getWorld());

        // Get the region container for this world
        Object worldContainer = container.getClass().getMethod("get", weWorld.getClass())
            .invoke(container, weWorld);
        if (worldContainer == null) return false;

        // Build a WorldEdit Location from coordinates
        Class<?> blockVector3 = Class.forName("com.sk89q.worldedit.math.BlockVector3");
        Object blockPos = blockVector3.getMethod("at", int.class, int.class, int.class)
            .invoke(null, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        Class<?> weLocClass = Class.forName("com.sk89q.worldedit.util.Location");
        Object weLoc = weLocClass.getConstructor(weWorld.getClass(), blockVector3)
            .newInstance(weWorld, blockPos);

        // query.getApplicableRegions(location)
        Object query = worldContainer.getClass().getMethod("createQuery").invoke(worldContainer);
        Object regions = query.getClass().getMethod("getApplicableRegions", weLocClass)
            .invoke(query, weLoc);

        // !regions.getRegions().isEmpty()
        java.util.Set<?> regionSet = (java.util.Set<?>) regions.getClass()
            .getMethod("getRegions").invoke(regions);
        return !regionSet.isEmpty();
    }

    /**
     * Returns whether WorldGuard integration is functional.
     *
     * @return true if WorldGuard is loaded and API calls are succeeding
     */
    public boolean isAvailable() {
        return available;
    }
}
