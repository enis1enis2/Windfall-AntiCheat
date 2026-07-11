package io.windfall.anticheat.core.alert;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Coordinates in-game chat alerts and Discord webhook dispatch
// Rate-limits alerts per player+check to prevent chat spam
public class AlertManager {

    private final WindfallPlugin plugin;
    private final DiscordWebhook discordWebhook;
    // Set.add() returns false if already present — used as a non-blocking rate limiter
    private final Set<String> alertCooldowns = ConcurrentHashMap.newKeySet();

    public AlertManager(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.discordWebhook = new DiscordWebhook(plugin);
    }

    public void sendAlert(WindfallPlayer player, Check check, String detail) {
        WindfallConfig config = plugin.getWindfallConfig();
        if (!config.isAlertsEnabled()) return;

        int vl = player.getViolationLevels().getOrDefault(check.getStableKey(), 0);
        String cooldownKey = player.getUuid() + ":" + check.getStableKey();

        // Atomic add — returns false if cooldown already active
        long now = System.currentTimeMillis();
        if (!alertCooldowns.add(cooldownKey)) {
            return;
        }
        // Cooldown in ms divided by 50ms tick = number of ticks to wait
        plugin.getScheduler().runLater(() -> alertCooldowns.remove(cooldownKey),
            config.getDiscordRateLimitMs() / 50);

        String permission = config.getAlertsStaffPermission();
        String alertMessage = ChatColor.translateAlternateColorCodes('&',
            config.getAlertPrefix() + " &r" + player.getName() + " flagged for &c" + check.getName()
            + " &r(VL: &e" + vl + "&r) — " + detail);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(alertMessage);
            }
        }

        if (config.isDiscordEnabled()) {
            BedrockInfo bedrock = player.getBedrockInfo();
            String platform = bedrock != null ? bedrock.deviceOs() : "Java";
            String deviceInfo = bedrock != null ? bedrock.inputMode() : "N/A";

            discordWebhook.sendAlert(
                player.getName(), platform, deviceInfo,
                check.getName(), vl,
                config.getDiscordServerName(),
                player.getTransactionPing(),
                detail,
                player.getX(), player.getY(), player.getZ(),
                player.getPlayer().getWorld().getName()
            );
        }
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}
