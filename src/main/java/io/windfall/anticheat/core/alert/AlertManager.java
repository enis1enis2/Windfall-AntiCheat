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

/**
 * Coordinates in-game chat alerts and Discord webhook dispatch.
 *
 * <p>Alerts are rate-limited per player+check combination to prevent chat spam.
 * The cooldown uses {@link Set#add()} atomic semantics — if the key already exists,
 * the alert is silently dropped. After the cooldown expires, the key is removed
 * via a scheduled task.
 *
 * <p>Alert flow:
 * <ol>
 *   <li>{@link Check#flag(WindfallPlayer)} calls {@code sendAlert()}</li>
 *   <li>Rate limiter checks cooldown — skips if still active</li>
 *   <li>Permission-gated broadcast to all online staff</li>
 *   <li>Optional Discord webhook dispatch via {@link DiscordWebhook}</li>
 * </ol>
 *
 * @see DiscordWebhook for webhook payload construction
 * @see WindfallConfig#isAlertsEnabled() for global enable/disable
 */
public class AlertManager {

    private final WindfallPlugin plugin;
    private final DiscordWebhook discordWebhook;
    // Set.add() returns false if already present — used as a non-blocking rate limiter
    private final Set<String> alertCooldowns = ConcurrentHashMap.newKeySet();

    public AlertManager(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.discordWebhook = new DiscordWebhook(plugin);
    }

    /**
     * Sends an alert to online staff and optionally to Discord.
     *
     * <p>Alerts are suppressed if:
     * <ul>
     *   <li>Alerts are disabled in config</li>
     *   <li>A cooldown is active for this player+check pair</li>
     *   <li>The player has alerts disabled (toggled via command)</li>
     * </ul>
     *
     * @param player the flagged player
     * @param check  the check that triggered the flag
     * @param detail additional context (e.g., "VL=15", "VL=8 (SETBACK)")
     */
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

        // Format and broadcast to staff
        String permission = config.getAlertsStaffPermission();
        String alertMessage = ChatColor.translateAlternateColorCodes('&',
            config.getAlertPrefix() + " &r" + player.getName() + " flagged for &c" + check.getName()
            + " &r(VL: &e" + vl + "&r) — " + detail);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(alertMessage);
            }
        }

        // Dispatch to Discord if enabled
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
