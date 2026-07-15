package io.windfall.anticheat;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

/**
 * bStats metrics integration — tracks anonymized usage statistics.
 *
 * <p>Custom charts:
 * <ul>
 *   <li><b>server_version</b>: Minecraft server version (e.g., "1.21.4")</li>
 *   <li><b>server_fork</b>: Server software (Spigot, Paper, Folia, Purpur, etc.)</li>
 *   <li><b>player_count</b>: Online player count per interval</li>
 *   <li><b>active_checks</b>: Number of registered anti-cheat checks</li>
 *   <li><b>bedrock_players</b>: Number of Bedrock/Geyser players detected</li>
 * </ul>
 *
 * <p>Plugin ID: 32624 (https://bstats.org/plugin/bukkit/Windfall%20Anticheat/32624)
 *
 * @see <a href="https://bstats.org/docs/custom-charts">bStats Custom Charts</a>
 */
public class WindfallMetrics {

    private static final int PLUGIN_ID = 32624;

    /**
     * Initializes bStats metrics. Call once in {@link WindfallPlugin#onEnable()}.
     *
     * @param plugin the Windfall plugin instance
     */
    public static void init(WindfallPlugin plugin) {
        Metrics metrics = new Metrics(plugin, PLUGIN_ID);

        // Server version (e.g., "1.21.4")
        metrics.addCustomChart(new SimplePie("server_version",
            () -> plugin.getVersionManager().getServerVersion()));

        // Server fork (e.g., "Paper", "Spigot", "Folia")
        metrics.addCustomChart(new SimplePie("server_fork",
            () -> plugin.getServerFork().getDisplayName()));

        // Online player count (sampled per interval)
        metrics.addCustomChart(new SingleLineChart("player_count",
            () -> plugin.getServer().getOnlinePlayers().size()));

        // Number of active (registered) checks
        metrics.addCustomChart(new SimplePie("active_checks",
            () -> String.valueOf(plugin.getCheckManager().getChecks().size())));

        // Bedrock/Geyser player count (players tracked by GeyserManager)
        metrics.addCustomChart(new SingleLineChart("bedrock_players",
            () -> {
                if (plugin.getGeyserManager() != null && plugin.getGeyserManager().isGeyserPresent()) {
                    int count = 0;
                    for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                        if (plugin.getGeyserManager().isBedrockPlayer(player.getUniqueId())) {
                            count++;
                        }
                    }
                    return count;
                }
                return 0;
            }));
    }
}
