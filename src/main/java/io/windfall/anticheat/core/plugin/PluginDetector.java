package io.windfall.anticheat.core.plugin;

import io.windfall.anticheat.WindfallPlugin;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class PluginDetector {

    private boolean viaVersionInstalled;
    private boolean viaBackwardsInstalled;
    private boolean viaRewindInstalled;
    private boolean geyserInstalled;
    private boolean oldCombatMechanicsInstalled;

    public void init(WindfallPlugin plugin) {
        Logger logger = plugin.getLogger();

        viaVersionInstalled = isPluginEnabled("ViaVersion");
        if (viaVersionInstalled) {
            logger.info("[Windfall] ViaVersion detected — per-player protocol adaptation active");
        }

        viaBackwardsInstalled = isPluginEnabled("ViaBackwards");
        if (viaBackwardsInstalled) {
            logger.info("[Windfall] ViaBackwards detected — backward protocol translation active");
        }

        viaRewindInstalled = isPluginEnabled("ViaRewind");
        if (viaRewindInstalled) {
            logger.info("[Windfall] ViaRewind detected — legacy client support active");
        }

        geyserInstalled = isPluginEnabled("Geyser-Spigot") || isPluginEnabled("geyser");
        if (geyserInstalled) {
            logger.info("[Windfall] Geyser detected — Bedrock player adaptation active");
        }

        oldCombatMechanicsInstalled = isPluginEnabled("OldCombatMechanics");
        if (oldCombatMechanicsInstalled) {
            logger.info("[Windfall] OldCombatMechanics detected — 1.8 combat emulation active");
        }
    }

    private boolean isPluginEnabled(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    public boolean isViaVersionInstalled() { return viaVersionInstalled; }
    public boolean isViaBackwardsInstalled() { return viaBackwardsInstalled; }
    public boolean isViaRewindInstalled() { return viaRewindInstalled; }
    public boolean isGeyserInstalled() { return geyserInstalled; }
    public boolean isOldCombatMechanicsInstalled() { return oldCombatMechanicsInstalled; }

    public boolean isAnyViaVersionPlugin() {
        return viaVersionInstalled || viaBackwardsInstalled || viaRewindInstalled;
    }
}
