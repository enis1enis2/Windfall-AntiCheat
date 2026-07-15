package io.windfall.anticheat.core.fingerprint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Client behavioral fingerprinting — identifies cheat clients by multi-vector analysis.
 *
 * <p>Builds a composite fingerprint from 5 behavioral vectors:
 * <ol>
 *   <li><b>Brand</b>: client brand string (e.g., "Vanilla", "Feather", "BadLion")</li>
 *   <li><b>Channels</b>: plugin channels registered by the client</li>
 *   <li><b>Protocol extensions</b>: custom packet types or protocol hacks</li>
 *   <li><b>Movement precision</b>: decimal precision of movement packets (vanilla = 4 decimals)</li>
 *   <li><b>Packet timing</b>: statistical distribution of packet intervals</li>
 * </ol>
 *
 * <p>Each vector produces a score 0-20. Combined severity is 0-100.
 * Higher severity = more likely to be a cheat client.
 *
 * <p>Known client signatures are stored in a built-in database.
 * The system is extensible — server admins can add custom signatures via config.
 *
 * <p>Thread-safe: per-player state uses ConcurrentHashMap.
 *
 * <p>Config keys:
 * <ul>
 *   <li>{@code fingerprint.enabled} — master toggle (default true)</li>
 *   <li>{@code fingerprint.min-severity-to-flag} — minimum severity to trigger alert (default 60)</li>
 *   <li>{@code fingerprint.max-fingerprint-age-ticks} — stale fingerprint cleanup (default 6000)</li>
 * </ul>
 *
 * @see io.windfall.anticheat.core.check.CheckManager for integration point
 */
public class PacketFingerprint {

    private static final int DEFAULT_MIN_SEVERITY_TO_FLAG = 60;
    private static final int DEFAULT_MAX_AGE_TICKS = 6000;
    private static final int MAX_TIMING_SAMPLES = 50;

    // Known cheat client signatures (brand substring -> severity bonus)
    private static final Map<String, Integer> KNOWN_CLIENTS = new HashMap<>();
    // Known safe clients (brand substring -> penalty)
    private static final Map<String, Integer> SAFE_CLIENTS = new HashMap<>();

    static {
        // Known cheat clients
        KNOWN_CLIENTS.put("ghost", 30);
        KNOWN_CLIENTS.put("aristois", 35);
        KNOWN_CLIENTS.put("wurst", 35);
        KNOWN_CLIENTS.put("impact", 30);
        KNOWN_CLIENTS.put("future", 30);
        KNOWN_CLIENTS.put("sigma", 25);
        KNOWN_CLIENTS.put("meteor", 30);
        KNOWN_CLIENTS.put("inertia", 25);
        KNOWN_CLIENTS.put("rusher", 20);
        KNOWN_CLIENTS.put("kamino", 25);
        KNOWN_CLIENTS.put("phoenix", 25);
        KNOWN_CLIENTS.put("moon", 20);
        KNOWN_CLIENTS.put("lambda", 25);
        KNOWN_CLIENTS.put("xulu", 30);
        KNOWN_CLIENTS.put("drip", 25);
        KNOWN_CLIENTS.put("hanzo", 25);
        KNOWN_CLIENTS.put("crescent", 25);
        KNOWN_CLIENTS.put("vyx", 30);
        KNOWN_CLIENTS.put("nz", 25);
        KNOWN_CLIENTS.put("bleachhack", 25);

        // Safe clients
        SAFE_CLIENTS.put("vanilla", -20);
        SAFE_CLIENTS.put("fabric", -15);
        SAFE_CLIENTS.put("forge", -15);
        SAFE_CLIENTS.put("optifine", -10);
        SAFE_CLIENTS.put("badlion", -10);
        SAFE_CLIENTS.put("lunar", -10);
        SAFE_CLIENTS.put("feather", -10);
        SAFE_CLIENTS.put("geyser", -5);
        SAFE_CLIENTS.put("bedrock", -5);
    }

    private final Map<UUID, FingerprintState> fingerprints = new ConcurrentHashMap<>();

    private volatile boolean enabled;
    private volatile int minSeverityToFlag;
    private volatile int maxFingerprintAgeTicks;

    /**
     * Creates a PacketFingerprint manager.
     */
    public PacketFingerprint() {
        this.enabled = true;
        this.minSeverityToFlag = DEFAULT_MIN_SEVERITY_TO_FLAG;
        this.maxFingerprintAgeTicks = DEFAULT_MAX_AGE_TICKS;
    }

