package io.windfall.anticheat.core.platform;

import io.windfall.anticheat.WindfallPlugin;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class FoliaCompat {

    private final boolean isFolia;
    private Method entitySchedulerRun;
    private boolean initialized;

    public FoliaCompat(boolean isFolia) {
        this.isFolia = isFolia;
    }

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

    public void runOnPlayer(Player player, Runnable task, Runnable fallback) {
        runOnEntity(player, task, fallback);
    }

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

    public boolean isFolia() { return isFolia; }
}
