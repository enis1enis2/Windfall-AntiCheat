package io.windfall.anticheat.core.punishment;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escalating punishment tiers: warn → kick → tempban → permban.
 *
 * <p>Each tier fires once per player — the {@link #appliedTiers} map tracks the
 * highest tier already applied, preventing duplicate actions at the same tier.
 * Tiers decay when VL drops below the threshold (via {@link #decayTierIfNeeded}).
 *
 * <p>Punishment flow:
 * <ol>
 *   <li>{@link io.windfall.anticheat.core.check.Check#flag(WindfallPlayer)} calls {@code evaluate()}</li>
 *   <li>Engine checks total VL against tier thresholds (highest first)</li>
 *   <li>{@code executeOnce()} applies the punishment if tier hasn't been reached before</li>
 *   <li>Commands are dispatched via console (tempban/ban) for cross-implementation compatibility</li>
 * </ol>
 *
 * @see WindfallConfig#getPunishmentWarnVl() for tier threshold configuration
 */
// Escalating punishment tiers: warn → kick → tempban → permban
// Each tier fires once per player — appliedTiers prevents duplicate actions
public class PunishmentEngine {

    private final WindfallPlugin plugin;
    // Tracks highest tier applied — prevents re-punishing at the same tier
    private final ConcurrentHashMap<UUID, Integer> appliedTiers = new ConcurrentHashMap<>();

    private final boolean enabled;
    private final int warnVl;
    private final int kickVl;
    private final int tempbanVl;
    private final int permbanVl;
    private final String tempbanDuration;
    private final String warnMessage;
    private final String kickMessage;
    private final String tempbanReason;
    private final String permbanReason;

    public PunishmentEngine(WindfallPlugin plugin) {
        this.plugin = plugin;
        WindfallConfig cfg = plugin.getWindfallConfig();
        this.enabled = cfg.isPunishmentsEnabled();
        this.warnVl = cfg.getPunishmentWarnVl();
        this.kickVl = cfg.getPunishmentKickVl();
        this.tempbanVl = cfg.getPunishmentTempbanVl();
        this.permbanVl = cfg.getPunishmentPermbanVl();
        this.tempbanDuration = cfg.getPunishmentTempbanDuration();
        this.warnMessage = cfg.getPunishmentWarnMessage();
        this.kickMessage = cfg.getPunishmentKickMessage();
        this.tempbanReason = cfg.getPunishmentTempbanReason();
        this.permbanReason = cfg.getPunishmentPermbanReason();
    }

    /**
     * Evaluates whether a player should be punished based on their total VL.
     * Checks tiers from highest to lowest — only the highest reached tier fires.
     */
    public void evaluate(WindfallPlayer player) {
        if (!enabled) return;

        int totalVl = player.getTotalViolationLevel();

        if (totalVl >= permbanVl) {
            executeOnce(player, permbanVl);
        } else if (totalVl >= tempbanVl) {
            executeOnce(player, tempbanVl);
        } else if (totalVl >= kickVl) {
            executeOnce(player, kickVl);
        } else if (totalVl >= warnVl) {
            executeOnce(player, warnVl);
        }
    }

    /**
     * Removes the applied tier if the player's VL has dropped below the tier threshold.
     * Called once per tick for all online players.
     */
    public void decayTierIfNeeded(WindfallPlayer player) {
        if (!enabled) return;

        int totalVl = player.getTotalViolationLevel();
        Integer current = appliedTiers.get(player.getUuid());
        if (current == null) return;

        boolean below = false;
        if (current == permbanVl && totalVl < permbanVl) below = true;
        else if (current == tempbanVl && totalVl < tempbanVl) below = true;
        else if (current == kickVl && totalVl < kickVl) below = true;
        else if (current == warnVl && totalVl < warnVl) below = true;

        if (below) {
            appliedTiers.remove(player.getUuid());
        }
    }

    /**
     * Applies the punishment for the given tier, but only once per player per tier.
     *
     * <p>Uses dispatchCommand for ban/tempban because these commands handle async lookups
     * and are more reliable across server implementations than direct API calls.
     * Punishments are executed on the main thread via {@link PlatformScheduler#runSync}.
     *
     * @param player the player to punish
     * @param tier   the VL threshold that triggered this punishment
     */
    // Uses dispatchCommand for ban/tempban because these commands handle async lookups
    // and are more reliable across server implementations than direct API calls
    private void executeOnce(WindfallPlayer player, int tier) {
        Integer current = appliedTiers.getOrDefault(player.getUuid(), 0);
        if (current >= tier) return;
        appliedTiers.put(player.getUuid(), tier);

        plugin.getScheduler().runSync(() -> {
            Player bukkitPlayer = player.getPlayer();
            if (!bukkitPlayer.isOnline()) return;

            if (tier == warnVl) {
                bukkitPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', warnMessage));
            } else if (tier == kickVl) {
                bukkitPlayer.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
            } else if (tier == tempbanVl) {
                String duration = plugin.getWindfallConfig().getPunishmentTempbanDuration().trim();
                bukkitPlayer.getServer().dispatchCommand(
                    bukkitPlayer.getServer().getConsoleSender(),
                    "tempban " + bukkitPlayer.getName() + " " + duration + " " + tempbanReason);
            } else if (tier == permbanVl) {
                bukkitPlayer.getServer().dispatchCommand(
                    bukkitPlayer.getServer().getConsoleSender(),
                    "ban " + bukkitPlayer.getName() + " " + permbanReason);
            }

            plugin.getLogger().info("[Punishment] " + player.getName()
                + " punished at tier " + tier + " (total VL=" + player.getTotalViolationLevel() + ")");
        });
    }

    /**
     * Parses a duration string (e.g., "1d", "12h", "30m") into milliseconds.
     *
     * @return duration in ms, or 24h default if parsing fails
     */
    private long parseDuration(String duration) {
        try {
            String trimmed = duration.trim().toLowerCase();
            long value = Long.parseLong(trimmed.replaceAll("[^0-9]", ""));
            if (trimmed.endsWith("d")) return value * 24 * 60 * 60 * 1000;
            if (trimmed.endsWith("h")) return value * 60 * 60 * 1000;
            if (trimmed.endsWith("m")) return value * 60 * 1000;
            if (trimmed.endsWith("s")) return value * 1000;
            return value;
        } catch (Exception e) {
            return 24 * 60 * 60 * 1000;
        }
    }

    /** Removes tracked tier state for a disconnected player to prevent memory leaks */
    public void cleanup(UUID uuid) {
        appliedTiers.remove(uuid);
    }

    public boolean isEnabled() { return enabled; }
    public int getWarnVl() { return warnVl; }
    public int getKickVl() { return kickVl; }
    public int getTempbanVl() { return tempbanVl; }
    public int getPermbanVl() { return permbanVl; }
    public String getTempbanDuration() { return tempbanDuration; }
}