    /**
     * Reloads config values.
     */
    public void loadConfig(boolean enabled, int minSeverityToFlag, int maxFingerprintAgeTicks) {
        this.enabled = enabled;
        this.minSeverityToFlag = minSeverityToFlag;
        this.maxFingerprintAgeTicks = maxFingerprintAgeTicks;
    }

    // === Vector 1: Brand ===

    /**
     * Records the client brand string. Called from ClientBrandReceiveEvent.
     *
     * @return brand severity score (0-20)
     */
    public int setBrand(UUID playerUuid, String brand) {
        if (!enabled) return 0;

        FingerprintState state = getOrCreate(playerUuid);
        state.brand = brand.toLowerCase().trim();
        state.lastUpdateTick = state.lastUpdateTick; // preserve

        int score = scoreBrand(state.brand);
        state.brandScore = score;
        return score;
    }

    private int scoreBrand(String brand) {
        if (brand == null || brand.isEmpty()) return 10; // unknown = slightly suspicious

        // Check known cheat clients
        for (Map.Entry<String, Integer> entry : KNOWN_CLIENTS.entrySet()) {
            if (brand.contains(entry.getKey())) {
                return Math.min(20, Math.max(0, 10 + entry.getValue() / 3));
            }
        }

        // Check safe clients
        for (Map.Entry<String, Integer> entry : SAFE_CLIENTS.entrySet()) {
            if (brand.contains(entry.getKey())) {
                return Math.max(0, 5 + entry.getValue() / 2);
            }
        }

        return 8; // unknown brand = mildly suspicious
    }

    // === Vector 2: Channels ===

    /**
     * Records a plugin channel registered by the client.
     *
     * @return channel anomaly score (0-20)
     */
    public int addChannel(UUID playerUuid, String channel) {
        if (!enabled) return 0;

        FingerprintState state = getOrCreate(playerUuid);
        state.channels.add(channel.toLowerCase());

        int score = scoreChannels(state);
        state.channelScore = score;
        return score;
    }

    private int scoreChannels(FingerprintState state) {
        Set<String> channels = state.channels;
        if (channels.isEmpty()) return 0;

        int suspicious = 0;
        for (String ch : channels) {
            // Minecraft: namespace:name format
            if (!ch.startsWith("minecraft:") && !ch.startsWith("bungeecord:")
                && !ch.startsWith("spigot:") && !ch.startsWith("papermc:")) {
                suspicious++;
            }
        }

        // More non-standard channels = more suspicious
        return Math.min(20, suspicious * 4);
    }

    // === Vector 3: Protocol Extensions ===

    /**
     * Records a protocol extension indicator (e.g., custom packet type).
     */
    public int recordProtocolExtension(UUID playerUuid, String extensionType) {
        if (!enabled) return 0;

        FingerprintState state = getOrCreate(playerUuid);
        state.protocolExtensions.add(extensionType);

        int score = Math.min(20, state.protocolExtensions.size() * 5);
        state.protocolScore = score;
        return score;
    }

    // === Vector 4: Movement Precision ===

    /**
     * Records the decimal precision of a movement packet coordinate.
     * Vanilla clients use 4 decimal places (1/4096 block precision).
     * Cheat clients often use higher precision.
     *
     * @return precision anomaly score (0-20)
     */
    public int recordMovementPrecision(UUID playerUuid, double coordinate) {
        if (!enabled) return 0;

        FingerprintState state = getOrCreate(playerUuid);

        // Count decimal places: 0.12345678 -> 8
        String str = String.valueOf(coordinate);
        int decimals = 0;
        int dotIndex = str.indexOf('.');
        if (dotIndex >= 0) {
            decimals = str.length() - dotIndex - 1;
        }

        state.precisionSamples.add(decimals);
        if (state.precisionSamples.size() > 100) {
            state.precisionSamples.remove(0);
        }

        // Vanilla = 4 decimals. >6 = suspicious
        double avgPrecision = state.precisionSamples.stream().mapToInt(Integer::intValue).average().orElse(4.0);
        int score = avgPrecision > 6 ? (int) Math.min(20, (avgPrecision - 4) * 3) : 0;
        state.precisionScore = score;
        return score;
    }

