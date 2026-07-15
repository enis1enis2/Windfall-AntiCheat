package io.windfall.anticheat.core.adaptive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveThresholdTest {

    private AdaptiveThreshold threshold;

    @BeforeEach
    void setUp() {
        threshold = new AdaptiveThreshold(50);
        threshold.reset();
    }

    @Test
    void testHealthyTpsReturnsMultiplier1() {
        // Push 20 TPS (healthy) for the full window
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(20.0);
        }
        assertEquals(1.0, threshold.getCurrentMultiplier(), 0.001,
            "Healthy TPS (20) should return 1.0 multiplier");
        assertFalse(threshold.isSafeMode());
    }

    @Test
    void testModerateLagScalesTolerance() {
        // Push 15 TPS (moderate lag) for the full window
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(15.0);
        }
        double multiplier = threshold.getCurrentMultiplier();
        // With tpsThreshold=19, scaleFactor=0.02: deficiency=4, multiplier=1+4*0.02=1.08
        assertEquals(1.08, multiplier, 0.001,
            "Moderate lag (15 TPS) should scale tolerance by ~8%");
        assertFalse(threshold.isSafeMode());
    }

    @Test
    void testSevereLagCapsAtMaxMultiplier() {
        // Push 14 TPS (significant lag, above safe-mode threshold of 12)
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(14.0);
        }
        double multiplier = threshold.getCurrentMultiplier();
        // deficiency=5, raw multiplier=1+5*0.02=1.10, below max 2.0
        assertEquals(1.10, multiplier, 0.001,
            "Significant lag (14 TPS) should scale proportionally");
        assertFalse(threshold.isSafeMode());
    }

    @Test
    void testSafeModeOverridesMultiplier() {
        // Push 5 TPS (below safe-mode threshold of 12)
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(5.0);
        }
        assertTrue(threshold.isSafeMode());
        assertEquals(2.0, threshold.getCurrentMultiplier(), 0.001,
            "Safe mode should use max tolerance multiplier");
    }

    @Test
    void testApplyToleranceScalesBaseThreshold() {
        // Push 15 TPS for full window
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(15.0);
        }
        double scaled = threshold.applyTolerance(100.0);
        // 100 * 1.08 = 108
        assertEquals(108.0, scaled, 0.001,
            "applyTolerance should scale base value by current multiplier");
    }

    @Test
    void testDisabledReturnsOriginalValue() {
        threshold.loadConfig(new MockConfig(false, 19.0, 0.02, 2.0, 12.0));
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(5.0);
        }
        assertEquals(1.0, threshold.getCurrentMultiplier(), 0.001,
            "Disabled threshold should always return 1.0 multiplier");
        assertEquals(100.0, threshold.applyTolerance(100.0), 0.001,
            "Disabled threshold should not scale values");
    }

    @Test
    void testRollingWindowAveragesCorrectly() {
        // Push 20 TPS for 25 ticks, then 10 TPS for 25 ticks
        for (int i = 0; i < 25; i++) {
            threshold.pushTps(20.0);
        }
        for (int i = 0; i < 25; i++) {
            threshold.pushTps(10.0);
        }
        // Average should be ~15 TPS
        double avgTps = threshold.getCurrentTps();
        assertEquals(15.0, avgTps, 0.5,
            "Rolling window should average to ~15 TPS");
    }

    @Test
    void testResetClearsAllState() {
        for (int i = 0; i < 50; i++) {
            threshold.pushTps(5.0);
        }
        assertTrue(threshold.isSafeMode());

        threshold.reset();

        assertEquals(20.0, threshold.getCurrentTps(), 0.001);
        assertEquals(1.0, threshold.getCurrentMultiplier(), 0.001);
        assertFalse(threshold.isSafeMode());
    }

    @Test
    void testOnTickPushesTickDurationBasedTps() {
        // 50ms tick = 20 TPS
        for (int i = 0; i < 50; i++) {
            threshold.onTick(50);
        }
        assertEquals(20.0, threshold.getCurrentTps(), 0.5,
            "50ms ticks should result in ~20 TPS average");
        assertEquals(1.0, threshold.getCurrentMultiplier(), 0.001);
    }

    @Test
    void testOnTickWithSlowTickIncreasesMultiplier() {
        // 100ms tick = 10 TPS
        for (int i = 0; i < 50; i++) {
            threshold.onTick(100);
        }
        double multiplier = threshold.getCurrentMultiplier();
        assertTrue(multiplier > 1.0,
            "Slow ticks (100ms) should increase tolerance multiplier");
    }

    // Mock config for testing loadConfig
    private static class MockConfig extends io.windfall.anticheat.core.config.WindfallConfig {
        private final boolean enabled;
        private final double tpsThreshold;
        private final double scaleFactor;
        private final double maxMultiplier;
        private final double safeModeTps;

        MockConfig(boolean enabled, double tpsThreshold, double scaleFactor,
                   double maxMultiplier, double safeModeTps) {
            super(null); // parent not needed for these accessors
            this.enabled = enabled;
            this.tpsThreshold = tpsThreshold;
            this.scaleFactor = scaleFactor;
            this.maxMultiplier = maxMultiplier;
            this.safeModeTps = safeModeTps;
        }

        @Override public boolean isAdaptiveEnabled() { return enabled; }
        @Override public double getAdaptiveTpsThreshold() { return tpsThreshold; }
        @Override public double getAdaptiveScaleFactor() { return scaleFactor; }
        @Override public double getAdaptiveMaxToleranceMultiplier() { return maxMultiplier; }
        @Override public double getAdaptiveSafeModeThreshold() { return safeModeTps; }
    }
}
