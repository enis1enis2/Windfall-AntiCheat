package io.windfall.anticheat.core.alert;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.config.WindfallConfig;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// Zero-dependency Discord webhook using raw HttpURLConnection
// Avoids adding Discord SDK or webhook4j as dependencies
public class DiscordWebhook {

    private final WindfallPlugin plugin;
    // Per-player+check rate limit — separate from AlertManager's in-game cooldown
    private final Map<String, Long> lastAlertTimes = new ConcurrentHashMap<>();

    public DiscordWebhook(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    // Sends a rich embed to Discord — runs async to avoid blocking the server thread
    public void sendAlert(String playerName, String platform, String deviceInfo,
                          String checkName, int vl, String serverName,
                          int ping, String detail,
                          double x, double y, double z, String world) {
        WindfallConfig config = plugin.getWindfallConfig();
        String webhookUrl = config.getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        String rateLimitKey = playerName + ":" + checkName;
        long now = System.currentTimeMillis();
        Long lastTime = lastAlertTimes.get(rateLimitKey);
        if (lastTime != null && now - lastTime < config.getDiscordRateLimitMs()) {
            return;
        }
        lastAlertTimes.put(rateLimitKey, now);

        int color = config.getDiscordEmbedColor(vl);
        String mention = "";
        if (config.isDiscordMentionOnHighVl() && vl >= config.getDiscordMentionThreshold()) {
            mention = "@everyone ";
        }

        // mc-heads.net is a free Minecraft avatar API — no API key required
        String headUrl = "https://mc-heads.net/avatar/" + playerName + "/100";

        // Manual JSON construction avoids adding a JSON library dependency
        StringBuilder json = new StringBuilder();
        json.append("{\"content\":\"").append(escapeJson(mention)).append("\",");
        json.append("\"embeds\":[{");
        json.append("\"title\":\"Windfall Alert\",");
        json.append("\"color\":").append(color).append(",");
        json.append("\"thumbnail\":{\"url\":\"").append(escapeJson(headUrl)).append("\"},");
        json.append("\"fields\":[");
        json.append(field("Player", playerName + " (" + platform + ")", true));
        json.append(",").append(field("Check", checkName, true));
        json.append(",").append(field("VL", String.valueOf(vl), true));
        json.append(",").append(field("Server", serverName, true));
        json.append(",").append(field("Ping", ping + "ms", true));
        json.append(",").append(field("Device", deviceInfo, true));
        json.append(",").append(field("Position", String.format("X:%.1f Y:%.1f Z:%.1f", x, y, z), true));
        json.append(",").append(field("World", world, true));
        json.append(",").append(field("Details", detail, false));
        json.append("]}]}");

        String payload = json.toString();

        plugin.getScheduler().runAsync(() -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "Windfall-AntiCheat/1.0");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 204 && responseCode != 200) {
                    plugin.getLogger().warning("Discord webhook returned HTTP " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escapeJson(name) + "\",\"value\":\"" + escapeJson(value) + "\",\"inline\":" + inline + "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Evicts stale rate-limit entries older than 5 minutes — called periodically from CheckManager.onTick() */
    public void cleanupStaleEntries() {
        long cutoff = System.currentTimeMillis() - 300_000L;
        lastAlertTimes.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }
}
