package io.windfall.anticheat.core.config;

import io.windfall.anticheat.WindfallPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.Set;

public class WindfallConfig {

    private final WindfallPlugin plugin;
    private FileConfiguration config;

    public WindfallConfig(WindfallPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        setDefaults();
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private void setDefaults() {
        // Alerts
        config.addDefault("alerts.enabled", true);
        config.addDefault("alerts.prefix", "&8[&bWindfall&8] &7");
        config.addDefault("alerts.staff-permission", "windfall.alerts");
        config.addDefault("alerts.broadcast-to-all-staff", true);

        // Discord
        config.addDefault("discord.enabled", false);
        config.addDefault("discord.webhook-url", "");
        config.addDefault("discord.server-name", "My Server");
        config.addDefault("discord.mention-on-high-vl", true);
        config.addDefault("discord.mention-threshold", 25);
        config.addDefault("discord.avatar-url", "");
        config.addDefault("discord.embed-color-low", 16776960);
        config.addDefault("discord.embed-color-med", 16744448);
        config.addDefault("discord.embed-color-high", 16711680);
        config.addDefault("discord.rate-limit-ms", 5000);

        // Bedrock
        config.addDefault("bedrock.enabled", true);
        config.addDefault("bedrock.geyser-plugin-name", "Geyser-Spigot");
        config.addDefault("bedrock.use-floodgate-api", true);
        config.addDefault("bedrock.bedrock-reach-multiplier", 1.5);
        config.addDefault("bedrock.bedrock-speed-tolerance", 1.15);
        config.addDefault("bedrock.bedrock-scaffold-threshold", 0.8);
        config.addDefault("bedrock.bedrock-aim-snap-threshold", 200.0);
        config.addDefault("bedrock.bedrock-controller-aim-snap", 185.0);
        config.addDefault("bedrock.bedrock-touch-aim-snap", 200.0);
        config.addDefault("bedrock.bedrock-cps-limit", 20);
        config.addDefault("bedrock.bedrock-tolerance", 1.10);

        // Verbose
        config.addDefault("verbose", true);

        // Severity
        config.addDefault("severity.enabled", true);
        config.addDefault("severity.moderate-vl", 10);
        config.addDefault("severity.high-vl", 25);
        config.addDefault("severity.extreme-vl", 50);
        config.addDefault("severity.moderate-multiplier", 1.3);
        config.addDefault("severity.high-multiplier", 1.6);
        config.addDefault("severity.extreme-multiplier", 2.0);
        config.addDefault("severity.bedrock-discount", 0.6);

        // Punishments
        config.addDefault("punishments.enabled", true);
        config.addDefault("punishments.warn-vl", 5);
        config.addDefault("punishments.kick-vl", 10);
        config.addDefault("punishments.tempban-vl", 20);
        config.addDefault("punishments.tempban-duration", "1d");
        config.addDefault("punishments.permban-vl", 30);
        config.addDefault("punishments.warn-message", "&c[Windfall] &eWarning: further cheating will result in a kick.");
        config.addDefault("punishments.kick-message", "&c[Windfall] Kicked for cheating.");
        config.addDefault("punishments.tempban-reason", "[Windfall] Temporarily banned for cheating.");
        config.addDefault("punishments.permban-reason", "[Windfall] Permanently banned for cheating.");

        // Check defaults
        config.addDefault("checks.default.enabled", true);
        config.addDefault("checks.default.max-vl", 100);
        config.addDefault("checks.default.setback-vl", 20);
        config.addDefault("checks.default.decay", 0.02);
        config.addDefault("checks.default.punishable", true);

        // Per-check defaults
        String[] allChecks = {
            "windfall.movement.speed", "windfall.movement.fly",
            "windfall.movement.velocity", "windfall.movement.timer",
            "windfall.movement.nofall", "windfall.movement.step",
            "windfall.movement.scaffold", "windfall.movement.elytra",
            "windfall.combat.reach", "windfall.combat.aim",
            "windfall.combat.killaura", "windfall.combat.criticals",
            "windfall.combat.fastheal", "windfall.combat.swordblock",
            "windfall.packet.bad", "windfall.packet.cheststealer",
            "windfall.packet.creative"
        };
        for (String key : allChecks) {
            config.addDefault("checks." + key + ".enabled", true);
            config.addDefault("checks." + key + ".max-vl", 100);
            config.addDefault("checks." + key + ".setback-vl", 20);
            config.addDefault("checks." + key + ".decay", 0.02);
            config.addDefault("checks." + key + ".punishable", true);
        }
    }

    // === Alert config ===
    public boolean isAlertsEnabled() {
        return config.getBoolean("alerts.enabled", true);
    }

    public String getAlertPrefix() {
        return config.getString("alerts.prefix", "&8[&bWindfall&8] &7");
    }

    public String getAlertsStaffPermission() {
        return config.getString("alerts.staff-permission", "windfall.alerts");
    }

    public boolean isBroadcastToAllStaff() {
        return config.getBoolean("alerts.broadcast-to-all-staff", true);
    }

    // === Discord config ===
    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", false);
    }

