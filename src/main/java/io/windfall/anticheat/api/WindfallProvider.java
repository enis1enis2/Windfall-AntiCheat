package io.windfall.anticheat.api;

import io.windfall.anticheat.WindfallPlugin;

/**
 * Singleton provider for accessing the Windfall API.
 *
 * <p>Use this class to obtain an {@link WindfallAPI} instance. The API is only
 * available after the plugin has fully initialized. If the plugin is not loaded
 * or is disabled, {@link #getAPI()} returns null.
 *
 * <p>Example usage:
 * <pre>{@code
 * // In your plugin's onEnable:
 * WindfallAPI api = WindfallProvider.getAPI();
 * if (api != null) {
 *     getLogger().info("Windfall " + api.getVersion() + " detected");
 * }
 * }</pre>
 *
 * @see WindfallAPI for available methods
 */
public final class WindfallProvider {

    private static WindfallAPI api;

    private WindfallProvider() {
        // Utility class — no instantiation
    }

    /**
     * Returns the Windfall API instance.
     *
     * @return API instance, or null if Windfall is not loaded
     */
    public static WindfallAPI getAPI() {
        return api;
    }

    /**
     * Registers the API instance. Called internally by Windfall during initialization.
     *
     * @param apiInstance the API implementation
     */
    public static void register(WindfallAPI apiInstance) {
        api = apiInstance;
    }

    /**
     * Unregisters the API instance. Called internally when Windfall is disabled.
     */
    public static void unregister() {
        api = null;
    }

    /**
     * Checks if the API is available.
     *
     * @return true if an API instance is registered
     */
    public static boolean isAvailable() {
        return api != null;
    }
}
