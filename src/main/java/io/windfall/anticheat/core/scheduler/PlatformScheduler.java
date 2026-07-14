package io.windfall.anticheat.core.scheduler;

import io.windfall.anticheat.WindfallPlugin;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class PlatformScheduler {

    private final WindfallPlugin plugin;
    private final boolean folia;
    private Object globalTask;

    public PlatformScheduler(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    // Folia detection by class presence — RegionizedServer only exists on Folia
    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void startGlobalTick() {
        if (folia) {
            startFoliaGlobalTick();
        } else {
            globalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    // Folia has no global sync scheduler; async + 50ms fixed rate as proxy for anti-cheat heartbeat
    private void startFoliaGlobalTick() {
        try {
            Method getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            Object asyncScheduler = getAsyncScheduler.invoke(null);
            Method runAtFixedRate;
            try {
                runAtFixedRate = asyncScheduler.getClass().getMethod(
                    "runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
            } catch (NoSuchMethodException e) {
                runAtFixedRate = asyncScheduler.getClass().getMethod(
                    "runAtFixedRate", Object.class, Consumer.class, long.class, long.class, TimeUnit.class);
            }
            globalTask = runAtFixedRate.invoke(
                asyncScheduler, plugin, (Consumer<Object>) task -> tick(), 50L, 50L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start Folia global tick: " + e.getMessage());
        }
    }

    // 50ms loop is the heartbeat — runs reward, decay, and punishment evaluation
    private void tick() {
        if (!plugin.isRunning()) return;
        try {
            plugin.getCheckManager().onTick();
        } catch (Exception e) {
            plugin.getLogger().severe("Global tick error: " + e.getMessage());
        }
    }

    // Folia requires global region scheduler for cross-region tasks; Bukkit uses standard task scheduler
    public void runSync(Runnable runnable) {
        if (folia) {
            try {
                Method getGlobalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
                Object scheduler = getGlobalScheduler.invoke(null);
                Method run;
                try {
                    run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class);
                } catch (NoSuchMethodException e) {
                    run = scheduler.getClass().getMethod("run", Object.class, Consumer.class);
                }
                run.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                plugin.getLogger().severe("Folia runSync error: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runAsync(Runnable runnable) {
        if (folia) {
            try {
                Method getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
                Object asyncScheduler = getAsyncScheduler.invoke(null);
                Method runNow;
                try {
                    runNow = asyncScheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, Consumer.class);
                } catch (NoSuchMethodException e) {
                    runNow = asyncScheduler.getClass().getMethod("runNow", Object.class, Consumer.class);
                }
                runNow.invoke(asyncScheduler, plugin, (Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                plugin.getLogger().severe("Folia runAsync error: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public void runLater(Runnable runnable, long delayTicks) {
        if (folia) {
            try {
                Method getGlobalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
                Object scheduler = getGlobalScheduler.invoke(null);
                Method runDelayed;
                try {
                    runDelayed = scheduler.getClass().getMethod(
                        "runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class);
                } catch (NoSuchMethodException e) {
                    runDelayed = scheduler.getClass().getMethod(
                        "runDelayed", Object.class, Consumer.class, long.class);
                }
                runDelayed.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run(), delayTicks);
            } catch (Exception e) {
                plugin.getLogger().severe("Folia runLater error: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public void shutdown() {
        if (globalTask != null) {
            if (folia) {
                try {
                    globalTask.getClass().getMethod("cancel").invoke(globalTask);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to cancel Folia task: " + e.getMessage());
                }
            } else {
                ((BukkitTask) globalTask).cancel();
            }
            globalTask = null;
        }
    }

    public boolean isFolia() {
        return folia;
    }
}
