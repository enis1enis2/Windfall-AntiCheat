package io.windfall.anticheat.core.severity;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repeat offender tracking — detects persistent cheaters across sessions.
 *
 * <p>Maintains per-player violation history with timestamps, enabling detection
 * of repeat offenders, toggle patterns (cheat on/off), and escalation trends.
 * History persists to flat-file properties in {@code plugins/Windfall/violation-history/}.
 *
 * <p>Detection signals:
 * <ul>
 *   <li><b>Repeat offender</b>: same player flagged >= N times across sessions</li>
 *   <li><b>Toggle pattern</b>: violations alternating with clean periods (detects "smart" cheaters)</li>
 *   <li><b>Escalation trend</b>: VL peaks increasing over time (getting bolder)</li>
 *   <li><b>Persistent low-level</b>: consistently low VL but never fully clean</li>
 * </ul>
 *
 * <p>Thread-safe: per-player maps are ConcurrentHashMap, file I/O is synchronized.
 *
 * @see io.windfall.anticheat.core.check.CheckManager for integration point
 */
public class ViolationPattern {

    private static final int DEFAULT_HISTORY_DAYS = 30;
    private static final int DEFAULT_REPEAT_THRESHOLD = 3;
    private static final int DEFAULT_TOGGLE_WINDOW = 6000;

    private final Map<UUID, PlayerHistory> historyMap = new ConcurrentHashMap<>();
    private final Path historyDir;
    private final Logger logger;

    private volatile boolean enabled;
    private volatile int historyDays;
    private volatile int repeatThreshold;
    private volatile int toggleDetectionWindow;

    /**
     * Creates a ViolationPattern manager with file persistence.
     */
    public ViolationPattern(Path dataDir, Logger logger) {
        this.historyDir = dataDir.resolve("violation-history");
        this.logger = logger;
        this.enabled = true;
        this.historyDays = DEFAULT_HISTORY_DAYS;
        this.repeatThreshold = DEFAULT_REPEAT_THRESHOLD;
        this.toggleDetectionWindow = DEFAULT_TOGGLE_WINDOW;

        try {
            Files.createDirectories(historyDir);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create violation-history directory", e);
        }
    }

    /**
     * Package-private constructor for testing (no file I/O).
     */
    ViolationPattern(int historyDays, int repeatThreshold, int toggleDetectionWindow) {
        this.historyDir = null;
        this.logger = Logger.getLogger("ViolationPatternTest");
        this.enabled = true;
        this.historyDays = historyDays;
        this.repeatThreshold = repeatThreshold;
        this.toggleDetectionWindow = toggleDetectionWindow;
    }

    /**
     * Reloads config values. Call on plugin reload.
     */
    public void loadConfig(boolean enabled, int historyDays, int repeatThreshold, int toggleDetectionWindow) {
        this.enabled = enabled;
        this.historyDays = historyDays;
        this.repeatThreshold = repeatThreshold;
        this.toggleDetectionWindow = toggleDetectionWindow;
    }

    /**
     * Records a violation event for the given player.
     */
    public void recordViolation(UUID playerUuid, String checkName, int vl, long tick) {
        if (!enabled) return;

        PlayerHistory history = historyMap.computeIfAbsent(playerUuid, k -> new PlayerHistory());
        history.violations.add(new ViolationEntry(checkName, vl, tick, System.currentTimeMillis()));

        // Prune old entries
        long cutoffMs = System.currentTimeMillis() - (long) historyDays * 24 * 60 * 60 * 1000;
        history.violations.removeIf(e -> e.timestampMs < cutoffMs);
    }

    /**
     * Records a clean session marker.
     */
    public void recordCleanSession(UUID playerUuid, long tick) {
        if (!enabled) return;

        PlayerHistory history = historyMap.computeIfAbsent(playerUuid, k -> new PlayerHistory());
        history.cleanSessions.add(new CleanSession(tick, System.currentTimeMillis()));
    }

