package io.windfall.anticheat.core.plugin;

import io.windfall.anticheat.WindfallPlugin;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Detects installed compatibility plugins that affect anti-cheat behaviour.
 *
 * <p>Windfall adapts its checks based on which plugins are installed:
 * <ul>
 *   <li><b>ViaVersion</b>: per-player protocol adaptation (mixed protocol servers)</li>
 *   <li><b>ViaBackwards</b>: backward protocol translation</li>
 *   <li><b>ViaRewind</b>: legacy client support</li>
 *   <li><b>Geyser</b>: Bedrock player adaptation (different movement model)</li>
 *   <li><b>OldCombatMechanics</b>: 1.8 combat emulation (affects attack reach/cooldown)</li>
 * </ul>
 *
 * <p>Called once at plugin startup — flags are checked by checks that need to adjust
 * tolerance or skip detection for compatibility reasons.
 *
 * @see io.windfall.anticheat.core.bedrock.GeyserManager for Bedrock-specific handling
 * @see io.windfall.anticheat.core.check.CheckData#dependencies() for check-level deps
 */
public final class PluginDetector {

    private boolean viaVersionInstalled;
    private boolean viaBackwardsInstalled;
    private boolean viaRewindInstalled;
    private boolean geyserInstalled;
    private boolean oldCombatMechanicsInstalled;
    private boolean worldGuardInstalled;

    /** Scans for installed plugins and logs what was found */
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
            String geyserVersion = getPluginVersion("Geyser-Spigot");
            if (geyserVersion == null) geyserVersion = getPluginVersion("geyser");
            if (geyserVersion != null && isGeyserVulnerable(geyserVersion)) {
                logger.warning("[Windfall] Geyser " + geyserVersion + " has known SSRF vulnerability (CVE-2026-42188) — update to 2.9.3+");
            }
        }

        oldCombatMechanicsInstalled = isPluginEnabled("OldCombatMechanics");
        if (oldCombatMechanicsInstalled) {
            logger.info("[Windfall] OldCombatMechanics detected — 1.8 combat emulation active");
        }

        worldGuardInstalled = isPluginEnabled("WorldGuard");
        if (worldGuardInstalled) {
            logger.info("[Windfall] WorldGuard detected — region-based exemptions available");
        }
    }

    /** Checks if a plugin is both present and enabled */
    private boolean isPluginEnabled(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    public boolean isViaVersionInstalled() { return viaVersionInstalled; }
    public boolean isViaBackwardsInstalled() { return viaBackwardsInstalled; }
    public boolean isViaRewindInstalled() { return viaRewindInstalled; }
    public boolean isGeyserInstalled() { return geyserInstalled; }
    public boolean isOldCombatMechanicsInstalled() { return oldCombatMechanicsInstalled; }
    public boolean isWorldGuardInstalled() { return worldGuardInstalled; }

    /** Returns true if any Via* plugin (Version, Backwards, or Rewind) is installed */
    public boolean isAnyViaVersionPlugin() {
        return viaVersionInstalled || viaBackwardsInstalled || viaRewindInstalled;
    }

    /** Gets the version string of a plugin by name, or null if not found */
    private String getPluginVersion(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null ? plugin.getDescription().getVersion() : null;
    }

    /**
     * Checks if a Geyser version is vulnerable to CVE-2026-42188 (SSRF).
     * Affects all versions <= 2.9.2.
     *
     * @param version Geyser version string
     * @return true if version is vulnerable
     */
    private boolean isGeyserVulnerable(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major < 2 || (major == 2 && minor < 9) || (major == 2 && minor == 9 && patch < 3);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
