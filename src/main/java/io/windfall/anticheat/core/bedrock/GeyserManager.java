package io.windfall.anticheat.core.bedrock;

import io.windfall.anticheat.WindfallPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages Bedrock Edition player detection and device info queries via Geyser/Floodgate integration.
 *
 * <p>Uses reflection because Geyser and Floodgate are optional dependencies — the plugin
 * must compile and run without them. Method handles are looked up once at startup via
 * {@link #init(WindfallPlugin)} and cached for all subsequent queries.
 *
 * <p>Detection hierarchy:
 * <ol>
 *   <li><b>Floodgate API</b> (preferred) &mdash; provides full device info (OS, input mode, UI profile,
 *       client version, language). More reliable and feature-rich.</li>
 *   <li><b>GeyserApi</b> (fallback) &mdash; only provides bedrock player detection and client version.
 *       Device info fields return "UNKNOWN".</li>
 * </ol>
 *
 * <p>If neither Geyser nor Floodgate is installed, all queries return false/null and
 * Bedrock checks fall back to Java-only thresholds.
 *
 * <p>Usage:
 * <pre>{@code
 * GeyserManager geyser = GeyserManager.getInstance();
 * if (geyser.isBedrockPlayer(uuid)) {
 *     BedrockInfo info = geyser.getBedrockInfo(uuid);
 *     if (info.isTouchDevice()) { ... }
 * }
 * }</pre>
 *
 * @see BedrockInfo for the returned device info data class
 * @see WindfallConfig#getBedrockGeyserPluginName() for configurable Geyser plugin name
 */
// Handles Geyser/Floodgate detection and Bedrock player queries via reflection
// Reflection is required because Geyser is an optional dependency — can't compile against it
public final class GeyserManager {

    private static GeyserManager instance;

    /** Whether the Geyser plugin was found and enabled at startup */
    private boolean geyserPresent = false;
    /** Whether the Floodgate plugin was found and enabled (provides richer device info) */
    private boolean floodgatePresent = false;

    /* Cached reflection method handles — resolved once at startup via init() */
    private Method floodgateApiGetInstance;
    private Method floodgateApiIsFloodgatePlayer;
    private Method floodgateApiGetPlayer;
    private Method floodgatePlayerGetDeviceOs;
    private Method floodgatePlayerGetInputMode;
    private Method floodgatePlayerGetUiProfile;
    private Method floodgatePlayerGetClientVersion;
    private Method floodgatePlayerGetLanguageCode;
    /** Cached FloodgateApi singleton instance for subsequent queries */
    private Object floodgateApiInstance;

    /** Private constructor — singleton via {@link #init(WindfallPlugin)} */
    private GeyserManager() {}

    /**
     * Initializes the Geyser manager by probing for Geyser and Floodgate plugins.
     * Discovery order: Geyser plugin → Floodgate plugin → GeyserApi fallback.
     * Each step is optional — graceful degradation if not installed.
     *
     * <p>Resolves all Floodgate API method handles via reflection at startup for
     * zero-overhead queries during runtime.
     *
     * @param plugin the Windfall plugin instance for config access and logging
     * @return the initialized GeyserManager singleton
     */
    // Discovery order: Geyser plugin → Floodgate plugin → GeyserApi fallback
    // Each step is optional — graceful degradation if not installed
    public static GeyserManager init(WindfallPlugin plugin) {
        GeyserManager mgr = new GeyserManager();

        String geyserName = plugin.getWindfallConfig().getBedrockGeyserPluginName();
        Plugin geyser = Bukkit.getPluginManager().getPlugin(geyserName);
        if (geyser == null || !geyser.isEnabled()) {
            plugin.getLogger().info("Geyser not found — Bedrock checks will use Java-only thresholds");
            instance = mgr;
            return mgr;
        }

        mgr.geyserPresent = true;
        plugin.getLogger().info("Geyser detected — Bedrock player checks enabled");

        if (plugin.getWindfallConfig().isBedrockUseFloodgateApi()) {
            Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
            if (floodgate != null && floodgate.isEnabled()) {
                try {
                    mgr.floodgateApiGetInstance = Class.forName("org.geysermc.floodgate.api.FloodgateApi")
                        .getMethod("getInstance");
                    mgr.floodgateApiInstance = mgr.floodgateApiGetInstance.invoke(null);
                    mgr.floodgateApiIsFloodgatePlayer = mgr.floodgateApiInstance.getClass()
                        .getMethod("isFloodgatePlayer", UUID.class);
                    mgr.floodgateApiGetPlayer = mgr.floodgateApiInstance.getClass()
                        .getMethod("getPlayer", UUID.class);

                    Class<?> floodgatePlayerClass = Class.forName("org.geysermc.floodgate.api.player.FloodgatePlayer");
                    mgr.floodgatePlayerGetDeviceOs = floodgatePlayerClass.getMethod("getDeviceOs");
                    mgr.floodgatePlayerGetInputMode = floodgatePlayerClass.getMethod("getInputMode");
                    mgr.floodgatePlayerGetUiProfile = floodgatePlayerClass.getMethod("getUiProfile");
                    mgr.floodgatePlayerGetLanguageCode = floodgatePlayerClass.getMethod("getLanguageCode");
                    try {
                        mgr.floodgatePlayerGetClientVersion = floodgatePlayerClass.getMethod("getClientVersion");
                    } catch (NoSuchMethodException ignored) {
                    }

                    mgr.floodgatePresent = true;
                    plugin.getLogger().info("Floodgate API loaded — full Bedrock device info available");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load Floodgate API, falling back to GeyserApi", e);
                }
            }
        }

        instance = mgr;
        return mgr;
    }

    /** Returns the singleton instance, or null if {@link #init} was never called */
    public static GeyserManager getInstance() {
        return instance;
    }

    /** Returns true if the Geyser plugin was found and enabled at startup */
    public boolean isGeyserPresent() {
        return geyserPresent;
    }

    /**
     * Checks if a player is connected via Bedrock Edition (Geyser/Floodgate proxy).
     * Uses a two-tier lookup: Floodgate API (more reliable) → GeyserApi (fallback).
     *
     * @param uuid the player's UUID
     * @return true if the player is a Bedrock Edition player, false if not or if Geyser is absent
     */
    // Two-tier lookup: Floodgate API (more reliable) → GeyserApi (fallback)
    public boolean isBedrockPlayer(UUID uuid) {
        if (!geyserPresent) return false;
        try {
            if (floodgatePresent && floodgateApiInstance != null) {
                return (Boolean) floodgateApiIsFloodgatePlayer.invoke(floodgateApiInstance, uuid);
            }
            Class<?> geyserApi = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = geyserApi.getMethod("api").invoke(null);
            return (Boolean) api.getClass().getMethod("isBedrockPlayer", UUID.class).invoke(api, uuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves device information for a Bedrock player via Floodgate API reflection.
     * Returns a {@link BedrockInfo} with all fields set to "UNKNOWN" if Floodgate is not
     * available or the player info cannot be retrieved.
     *
     * @param uuid the player's UUID
     * @return the player's Bedrock device info, or null if the player is not Bedrock
     */
    public BedrockInfo getBedrockInfo(UUID uuid) {
        if (!isBedrockPlayer(uuid)) return null;
        if (!floodgatePresent || floodgateApiInstance == null) {
            return new BedrockInfo("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "unknown");
        }
        try {
            Object floodgatePlayer = floodgateApiGetPlayer.invoke(floodgateApiInstance, uuid);
            if (floodgatePlayer == null) {
                return new BedrockInfo("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "unknown");
            }

            String deviceOs = invokeSafe(floodgatePlayerGetDeviceOs, floodgatePlayer, "UNKNOWN");
            String inputMode = invokeSafe(floodgatePlayerGetInputMode, floodgatePlayer, "UNKNOWN");
            String uiProfile = invokeSafe(floodgatePlayerGetUiProfile, floodgatePlayer, "UNKNOWN");
            String clientVersion = invokeSafe(floodgatePlayerGetClientVersion, floodgatePlayer, null);
            if (clientVersion == null) {
                clientVersion = getGeyserClientVersion(uuid);
            }
            String languageCode = invokeSafe(floodgatePlayerGetLanguageCode, floodgatePlayer, "unknown");

            return new BedrockInfo(deviceOs, inputMode, uiProfile, clientVersion, languageCode);
        } catch (Exception e) {
            return new BedrockInfo("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "unknown");
        }
    }

    /**
     * Safely invokes a reflection method, returning the fallback value on any exception.
     * Used for all Floodgate player queries to prevent crashes from API changes.
     *
     * @param method   the reflected method to invoke
     * @param target   the object to invoke the method on (FloodgatePlayer instance)
     * @param fallback value to return if invocation fails
     * @return the method result as a String, or the fallback value
     */
    private String invokeSafe(Method method, Object target, String fallback) {
        try {
            Object result = method.invoke(target);
            return result != null ? result.toString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Fallback client version query via GeyserApi when Floodgate doesn't provide it.
     * Uses reflection to access GeyserApi.api().sessionByUuid().getClientVersion().
     *
     * @param uuid the player's UUID
     * @return the client version string, or "unknown" if unavailable
     */
    private String getGeyserClientVersion(UUID uuid) {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = geyserApiClass.getMethod("api").invoke(null);
            Object session = api.getClass().getMethod("sessionByUuid", UUID.class).invoke(api, uuid);
            if (session == null) return "unknown";
            Object clientVersion = session.getClass().getMethod("getClientVersion").invoke(session);
            return clientVersion != null ? clientVersion.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Convenience overload that accepts a Bukkit Player object.
     *
     * @param player the Bukkit Player
     * @return the player's Bedrock device info, or null if not Bedrock
     * @see #getBedrockInfo(UUID)
     */
    public BedrockInfo getBedrockInfo(Player player) {
        return getBedrockInfo(player.getUniqueId());
    }
}
