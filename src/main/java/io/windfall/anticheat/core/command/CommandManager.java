package io.windfall.anticheat.core.command;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Registers /windfall via CommandMap reflection — avoids plugin.yml dependency issues
// Subcommands are dispatched in a switch for clarity over annotation-based frameworks
public class CommandManager {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "reload", "info", "alerts", "verbose", "setback", "debug", "checks", "toggle", "severity", "version", "gui", "help"
    );

    private final WindfallPlugin plugin;
    private final PlayerManager playerManager;

    public CommandManager(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        registerCommand();
    }

    // Reflective registration — works on all server forks without plugin.yml edits
    private void registerCommand() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            CommandMap map = (CommandMap) field.get(Bukkit.getServer());
            map.register(plugin.getName(), new WindfallCommand());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register /windfall command: " + e.getMessage());
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&7[&bWindfall&7] &7" + message));
    }

    private void handleReload(CommandSender sender) {
        plugin.getCheckManager().reloadChecks();
        sendMessage(sender, "Configuration reloaded. Checks re-evaluated.");
    }

    private void handleInfo(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        int playerCount = playerManager.size();
        List<Check> checks = plugin.getCheckManager().getChecks();
        long enabledCount = checks.stream().filter(Check::isEnabled).count();

        sender.sendMessage(ChatColor.AQUA + "=== Windfall ===");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + version);
        sender.sendMessage(ChatColor.GRAY + "Players tracked: " + ChatColor.WHITE + playerCount);
        sender.sendMessage(ChatColor.GRAY + "Active checks: " + ChatColor.WHITE + enabledCount + "/" + checks.size());

        if (plugin.getGeyserManager().isGeyserPresent()) {
            sender.sendMessage(ChatColor.GRAY + "Bedrock: " + ChatColor.GREEN + "Geyser detected");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Bedrock: " + ChatColor.RED + "Not available");
        }
    }

    private void handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "Only players can toggle alerts.");
            return;
        }
        Player player = (Player) sender;

        WindfallPlayer wp = playerManager.get(player.getUniqueId());
        if (wp == null) {
            sendMessage(sender, "No player data found.");
            return;
        }

        wp.setAlertsEnabled(!wp.isAlertsEnabled());
        sendMessage(sender, "Alerts " + (wp.isAlertsEnabled() ? "enabled" : "disabled") + ".");
    }

    private void handleVerbose(CommandSender sender, String[] args) {
        boolean current = plugin.getWindfallConfig().isVerboseEnabled();
        sendMessage(sender, "Verbose logging is currently " + (current ? "enabled" : "disabled") + ".");
    }

    private void handleSetback(CommandSender sender, String targetName) {
        if (targetName == null) {
            sendMessage(sender, "Usage: /windfall setback <player>");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sendMessage(sender, "Player not found.");
            return;
        }

        WindfallPlayer wp = playerManager.get(target.getUniqueId());
        if (wp == null) {
            sendMessage(sender, "No data tracked for that player.");
            return;
        }

        target.teleport(new org.bukkit.Location(
            target.getWorld(),
            wp.getGroundX(), wp.getGroundY(), wp.getGroundZ(),
            target.getLocation().getYaw(), target.getLocation().getPitch()
        ));

        sendMessage(sender, "Setback applied to " + target.getName() + ".");
    }

    private void handleChecks(CommandSender sender) {
        List<Check> checks = plugin.getCheckManager().getChecks();
        sender.sendMessage(ChatColor.AQUA + "=== Active Checks ===");

        for (Check check : checks) {
            String status = check.isEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF";
            String punish = check.isPunishable() ? ChatColor.RED + " [P]" : ChatColor.YELLOW + " [S]";

            sender.sendMessage(ChatColor.GRAY + "  " + check.getName() + " "
                + status + punish + ChatColor.DARK_GRAY + " (key: " + check.getStableKey() + ")");
        }
    }

    private void handleToggle(CommandSender sender, String checkKey) {
        if (checkKey == null) {
            sendMessage(sender, "Usage: /windfall toggle <check-key>");
            return;
        }

        CheckManager checkManager = plugin.getCheckManager();
        Check check = checkManager.getCheckByStableKey(checkKey);
        if (check == null) {
            sendMessage(sender, "Check not found: " + checkKey);
            sendMessage(sender, "Use /windfall checks to list all checks.");
            return;
        }

        check.setEnabled(!check.isEnabled());
        sendMessage(sender, check.getName() + " " + (check.isEnabled() ? "enabled" : "disabled") + ".");
    }

    private void handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String server = plugin.getVersionManager().getServerVersion();
        boolean folia = plugin.getScheduler().isFolia();

        sender.sendMessage(ChatColor.AQUA + "Windfall v" + version);
        sender.sendMessage(ChatColor.GRAY + "Server: " + server);
        sender.sendMessage(ChatColor.GRAY + "Folia: " + folia);
        sender.sendMessage(ChatColor.GRAY + "Java 11+ required");
    }

    // Debug shows all tracked data for a player — useful for diagnosing false positives
    private void handleDebug(CommandSender sender, String targetName) {
        if (targetName == null) {
            sendMessage(sender, "Usage: /windfall debug <player>");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sendMessage(sender, "Player not found.");
            return;
        }

        WindfallPlayer wp = playerManager.get(target.getUniqueId());
        if (wp == null) {
            sendMessage(sender, "No data tracked for that player.");
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Debug: " + target.getName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Position: " + ChatColor.WHITE
            + String.format("%.3f %.3f %.3f", wp.getX(), wp.getY(), wp.getZ()));
        sender.sendMessage(ChatColor.GRAY + "Ground: " + ChatColor.WHITE
            + String.format("%.3f %.3f %.3f", wp.getGroundX(), wp.getGroundY(), wp.getGroundZ()));
        sender.sendMessage(ChatColor.GRAY + "Delta: " + ChatColor.WHITE
            + String.format("%.4f %.4f %.4f", wp.getDeltaX(), wp.getDeltaY(), wp.getDeltaZ()));
        sender.sendMessage(ChatColor.GRAY + "OnGround: " + ChatColor.WHITE + wp.isOnGround());
        sender.sendMessage(ChatColor.GRAY + "Flying: " + ChatColor.WHITE + wp.isFlying());
        sender.sendMessage(ChatColor.GRAY + "Sprinting: " + ChatColor.WHITE + wp.isSprinting());
        sender.sendMessage(ChatColor.GRAY + "Ping: " + ChatColor.WHITE + wp.getTransactionPing() + "ms");
        sender.sendMessage(ChatColor.GRAY + "Ticks: " + ChatColor.WHITE + wp.getTickCount());
        sender.sendMessage(ChatColor.GRAY + "Protocol: " + ChatColor.WHITE + "v" + wp.getProtocolVersion());
        sender.sendMessage(ChatColor.GRAY + "Total VL: " + ChatColor.WHITE + wp.getTotalViolationLevel());
        sender.sendMessage(ChatColor.GRAY + "Severity: " + ChatColor.YELLOW
            + plugin.getSeverityManager().getSeverityLabel(wp));

        if (wp.isBedrock()) {
            io.windfall.anticheat.core.bedrock.BedrockInfo bedrock = wp.getBedrockInfo();
            sender.sendMessage(ChatColor.GRAY + "Bedrock: " + ChatColor.LIGHT_PURPLE
                + bedrock.deviceOs() + " (" + bedrock.inputMode() + ")");
            sender.sendMessage(ChatColor.GRAY + "UI: " + ChatColor.LIGHT_PURPLE
                + bedrock.uiProfile() + " v" + bedrock.clientVersion());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Platform: " + ChatColor.WHITE + "Java");
        }

        StringBuilder violations = new StringBuilder(ChatColor.GRAY + "Violations: ");
        for (java.util.Map.Entry<String, Integer> entry : wp.getViolationLevels().entrySet()) {
            violations.append("\n  ").append(ChatColor.DARK_GRAY).append(entry.getKey())
                .append(": ").append(ChatColor.RED).append(entry.getValue());
        }
        sender.sendMessage(violations.toString());
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "Only players can use the GUI.");
            return;
        }
        Player player = (Player) sender;
        if (plugin.getChecklistGUI() == null) {
            sendMessage(sender, "GUI not initialized.");
            return;
        }
        plugin.getChecklistGUI().open(player);
    }

    private void handleSeverity(CommandSender sender, String targetName) {
        if (targetName == null) {
            sendMessage(sender, "Usage: /windfall severity <player>");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sendMessage(sender, "Player not found.");
            return;
        }

        WindfallPlayer wp = playerManager.get(target.getUniqueId());
        if (wp == null) {
            sendMessage(sender, "No data tracked for that player.");
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Severity: " + target.getName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Total VL: " + ChatColor.WHITE + wp.getTotalViolationLevel());
        sender.sendMessage(ChatColor.GRAY + "Severity: " + ChatColor.YELLOW
            + plugin.getSeverityManager().getSeverityLabel(wp));
        sender.sendMessage(ChatColor.GRAY + "VL Multiplier: " + ChatColor.WHITE
            + String.format("%.1fx", plugin.getSeverityManager().getSeverityMultiplier(wp)));

        if (wp.isBedrock()) {
            sender.sendMessage(ChatColor.GRAY + "Bedrock Discount: " + ChatColor.LIGHT_PURPLE
                + String.format("%.0f%%", plugin.getSeverityManager().getBedrockDiscount() * 100));
        }

        StringBuilder violations = new StringBuilder(ChatColor.GRAY + "Per-Check VLs: ");
        for (java.util.Map.Entry<String, Integer> entry : wp.getViolationLevels().entrySet()) {
            violations.append("\n  ").append(ChatColor.DARK_GRAY).append(entry.getKey())
                .append(": ").append(ChatColor.RED).append(entry.getValue());
        }
        sender.sendMessage(violations.toString());
    }

    private void handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "=== Windfall Commands ===");
        sender.sendMessage(ChatColor.GOLD + "/windfall reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.GOLD + "/windfall info" + ChatColor.GRAY + " - Plugin information");
        sender.sendMessage(ChatColor.GOLD + "/windfall checks" + ChatColor.GRAY + " - List all checks");
        sender.sendMessage(ChatColor.GOLD + "/windfall toggle <key>" + ChatColor.GRAY + " - Toggle a check");
        sender.sendMessage(ChatColor.GOLD + "/windfall alerts" + ChatColor.GRAY + " - Toggle alerts");
        sender.sendMessage(ChatColor.GOLD + "/windfall debug <player>" + ChatColor.GRAY + " - Debug info");
        sender.sendMessage(ChatColor.GOLD + "/windfall severity <player>" + ChatColor.GRAY + " - Severity info");
        sender.sendMessage(ChatColor.GOLD + "/windfall setback <player>" + ChatColor.GRAY + " - Setback a player");
        sender.sendMessage(ChatColor.GOLD + "/windfall version" + ChatColor.GRAY + " - Version info");
        sender.sendMessage(ChatColor.GOLD + "/windfall gui" + ChatColor.GRAY + " - Open check GUI");
    }

    // Inner class keeps command registration self-contained
    private class WindfallCommand extends Command {

        protected WindfallCommand() {
            super("windfall", "Windfall anti-cheat commands", "/windfall help",
                Arrays.asList("wf", "wfall"));
            setPermission("windfall.admin");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!testPermission(sender)) return true;

            if (args.length == 0) {
                handleHelp(sender);
                return true;
            }

            String sub = args[0].toLowerCase();
            switch (sub) {
                case "reload": handleReload(sender); break;
                case "info": handleInfo(sender); break;
                case "alerts": handleAlerts(sender); break;
                case "verbose": handleVerbose(sender, args); break;
                case "setback": handleSetback(sender, args.length > 1 ? args[1] : null); break;
                case "debug": handleDebug(sender, args.length > 1 ? args[1] : null); break;
                case "checks": handleChecks(sender); break;
                case "toggle": handleToggle(sender, args.length > 1 ? args[1] : null); break;
                case "severity": handleSeverity(sender, args.length > 1 ? args[1] : null); break;
                case "version": handleVersion(sender); break;
                case "gui": handleGui(sender); break;
                case "help": handleHelp(sender); break;
                default: sendMessage(sender, "Unknown command. Use /windfall help for available commands.");
            }

            return true;
        }

        // Tab completion for subcommands and player names — only for admins
        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (!sender.hasPermission("windfall.admin")) {
                return Collections.emptyList();
            }

            if (args.length == 1) {
                String prefix = args[0].toLowerCase();
                return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
            }

            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                String prefix = args[1].toLowerCase();

                if (sub.equals("setback") || sub.equals("debug") || sub.equals("severity")) {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
                }

                if (sub.equals("toggle")) {
                    return plugin.getCheckManager().getChecks().stream()
                        .map(Check::getStableKey)
                        .filter(key -> key.startsWith(prefix))
                        .collect(Collectors.toList());
                }
            }

            return Collections.emptyList();
        }
    }
}