    // === Vector 5: Packet Timing ===

    /**
     * Records the time interval between consecutive packets from a player.
     * Perfectly regular intervals suggest bot-controlled movement.
     *
     * @return timing anomaly score (0-20)
     */
    public int recordPacketInterval(UUID playerUuid, long intervalMs) {
        if (!enabled) return 0;

        FingerprintState state = getOrCreate(playerUuid);

        state.timingSamples.add(intervalMs);
        if (state.timingSamples.size() > MAX_TIMING_SAMPLES) {
            state.timingSamples.remove(0);
        }

        int score = scoreTiming(state);
        state.timingScore = score;
        return score;
    }

    private int scoreTiming(FingerprintState state) {
        if (state.timingSamples.size() < 10) return 0;

        // Calculate coefficient of variation (stddev / mean)
        double mean = state.timingSamples.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 0;

        double variance = state.timingSamples.stream()
            .mapToLong(Long::longValue)
            .mapToDouble(v -> (v - mean) * (v - mean))
            .average()
            .orElse(0);

        double stddev = Math.sqrt(variance);
        double cv = stddev / mean;

        // Very low CV = perfectly timed = bot-like
        // Vanilla players have CV > 0.3 typically
        if (cv < 0.05) return 18; // almost perfectly timed
        if (cv < 0.1) return 12;
        if (cv < 0.2) return 6;
        return 0; // natural-looking timing
    }

    // === Composite Severity ===

    /**
     * Calculates the composite severity score for a player.
     *
     * @return severity 0-100
     */
    public int getSeverity(UUID playerUuid) {
        FingerprintState state = fingerprints.get(playerUuid);
        if (state == null) return 0;

        int total = state.brandScore + state.channelScore + state.protocolScore
            + state.precisionScore + state.timingScore;

        return Math.min(100, Math.max(0, total));
    }

    /**
     * Returns true if the player's fingerprint exceeds the flag threshold.
     */
    public boolean shouldFlag(UUID playerUuid) {
        return getSeverity(playerUuid) >= minSeverityToFlag;
    }

    /**
     * Returns a human-readable summary of the player's fingerprint.
     */
    public String getSummary(UUID playerUuid) {
        FingerprintState state = fingerprints.get(playerUuid);
        if (state == null) return "No fingerprint data";

        StringBuilder sb = new StringBuilder();
        sb.append("Brand=").append(state.brandScore);
        sb.append(" Channels=").append(state.channelScore);
        sb.append(" Protocol=").append(state.protocolScore);
        sb.append(" Precision=").append(state.precisionScore);
        sb.append(" Timing=").append(state.timingScore);
        sb.append(" | Total=").append(getSeverity(playerUuid));
        sb.append(" | Brand=").append(state.brand != null ? state.brand : "unknown");
        return sb.toString();
    }

    // === Lifecycle ===

    /**
     * Per-tick cleanup of stale fingerprints.
     */
    public void onTick(long currentTick) {
        if (!enabled) return;

        fingerprints.entrySet().removeIf(entry -> {
            long age = currentTick - entry.getValue().lastUpdateTick;
            return age > maxFingerprintAgeTicks;
        });
    }

    /**
     * Removes a player's fingerprint (on disconnect).
     */
    public void removePlayer(UUID playerUuid) {
        fingerprints.remove(playerUuid);
    }

    /** Returns the number of active fingerprints being tracked. */
    public int getTrackedPlayerCount() {
        return fingerprints.size();
    }

    /** Returns true if fingerprinting is enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Returns the minimum severity to trigger a flag. */
    public int getMinSeverityToFlag() {
        return minSeverityToFlag;
    }

    private FingerprintState getOrCreate(UUID playerUuid) {
        return fingerprints.computeIfAbsent(playerUuid, k -> {
            FingerprintState state = new FingerprintState();
            state.lastUpdateTick = 0;
            return state;
        });
    }

    // === Internal state ===

    static class FingerprintState {
        String brand;
        Set<String> channels = new LinkedHashSet<>();
        Set<String> protocolExtensions = new LinkedHashSet<>();
        List<Integer> precisionSamples = new ArrayList<>();
        List<Long> timingSamples = new ArrayList<>();

        int brandScore;
        int channelScore;
        int protocolScore;
        int precisionScore;
        int timingScore;

        long lastUpdateTick;
    }
}
