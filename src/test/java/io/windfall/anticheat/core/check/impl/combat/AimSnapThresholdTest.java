package io.windfall.anticheat.core.check.impl.combat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the instant-snap arithmetic in {@link AimCheck}.
 *
 * <p>Yaw deltas are normalized into [-180, 180) before the comparison, so the previous threshold
 * of 180 degrees could never be exceeded and the yaw snap branch was dead code.
 */
class AimSnapThresholdTest {

    @Test
    void threshold_isReachableForNormalizedYaw() throws Exception {
        double yawThreshold = yawSnapThreshold();
        double pitchThreshold = pitchSnapThreshold();

        assertTrue(yawThreshold < 180.0,
            "Threshold must sit below the 180 degree normalization ceiling or yaw snaps are unreachable");
        assertTrue(yawThreshold >= 60.0,
            "Threshold should stay high enough that ordinary flicking is not flagged");
        assertTrue(pitchThreshold > 0.0 && pitchThreshold < 180.0);
    }

    @Test
    void largeYawDelta_isASnap() {
        assertTrue(AimCheck.isInstantSnap(120.0, 0.0),
            "A 120 degree single-packet yaw snap must register");
    }

    @Test
    void ordinaryFlick_isNotASnap() {
        assertFalse(AimCheck.isInstantSnap(25.0, 3.0),
            "Normal mouse movement must not register as a snap");
    }

    @Test
    void largePitchDelta_isASnap() throws Exception {
        assertTrue(AimCheck.isInstantSnap(0.0, pitchSnapThreshold() + 15.0),
            "A pitch delta beyond its threshold must register");
        assertFalse(AimCheck.isInstantSnap(0.0, 5.0),
            "Small pitch corrections must not register");
    }

    @Test
    void exactThreshold_isNotASnap() throws Exception {
        assertFalse(AimCheck.isInstantSnap(yawSnapThreshold(), pitchSnapThreshold()),
            "Comparison is strict, so the threshold value itself is not a violation");
    }

    @Test
    void normalizedYaw_staysWithinCeiling() {
        for (int i = -720; i <= 720; i += 7) {
            float normalized = AimCheck.normalizeYawDelta(i);
            assertTrue(Math.abs(normalized) <= 180.0f,
                "Normalized yaw delta out of range for input " + i);
        }
    }

    @Test
    void wrapAroundYaw_isSmallNotFullRotation() {
        // 359 -> 1 is a 2 degree turn, not a 358 degree whip
        float delta = AimCheck.normalizeYawDelta(1.0f - 359.0f);
        assertEquals(2.0f, delta, 1e-4f);
        assertFalse(AimCheck.isInstantSnap(Math.abs(delta), 0.0));
    }

    @Test
    void halfTurnRotation_staysAtCeilingNotUndetectable() {
        // +180 is folded to -180 so the range is half-open; both are equally extreme
        float delta = AimCheck.normalizeYawDelta(540.0f);
        assertEquals(-180.0f, delta, 1e-4f);
        assertTrue(AimCheck.isInstantSnap(Math.abs(delta), 0.0),
            "A half turn in one packet must remain detectable");
    }

    private double yawSnapThreshold() throws Exception {
        return staticDouble("INSTANT_SNAP_THRESHOLD");
    }

    private double pitchSnapThreshold() throws Exception {
        return staticDouble("INSTANT_SNAP_PITCH_THRESHOLD");
    }

    private double staticDouble(String name) throws Exception {
        Field field = AimCheck.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Double) field.get(null);
    }
}