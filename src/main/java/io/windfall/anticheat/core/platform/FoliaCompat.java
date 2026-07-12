package io.windfall.anticheat.core.platform;

import io.windfall.anticheat.WindfallPlugin;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Folia compatibility layer — handles regionised multithreading safely.
 *
 * <p>Folia (Paper's multithreaded fork) requires all entity operations to run
 * on the owning region's thread. This class uses reflection to call Folia's
 * {@code EntityScheduler} API without a compile-time dependency.
 *
 * <p>On non-Folia servers, all operations fall back to synchronous Bukkit scheduler
 * calls or direct execution. This ensures Windfall works identically on any fork.
 *
 * <p>Key operations:
 * <ul>
 *   <li>{@link #runOnEntity}: schedules a task on an entity's region thread</li>
 *   <li>{@link #isOwnedByCurrentRegion}: checks thread ownership before accessing entity data</li>
 *   <li>{@link #teleportAsync}: uses Folia's async teleport when available</li>
 * </ul>
 *
 * @see ServerFork#FOLIA for fork detection
 * @see io.windfall.anticheat.core.check.CheckManager for usage in check dispatch
 */
public final class FoliaCompat {

    private final boolean isFolia;
    private Method entitySchedulerRun;
    private boolean initialized;

    /** Creates a Folia compat layer. Pass true if Folia was detected at startup. */
    public FoliaCompat(boolean isFolia) {
        this.isFolia = isFolia;
    }

    /**
     * Initialises the reflection handle for EntityScheduler.run().
     * Must be called after Folia is detected — silently no-ops on non-Folia.
     */
    public void init(Logger logger) {
        if (!isFolia) return;
        try {
            entitySchedulerRun = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler")
                .getMethod("run", Object.class, java.util.function.Consumer.class, Runnable.class);
            initialized = true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to init Folia EntityScheduler reflection", e);
        }
    }

    /**
     * Schedules a task to run on an entity's owning region thread.
     * On non-Folia servers, runs synchronously on the main thread.
     *
     * @param entity the target entity (must be in a loaded chunk)
     * @param task the task to run on the region thread
     * @param fallback fallback task if scheduling fails (may be null)
     */
    public void runOnEntity(Entity entity, Runnable task, Runnable fallback) {
        if (!isFolia || !initialized || entity == null) {
            if (task != null) task.run();
            return;
        }
        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            entitySchedulerRun.invoke(scheduler, WindfallPlugin.getInstance(),
                (java.util.function.Consumer<Object>) t -> task.run(), fallback);
        } catch (Exception e) {
            if (fallback != null) fallback.run();
        }
    }

    /** Convenience wrapper for Player entities */
    public void runOnPlayer(Player player, Runnable task, Runnable fallback) {
        runOnEntity(player, task, fallback);
    }

    /**
     * Checks if two entities are in the same Folia region.
     * Always returns true on non-Folia servers (single-threaded).
     */
    public boolean isSameRegion(Entity a, Entity b) {
        if (!isFolia || a == null || b == null) return true;
        try {
            Method isOwnedByCurrentRegion = Entity.class.getMethod("isOwnedByCurrentRegion");
            if (!(Boolean) isOwnedByCurrentRegion.invoke(b)) return false;
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Teleports a player asynchronously when Folia is available.
     * Falls back to synchronous teleport on non-Folia servers.
     */
    public void teleportAsync(Player player, Location location) {
        if (isFolia) {
            try {
                Method teleportAsync = Player.class.getMethod("teleportAsync", Location.class);
                teleportAsync.invoke(player, location);
                return;
            } catch (Exception ignored) {}
        }
        player.teleport(location);
    }

    /**
     * Checks if the current thread owns the entity's region.
     * Used to guard access to entity data — prevents cross-region race conditions.
     */
    public boolean isOwnedByCurrentRegion(Entity entity) {
        if (!isFolia || entity == null) return !isFolia || Bukkit.isPrimaryThread();
        try {
            Method method = Bukkit.class.getMethod("isOwnedByCurrentRegion", Entity.class);
            Object result = method.invoke(null, entity);
            return result instanceof Boolean ? (Boolean) result : true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** Location-based overload — checks region ownership by position */
    public boolean isOwnedByCurrentRegion(Location location) {
        if (!isFolia || location == null) return !isFolia || Bukkit.isPrimaryThread();
        try {
            Method method = Bukkit.class.getMethod("isOwnedByCurrentRegion", Location.class);
            Object result = method.invoke(null, location);
            return result instanceof Boolean ? (Boolean) result : true;
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Schedules a delayed task on a player's region thread.
     * On non-Folia, uses Bukkit.getScheduler().runTaskLater().
     */
    public Runnable runOnPlayerLater(Player player, long delayTicks, Runnable task) {
        if (player == null) return () -> {};
        if (!isFolia) {
            Bukkit.getScheduler().runTaskLater(WindfallPlugin.getInstance(), task, delayTicks);
            return task;
        }
        runOnEntity(player, task, task);
        return task;
    }

    /** Returns true if this server is running Folia */
    public boolean isFolia() { return isFolia; }
}
