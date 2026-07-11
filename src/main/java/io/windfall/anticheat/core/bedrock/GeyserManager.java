package io.windfall.anticheat.core.bedrock;

import io.windfall.anticheat.WindfallPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

public final class GeyserManager {

    private static GeyserManager instance;

    private boolean geyserPresent = false;
    private boolean floodgatePresent = false;

    private Method floodgateApiGetInstance;
    private Method floodgateApiIsFloodgatePlayer;
    private Method floodgateApiGetPlayer;
    private Method floodgatePlayerGetDeviceOs;
    private Method floodgatePlayerGetInputMode;
    private Method floodgatePlayerGetUiProfile;
    private Method floodgatePlayerGetClientVersion;
    private Method floodgatePlayerGetLanguageCode;
    private Object floodgateApiInstance;

    private GeyserManager() {}

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

    public static GeyserManager getInstance() {
        return instance;
    }

    public boolean isGeyserPresent() {
        return geyserPresent;
    }

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

    private String invokeSafe(Method method, Object target, String fallback) {
        try {
            Object result = method.invoke(target);
            return result != null ? result.toString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

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

    public BedrockInfo getBedrockInfo(Player player) {
        return getBedrockInfo(player.getUniqueId());
    }
}
