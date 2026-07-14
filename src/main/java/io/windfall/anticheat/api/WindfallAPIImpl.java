package io.windfall.anticheat.api;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.entity.Player;

/**
 * Implementation of the Windfall public API.
 *
 * <p>Provides read-only access to violation data, check status, and player exemptions.
 * All methods are thread-safe and can be called from any thread.
 *
 * <p>Obtain an instance via {@link WindfallProvider#getAPI()}.
 *
 * @see WindfallAPI for method documentation
 * @see WindfallProvider for obtaining an instance
 */
public final class WindfallAPIImpl implements WindfallAPI {

    private final WindfallPlugin plugin;

    /**
     * Creates a new API implementation.
     *
     * @param plugin the Windfall plugin instance
     */
    public WindfallAPIImpl(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the total violation level across all checks for the given player.
     */
    @Override
    public int getViolationLevel(Player player) {
        WindfallPlayer wp = plugin.getPlayerManager().get(player.getUniqueId());
        return wp != null ? wp.getTotalViolationLevel() : 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks if the player has the windfall.exempt permission.
     */
    @Override
    public boolean isExempt(Player player) {
        WindfallPlayer wp = plugin.getPlayerManager().get(player.getUniqueId());
        return wp != null && wp.getPlayer().hasPermission("windfall.exempt");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a read-only snapshot of the player's data.
     */
    @Override
    public PlayerData getPlayerData(Player player) {
        WindfallPlayer wp = plugin.getPlayerManager().get(player.getUniqueId());
        if (wp == null) return null;

        return new PlayerData(
            wp.getUuid(),
            wp.getTotalViolationLevel(),
            wp.getPlayer().hasPermission("windfall.exempt"),
            wp.isAlertsEnabled(),
            wp.getClientVersion().getProtocolVersion(),
            wp.isBedrock()
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks the CheckManager's registered checks for the given stable key.
     */
    @Override
    public boolean isCheckEnabled(String checkKey) {
        Check check = plugin.getCheckManager().getCheckByStableKey(checkKey);
        return check != null && check.isEnabled();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the count of currently enabled checks.
     */
    @Override
    public int getActiveCheckCount() {
        int count = 0;
        for (Check check : plugin.getCheckManager().getChecks()) {
            if (check.isEnabled()) count++;
        }
        return count;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the version from the plugin's plugin.yml.
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns true if the plugin is enabled and the check manager is initialized.
     */
    @Override
    public boolean isReady() {
        return plugin.isEnabled() && plugin.getCheckManager() != null;
    }
}
