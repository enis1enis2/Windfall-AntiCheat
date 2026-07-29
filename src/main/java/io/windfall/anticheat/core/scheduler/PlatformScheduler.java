package io.windfall.anticheat.core.scheduler;

import io.windfall.anticheat.WindfallPlugin;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class PlatformScheduler {

    private final WindfallPlugin plugin;
    private final boolean folia;
    private Object globalTask;
    private ScheduledExecutorService fallbackExecutor;
    private ScheduledFuture<?> fallbackFuture;

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
        // Try Folia AsyncScheduler first
        if (tryFoliaAsyncScheduler()) return;
        // Fallback: use a standalone ScheduledExecutorService
        plugin.getLogger().warning("Windfall: Folia AsyncScheduler unavailable, falling back to own executor");
        fallbackExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Windfall-Folia-Tick");
            t.setDaemon(true);
            return t;
        });
        fallbackFuture = fallbackExecutor.scheduleAtFixedRate(this::tick, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    private boolean tryFoliaAsyncScheduler() {
        try {
            Method getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
            Object asyncScheduler = getAsyncScheduler.invoke(null);
            // Known signatures in preference order
            Class<?>[][] signatures = {
                {org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class},
                {Object.class, Consumer.class, long.class, long.class, TimeUnit.class},
                {Object.class, Consumer.class, long.class, long.class, Object.class}
            };
            Method runAtFixedRate = null;
            for (Class<?>[] params : signatures) {
                try {
                    runAtFixedRate = asyncScheduler.getClass().getMethod("runAtFixedRate", params);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (runAtFixedRate == null) {
                // Scan for any 5-param runAtFixedRate method
                for (Method m : asyncScheduler.getClass().getMethods()) {
                    if (m.getName().equals("runAtFixedRate") && m.getParameterCount() == 5) {
                        runAtFixedRate = m;
                        break;
                    }
                }
            }
            if (runAtFixedRate == null) return false;
            globalTask = runAtFixedRate.invoke(
                asyncScheduler, plugin, (Consumer<Object>) task -> tick(), 50L, 50L, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Windfall: Folia AsyncScheduler init failed: " + e.getMessage());
            return false;
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
        if (fallbackFuture != null) {
            fallbackFuture.cancel(false);
            fallbackFuture = null;
        }
        if (fallbackExecutor != null) {
            fallbackExecutor.shutdown();
            fallbackExecutor = null;
        }
    }

    public boolean isFolia() {
        return folia;
    }
}
