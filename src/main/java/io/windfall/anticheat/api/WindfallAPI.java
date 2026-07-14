package io.windfall.anticheat.api;

import org.bukkit.entity.Player;

/**
 * Public API for Windfall AntiCheat.
 *
 * <p>Provides access to player violation data, check information, and plugin state.
 * Obtain an instance via {@link WindfallProvider#getAPI()}.
 *
 * <p>Example usage:
 * <pre>{@code
 * WindfallAPI api = WindfallProvider.getAPI();
 * if (api != null) {
 *     int vl = api.getViolationLevel(player);
 *     boolean exempt = api.isExempt(player);
 * }
 * }</pre>
 *
 * @see WindfallProvider for obtaining an API instance
 * @see PlayerData for per-player data
 */
public interface WindfallAPI {

    /**
     * Returns the violation level for a player.
     *
     * @param player the player to query
     * @return total violation level, or 0 if player is not tracked
     */
    int getViolationLevel(Player player);

    /**
     * Checks if a player is exempt from all checks.
     *
     * @param player the player to check
     * @return true if the player is exempt
     */
    boolean isExempt(Player player);

    /**
     * Returns per-player data including violation levels and check history.
     *
     * @param player the player to query
     * @return PlayerData for the player, or null if not tracked
     */
    PlayerData getPlayerData(Player player);

    /**
     * Checks if a specific check is enabled.
     *
     * @param checkKey the stable key of the check (e.g., "windfall.movement.speed")
     * @return true if the check is enabled
     */
    boolean isCheckEnabled(String checkKey);

    /**
     * Returns the total number of active checks.
     *
     * @return number of registered and enabled checks
     */
    int getActiveCheckCount();

    /**
     * Returns the plugin version.
     *
     * @return version string (e.g., "1.7.0")
     */
    String getVersion();

    /**
     * Checks if the plugin is fully loaded and operational.
     *
     * @return true if the plugin is ready
     */
    boolean isReady();
}