    /**
     * Analyzes a player's violation history and returns a pattern assessment.
     */
    public PatternAssessment analyze(UUID playerUuid) {
        if (!enabled) return PatternAssessment.NONE;

        PlayerHistory history = historyMap.get(playerUuid);
        if (history == null || history.violations.isEmpty()) {
            return PatternAssessment.NONE;
        }

        // Count unique session violations (group by day)
        Set<String> violationDays = new HashSet<>();
        for (ViolationEntry entry : history.violations) {
            long dayMs = entry.timestampMs / (24 * 60 * 60 * 1000);
            violationDays.add(String.valueOf(dayMs));
        }

        // Repeat offender: violations on N+ distinct days
        if (violationDays.size() >= repeatThreshold) {
            return new PatternAssessment(PatternType.REPEAT_OFFENDER, violationDays.size(),
                "Violated on " + violationDays.size() + " distinct days");
        }

        // Toggle pattern: alternating violation/clean sessions within window
        if (history.violations.size() >= 4 && history.cleanSessions.size() >= 2) {
            List<ViolationEntry> recent = getRecentViolations(history, toggleDetectionWindow);
            List<CleanSession> recentClean = getRecentCleanSessions(history, toggleDetectionWindow);

            if (!recent.isEmpty() && !recentClean.isEmpty()) {
                long firstViolation = recent.get(0).tick;
                long lastViolation = recent.get(recent.size() - 1).tick;
                long firstClean = recentClean.get(0).tick;

                if (firstClean > firstViolation && firstClean < lastViolation) {
                    return new PatternAssessment(PatternType.TOGGLE_PATTERN, recent.size(),
                        "Alternating violation/clean within " + toggleDetectionWindow + " ticks");
                }
            }
        }

        // Escalation: VL peaks increasing
        if (history.violations.size() >= 6) {
            List<ViolationEntry> sorted = new ArrayList<>(history.violations);
            sorted.sort(Comparator.comparingLong(e -> e.tick));

            int mid = sorted.size() / 2;
            int firstHalfPeak = sorted.subList(0, mid).stream().mapToInt(e -> e.vl).max().orElse(0);
            int secondHalfPeak = sorted.subList(mid, sorted.size()).stream().mapToInt(e -> e.vl).max().orElse(0);

            if (secondHalfPeak > firstHalfPeak && secondHalfPeak >= firstHalfPeak + 5) {
                return new PatternAssessment(PatternType.ESCALATION, history.violations.size(),
                    "VL peak increased from " + firstHalfPeak + " to " + secondHalfPeak);
            }
        }

        return PatternAssessment.NONE;
    }

    /**
     * Returns the recommended graduated response for a given pattern.
     */
    public String getRecommendedAction(PatternAssessment assessment) {
        if (assessment == null || assessment.type == PatternType.NONE) {
            return "standard";
        }

        switch (assessment.type) {
            case REPEAT_OFFENDER:
                return "escalate";
            case TOGGLE_PATTERN:
                return "investigate";
            case ESCALATION:
                return "immediate-action";
            case PERSISTENT_LOW:
                return "monitor";
            default:
                return "standard";
        }
    }

    private List<ViolationEntry> getRecentViolations(PlayerHistory history, int tickWindow) {
        if (history.violations.isEmpty()) return Collections.emptyList();
        long lastTick = history.violations.get(history.violations.size() - 1).tick;
        long cutoff = lastTick - tickWindow;

        List<ViolationEntry> result = new ArrayList<>();
        for (ViolationEntry entry : history.violations) {
            if (entry.tick >= cutoff) {
                result.add(entry);
            }
        }
        return result;
    }

    private List<CleanSession> getRecentCleanSessions(PlayerHistory history, int tickWindow) {
        if (history.cleanSessions.isEmpty()) return Collections.emptyList();
        long lastTick = history.cleanSessions.get(history.cleanSessions.size() - 1).tick;
        long cutoff = lastTick - tickWindow;

        List<CleanSession> result = new ArrayList<>();
        for (CleanSession session : history.cleanSessions) {
            if (session.tick >= cutoff) {
                result.add(session);
            }
        }
        return result;
    }

    // === Persistence using Properties (zero external deps) ===