    public String getDiscordWebhookUrl() {
        return config.getString("discord.webhook-url", "");
    }

    public String getDiscordServerName() {
        return config.getString("discord.server-name", "My Server");
    }

    public boolean isDiscordMentionOnHighVl() {
        return config.getBoolean("discord.mention-on-high-vl", true);
    }

    public int getDiscordMentionThreshold() {
        return config.getInt("discord.mention-threshold", 25);
    }

    public String getDiscordAvatarUrl() {
        return config.getString("discord.avatar-url", "");
    }

    public int getDiscordEmbedColor(int vl) {
        if (vl >= getDiscordMentionThreshold()) {
            return config.getInt("discord.embed-color-high", 16711680);
        } else if (vl >= 10) {
            return config.getInt("discord.embed-color-med", 16744448);
        }
        return config.getInt("discord.embed-color-low", 16776960);
    }

    public long getDiscordRateLimitMs() {
        return config.getLong("discord.rate-limit-ms", 5000);
    }

    // === Bedrock config ===
    public boolean isBedrockEnabled() {
        return config.getBoolean("bedrock.enabled", true);
    }

    public String getBedrockGeyserPluginName() {
        return config.getString("bedrock.geyser-plugin-name", "Geyser-Spigot");
    }

    public boolean isBedrockUseFloodgateApi() {
        return config.getBoolean("bedrock.use-floodgate-api", true);
    }

    public double getBedrockReachMultiplier() {
        return config.getDouble("bedrock.bedrock-reach-multiplier", 1.5);
    }

    public double getBedrockSpeedTolerance() {
        return config.getDouble("bedrock.bedrock-speed-tolerance", 1.15);
    }

    public double getBedrockScaffoldThreshold() {
        return config.getDouble("bedrock.bedrock-scaffold-threshold", 0.8);
    }

    public double getBedrockAimSnapThreshold() {
        return config.getDouble("bedrock.bedrock-aim-snap-threshold", 200.0);
    }

    public double getBedrockControllerAimSnap() {
        return config.getDouble("bedrock.bedrock-controller-aim-snap", 185.0);
    }

    public double getBedrockTouchAimSnap() {
        return config.getDouble("bedrock.bedrock-touch-aim-snap", 200.0);
    }

    public int getBedrockCpsLimit() {
        return config.getInt("bedrock.bedrock-cps-limit", 20);
    }

    public double getBedrockTolerance() {
        return config.getDouble("bedrock.bedrock-tolerance", 1.10);
    }

    // === Verbose ===
    public boolean isVerboseEnabled() {
        return config.getBoolean("verbose", true);
    }

    // === Severity config ===
    public boolean isSeverityEnabled() {
        return config.getBoolean("severity.enabled", true);
    }

