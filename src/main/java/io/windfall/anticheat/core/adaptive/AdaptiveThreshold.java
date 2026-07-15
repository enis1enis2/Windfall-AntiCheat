package io.windfall.anticheat.core.adaptive;

import io.windfall.anticheat.core.config.WindfallConfig;

import java.util.ArrayDeque;
import java.util.logging.Logger;

/**
 * TPS-aware tolerance scaling — the core of server-load adaptation.
 *
 * <p>Monitors server TPS with a rolling window (default 50 ticks = 2.5s).
 * When TPS drops below the configured threshold (default 19), tolerances
 * are scaled up by {@code scaleFactor * deficiency} per tick of deficiency.
 * This prevents false positives during lag spikes while maintaining
 * detection sensitivity at healthy TPS.
 *
 * <p>Thread-safe: all state is either volatile or updated atomically.
 * Designed to be called once per server tick from {@link io.windfall.anticheat.core.check.CheckManager#onTick()}.
 *
 * <p>Example: with {@code tpsThreshold=19, scaleFactor=0.02, maxToleranceMultiplier=2.0}:
 * <ul>
 *   <li>TPS=20 (healthy): multiplier=1.0 (no scaling)</li>
 *   <li>TPS=15 (moderate lag): multiplier=1.10 (10% tolerance increase)</li>
 *   <li>TPS=10 (severe lag): multiplier=1.20 (20% tolerance increase)</li>
 *   <li>TPS=5 (critical lag): multiplier=1.30 (capped at maxToleranceMultiplier if configured)</li>
 * </ul>
 *
 * <p>Config keys:
 * <ul>
 *   <li>{@code adaptive.enabled} — master toggle (default true)</li>
 *   <li>{@code adaptive.tps-threshold} — TPS below which scaling activates (default 19)</li>
 *   <li>{@code adaptive.scale-factor} — percentage tolerance increase per TPS below threshold (default 0.02 = 2%)</li>
 *   <li>{@code adaptive.max-tolerance-multiplier} — hard cap on tolerance scaling (default 2.0)</li>
 *   <li>{@code adaptive.safe-mode-threshold} — TPS below which all tolerance checks are skipped (default 12)</li>
 * </ul>
 *
 * @see io.windfall.anticheat.core.check.CheckManager#onTick() for integration point
 */
public class AdaptiveThreshold {

    private static final int DEFAULT_WINDOW_SIZE = 50;
    private static final double DEFAULT_TPS_THRESHOLD = 19.0;
    private static final double DEFAULT_SCALE_FACTOR = 0.02;
    private static final double DEFAULT_MAX_MULTIPLIER = 2.0;
    private static final double DEFAULT_SAFE_MODE_TPS = 12.0;

    private final ArrayDeque<Double> tpsSamples = new ArrayDeque<>(DEFAULT_WINDOW_SIZE + 1);
    private final int windowSize;

    private volatile boolean enabled;
    private volatile double tpsThreshold;
    private volatile double scaleFactor;
    private volatile double maxToleranceMultiplier;
    private volatile double safeModeTps;

    private volatile double currentTps = 20.0;
    private volatile double currentMultiplier = 1.0;
    private volatile boolean safeMode = false;
    private volatile long lastUpdateTick = 0;

    private static AdaptiveThreshold instance;

    /**
     * Returns the singleton instance, creating one if needed.
     */
    public static synchronized AdaptiveThreshold getInstance() {
        if (instance == null) {
            instance = new AdaptiveThreshold(DEFAULT_WINDOW_SIZE);
        }
        return instance;
    }

    /**
     * Package-private constructor for testing.
     */
    AdaptiveThreshold(int windowSize) {
        this.windowSize = windowSize;
        this.enabled = true;
        this.tpsThreshold = DEFAULT_TPS_THRESHOLD;
        this.scaleFactor = DEFAULT_SCALE_FACTOR;
        this.maxToleranceMultiplier = DEFAULT_MAX_MULTIPLIER;
        this.safeModeTps = DEFAULT_SAFE_MODE_TPS;
    }