    /**
     * Saves a player's history to disk. Uses Properties format for zero-dependency persistence.
     */
    public void savePlayerHistory(UUID playerUuid) {
        if (historyDir == null) return;

        PlayerHistory history = historyMap.get(playerUuid);
        if (history == null) return;

        Path file = historyDir.resolve(playerUuid.toString() + ".properties");
        Properties props = new Properties();
        props.setProperty("violation.count", String.valueOf(history.violations.size()));
        for (int i = 0; i < history.violations.size(); i++) {
            ViolationEntry e = history.violations.get(i);
            props.setProperty("v" + i + ".check", e.checkName);
            props.setProperty("v" + i + ".vl", String.valueOf(e.vl));
            props.setProperty("v" + i + ".tick", String.valueOf(e.tick));
            props.setProperty("v" + i + ".ts", String.valueOf(e.timestampMs));
        }
        props.setProperty("clean.count", String.valueOf(history.cleanSessions.size()));
        for (int i = 0; i < history.cleanSessions.size(); i++) {
            CleanSession s = history.cleanSessions.get(i);
            props.setProperty("c" + i + ".tick", String.valueOf(s.tick));
            props.setProperty("c" + i + ".ts", String.valueOf(s.timestampMs));
        }

        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "Windfall Violation History");
        } catch (IOException e) {
            logger.log(Level.FINE, "Failed to save violation history for " + playerUuid, e);
        }
    }

    /**
     * Loads a player's history from disk.
     */
    public void loadPlayerHistory(UUID playerUuid) {
        if (historyDir == null) return;

        Path file = historyDir.resolve(playerUuid.toString() + ".properties");
        if (!Files.exists(file)) return;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            logger.log(Level.FINE, "Failed to load violation history for " + playerUuid, e);
            return;
        }

        PlayerHistory history = new PlayerHistory();
        int vCount = Integer.parseInt(props.getProperty("violation.count", "0"));
        for (int i = 0; i < vCount; i++) {
            String check = props.getProperty("v" + i + ".check", "");
            int vl = Integer.parseInt(props.getProperty("v" + i + ".vl", "0"));
            long tick = Long.parseLong(props.getProperty("v" + i + ".tick", "0"));
            long ts = Long.parseLong(props.getProperty("v" + i + ".ts", "0"));
            history.violations.add(new ViolationEntry(check, vl, tick, ts));
        }
        int cCount = Integer.parseInt(props.getProperty("clean.count", "0"));
        for (int i = 0; i < cCount; i++) {
            long tick = Long.parseLong(props.getProperty("c" + i + ".tick", "0"));
            long ts = Long.parseLong(props.getProperty("c" + i + ".ts", "0"));
            history.cleanSessions.add(new CleanSession(tick, ts));
        }

        historyMap.put(playerUuid, history);
    }

    /**
     * Removes a player's history from memory and disk.
     */
    public void clearPlayerHistory(UUID playerUuid) {
        historyMap.remove(playerUuid);

        if (historyDir != null) {
            Path file = historyDir.resolve(playerUuid.toString() + ".properties");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                logger.log(Level.FINE, "Failed to delete violation history for " + playerUuid, e);
            }
        }
    }

    /**
     * Prunes history files older than historyDays.
     */
    public void pruneOldHistories() {
        if (historyDir == null || !Files.exists(historyDir)) return;

        long cutoffMs = System.currentTimeMillis() - (long) historyDays * 24 * 60 * 60 * 1000;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(historyDir, "*.properties")) {
            for (Path file : stream) {
                if (Files.getLastModifiedTime(file).toMillis() < cutoffMs) {
                    Files.deleteIfExists(file);
                }
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Failed to prune old violation histories", e);
        }
    }

    /** Returns the number of players with stored history. */
    public int getTrackedPlayerCount() {
        return historyMap.size();
    }

    /** Returns true if pattern detection is enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    // === Internal data classes ===

    static class PlayerHistory {
        List<ViolationEntry> violations = new ArrayList<>();
        List<CleanSession> cleanSessions = new ArrayList<>();
    }

    static class ViolationEntry {
        String checkName;
        int vl;
        long tick;
        long timestampMs;

        ViolationEntry(String checkName, int vl, long tick, long timestampMs) {
            this.checkName = checkName;
            this.vl = vl;
            this.tick = tick;
            this.timestampMs = timestampMs;
        }
    }

    static class CleanSession {
        long tick;
        long timestampMs;

        CleanSession(long tick, long timestampMs) {
            this.tick = tick;
            this.timestampMs = timestampMs;
        }
    }

    public enum PatternType {
        NONE, REPEAT_OFFENDER, TOGGLE_PATTERN, ESCALATION, PERSISTENT_LOW
    }

    public static class PatternAssessment {
        public static final PatternAssessment NONE = new PatternAssessment(PatternType.NONE, 0, "No pattern detected");

        public final PatternType type;
        public final int signalStrength;
        public final String description;

        PatternAssessment(PatternType type, int signalStrength, String description) {
            this.type = type;
            this.signalStrength = signalStrength;
            this.description = description;
        }
    }
}
