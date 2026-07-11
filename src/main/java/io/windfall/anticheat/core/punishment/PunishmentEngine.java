package io.windfall.anticheat.core.punishment;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentEngine {

    private final WindfallPlugin plugin;
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
