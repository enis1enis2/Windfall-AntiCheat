package io.windfall.anticheat.compat.elytra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireworkBoostModelTest {

    @Test
    void legitimateGlowstoneBoostIsCredited() {
        FireworkBoostModel model = new FireworkBoostModel();
        model.onFireworkInteraction(0, 1.0D, true);
        assertTrue(model.isBoostActive());

        double[] speeds = {1.25D, 1.40D, 1.50D, 1.55D, 1.60D, 1.62D};
        for (int i = 0; i < speeds.length; i++) {
            model.update(i + 1, speeds[i], speeds[i] - (i == 0 ? 1.0D : speeds[i - 1]));
        }

        model.update(7, 1.63D, 0.01D);

        assertTrue(model.wasLastBoostLegitimate());
        assertEquals(1, model.getLegitimateBoostCount());
        assertEquals(0, model.getSuspiciousBoostCount());
        assertEquals(0.0D, model.getSuspicionScore(), 1.0E-9D);
        assertFalse(model.isFakeFirework());
        assertTrue(model.isInGracePeriod());
    }

    @Test
    void fakeFireworkWithoutAccelerationIsSuspicious() {
        FireworkBoostModel model = new FireworkBoostModel();
        model.onFireworkInteraction(0, 1.0D, true);

        for (int i = 1; i <= 8; i++) {
            model.update(i, 1.0D, 0.0D);
        }

        assertFalse(model.wasLastBoostLegitimate());
        assertEquals(1, model.getSuspiciousBoostCount());
        assertTrue(model.isFakeFirework());
        assertTrue(model.getSuspicionScore() > 0.0D);
    }

    @Test
    void fireworkInsideGraceStillExempts() {
        FireworkBoostModel model = new FireworkBoostModel();
        model.onFireworkInteraction(0, 1.0D, true);

        double[] speeds = {1.25D, 1.40D, 1.50D, 1.55D, 1.60D, 1.62D};
        for (int i = 0; i < speeds.length; i++) {
            model.update(i + 1, speeds[i], speeds[i] - (i == 0 ? 1.0D : speeds[i - 1]));
        }
        model.update(7, 1.63D, 0.01D);

        assertFalse(model.isBoostActive());
        assertTrue(model.isInBoostOrGrace());
        assertTrue(model.update(8, 1.63D, 0.0D), "grace must keep returning exempt");
    }

    @Test
    void graceExpiresAfterPostBoostGraceTicks() {
        FireworkBoostModel model = new FireworkBoostModel();
        model.onFireworkInteraction(0, 1.0D, true);

        double[] speeds = {1.25D, 1.40D, 1.50D, 1.55D, 1.60D, 1.62D};
        for (int i = 0; i < speeds.length; i++) {
            model.update(i + 1, speeds[i], speeds[i] - (i == 0 ? 1.0D : speeds[i - 1]));
        }
        model.update(7, 1.63D, 0.01D);

        for (int i = 8; i <= 17; i++) {
            model.update(i, 1.63D, 0.0D);
        }

        assertFalse(model.isInBoostOrGrace());
        assertFalse(model.isInGracePeriod());
    }

    @Test
    void interactionWhileNotGlidingIsRejected() {
        FireworkBoostModel model = new FireworkBoostModel();
        model.onFireworkInteraction(0, 1.0D, false);

        assertFalse(model.isBoostActive());
        assertEquals("Not gliding", model.getLastBoostFailReason());
        assertFalse(model.update(1, 1.0D, 0.0D), "no boost may be processed while not gliding");
    }

    @Test
    void boostLongerThanMaximumIsEnded() {
        FireworkBoostModel model = new FireworkBoostModel();
        model.onFireworkInteraction(0, 1.0D, true);

        boolean stillActive = true;
        for (int i = 1; i <= 50 && stillActive; i++) {
            stillActive = model.update(i, 1.001D, 0.02D);
        }

        assertFalse(model.isBoostActive());
        assertEquals("Duration exceeded maximum", model.getLastBoostFailReason());
    }
}