    /**
     * Loads configuration from WindfallConfig.
     * Call once on plugin enable and again on reload.
     */
    public void loadConfig(WindfallConfig config) {
        this.enabled = config.isAdaptiveEnabled();
        this.tpsThreshold = config.getAdaptiveTpsThreshold();
        this.scaleFactor = config.getAdaptiveScaleFactor();
        this.maxToleranceMultiplier = config.getAdaptiveMaxToleranceMultiplier();
        this.safeModeTps = config.getAdaptiveSafeModeThreshold();
    }

    /**
     * Called once per server tick. Updates TPS estimate and tolerance multiplier.
     *
     * @param currentTickMs time in milliseconds this tick took (measured by scheduler)
     */
    public void onTick(long currentTickMs) {
        if (!enabled) return;

        double tickTps = 1000.0 / Math.max(currentTickMs, 1);

        synchronized (tpsSamples) {
            if (tpsSamples.size() >= windowSize) {
                tpsSamples.pollFirst();
            }
            tpsSamples.addLast(tickTps);
        }

        recalculate();
    }

    /**
     * Pushes an externally measured TPS value into the rolling window.
     * Use this when the scheduler doesn't expose tick duration directly.
     */
    public void pushTps(double tps) {
        if (!enabled) return;

        synchronized (tpsSamples) {
            if (tpsSamples.size() >= windowSize) {
                tpsSamples.pollFirst();
            }
            tpsSamples.addLast(tps);
        }

        recalculate();
    }

    private void recalculate() {
        double avgTps;
        synchronized (tpsSamples) {
            if (tpsSamples.isEmpty()) {
                avgTps = 20.0;
            } else {
                double sum = 0;
                for (double s : tpsSamples) {
                    sum += s;
                }
                avgTps = sum / tpsSamples.size();
            }
        }

        this.currentTps = avgTps;

        if (avgTps < safeModeTps) {
            this.safeMode = true;
            this.currentMultiplier = maxToleranceMultiplier;
        } else if (avgTps < tpsThreshold) {
            this.safeMode = false;
            double deficiency = tpsThreshold - avgTps;
            double multiplier = 1.0 + (deficiency * scaleFactor);
            this.currentMultiplier = Math.min(multiplier, maxToleranceMultiplier);
        } else {
            this.safeMode = false;
            this.currentMultiplier = 1.0;
        }
    }

    /**
     * Applies the adaptive tolerance multiplier to a base threshold value.
     * Use this in checks instead of the raw threshold.
     *
     * @param baseThreshold the base tolerance/threshold from config
     * @return scaled threshold (baseThreshold * currentMultiplier)
     */
    public double applyTolerance(double baseThreshold) {
        if (!enabled) return baseThreshold;
        return baseThreshold * currentMultiplier;
    }

    /**
     * Returns true if the server is in safe mode (severe lag).
     * Checks should consider skipping non-critical detections in safe mode.
     */
    public boolean isSafeMode() {
        return safeMode;
    }

    /** Returns the current estimated TPS (rolling average). */
    public double getCurrentTps() {
        return currentTps;
    }

    /** Returns the current tolerance multiplier (1.0 = no scaling). */
    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    /** Returns true if adaptive scaling is enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Returns the TPS threshold below which scaling activates. */
    public double getTpsThreshold() {
        return tpsThreshold;
    }

    /** Returns the scale factor (percentage increase per TPS deficiency). */
    public double getScaleFactor() {
        return scaleFactor;
    }

    /** Returns the hard cap on tolerance multiplier. */
    public double getMaxToleranceMultiplier() {
        return maxToleranceMultiplier;
    }

    /** Returns the TPS below which safe mode activates. */
    public double getSafeModeTps() {
        return safeModeTps;
    }

    /** Resets all state (for testing or full reload). */
    public void reset() {
        synchronized (tpsSamples) {
            tpsSamples.clear();
        }
        this.currentTps = 20.0;
        this.currentMultiplier = 1.0;
        this.safeMode = false;
        this.lastUpdateTick = 0;
    }
}