    public int getSeverityModerateVl() {
        return config.getInt("severity.moderate-vl", 10);
    }

    public int getSeverityHighVl() {
        return config.getInt("severity.high-vl", 25);
    }

    public int getSeverityExtremeVl() {
        return config.getInt("severity.extreme-vl", 50);
    }

    public double getSeverityModerateMultiplier() {
        return config.getDouble("severity.moderate-multiplier", 1.3);
    }

    public double getSeverityHighMultiplier() {
        return config.getDouble("severity.high-multiplier", 1.6);
    }

    public double getSeverityExtremeMultiplier() {
        return config.getDouble("severity.extreme-multiplier", 2.0);
    }

    public double getSeverityBedrockDiscount() {
        return config.getDouble("severity.bedrock-discount", 0.6);
    }

    // === Punishment config ===
    public boolean isPunishmentsEnabled() {
        return config.getBoolean("punishments.enabled", true);
    }

    public int getPunishmentWarnVl() {
        return config.getInt("punishments.warn-vl", 5);
    }

    public int getPunishmentKickVl() {
        return config.getInt("punishments.kick-vl", 10);
    }

    public int getPunishmentTempbanVl() {
        return config.getInt("punishments.tempban-vl", 20);
    }

    public String getPunishmentTempbanDuration() {
        return config.getString("punishments.tempban-duration", "1d");
    }

    public int getPunishmentPermbanVl() {
        return config.getInt("punishments.permban-vl", 30);
    }

    public String getPunishmentWarnMessage() {
        return config.getString("punishments.warn-message",
            "&c[Windfall] &eWarning: further cheating will result in a kick.");
    }

    public String getPunishmentKickMessage() {
        return config.getString("punishments.kick-message", "&c[Windfall] Kicked for cheating.");
    }

    public String getPunishmentTempbanReason() {
        return config.getString("punishments.tempban-reason",
            "[Windfall] Temporarily banned for cheating.");
    }

    public String getPunishmentPermbanReason() {
        return config.getString("punishments.permban-reason",
            "[Windfall] Permanently banned for cheating.");
    }

    // === Check config ===
    public boolean isCheckEnabled(String checkKey) {
        String path = "checks." + checkKey + ".enabled";
        if (config.isSet(path)) {
            return config.getBoolean(path);
        }
        return config.getBoolean("checks.default.enabled", true);
    }

    public int getCheckMaxVl(String checkKey) {
        String path = "checks." + checkKey + ".max-vl";
        if (config.isSet(path)) {
            return config.getInt(path);
        }
        return config.getInt("checks.default.max-vl", 100);
    }

    public int getCheckSetbackVl(String checkKey) {
        String path = "checks." + checkKey + ".setback-vl";
        if (config.isSet(path)) {
            return config.getInt(path);
        }
        return config.getInt("checks.default.setback-vl", 20);
    }

    public double getCheckDecay(String checkKey) {
        String path = "checks." + checkKey + ".decay";
        if (config.isSet(path)) {
            return config.getDouble(path);
        }
        return config.getDouble("checks.default.decay", 0.02);
    }

    public boolean isCheckPunishable(String checkKey) {
        String path = "checks." + checkKey + ".punishable";
        if (config.isSet(path)) {
            return config.getBoolean(path);
        }
        return config.getBoolean("checks.default.punishable", true);
    }

    public Set<String> getCheckKeys() {
        if (config.isConfigurationSection("checks")) {
            return config.getConfigurationSection("checks").getKeys(false);
        }
        return Collections.emptySet();
    }

    // === Config persistence (for GUI sync) ===
    public void saveCheckEnabled(String checkKey, boolean enabled) {
        config.set("checks." + checkKey + ".enabled", enabled);
        plugin.saveConfig();
    }

    public void saveCheckPunishable(String checkKey, boolean punishable) {
        config.set("checks." + checkKey + ".punishable", punishable);
        plugin.saveConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
