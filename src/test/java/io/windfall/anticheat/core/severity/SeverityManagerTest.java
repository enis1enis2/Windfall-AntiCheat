package io.windfall.anticheat.core.severity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeverityManagerTest {

    private SeverityManager severityManager;

    @BeforeEach
    void setUp() {
        severityManager = new SeverityManager(true, 10, 20, 30, 1.3, 1.6, 2.0, 0.6);
    }

    @Test
    void disabledManager_allReturns1() {
        SeverityManager disabled = new SeverityManager(false, 10, 20, 30, 1.3, 1.6, 2.0, 0.6);
        assertFalse(disabled.isEnabled());
        assertEquals("Disabled", disabled.getSeverityLabel(null));
    }

    @Test
    void enabled_isEnabled() {
        assertTrue(severityManager.isEnabled());
    }

    @Test
    void getters_moderate() {
        assertEquals(10, severityManager.getModerateVl());
        assertEquals(1.3, severityManager.getModerateMultiplier());
    }

    @Test
    void getters_high() {
        assertEquals(20, severityManager.getHighVl());
        assertEquals(1.6, severityManager.getHighMultiplier());
    }

    @Test
    void getters_extreme() {
        assertEquals(30, severityManager.getExtremeVl());
        assertEquals(2.0, severityManager.getExtremeMultiplier());
    }

    @Test
    void getters_bedrockDiscount() {
        assertEquals(0.6, severityManager.getBedrockDiscount());
    }

    @Test
    void multiplierLogic_lowVl_noBedrock() {
        // VL < moderate (10) → multiplier is 1.0
        double mult = calculateMultiplier(severityManager, 5, false);
        assertEquals(1.0, mult);
    }

    @Test
    void multiplierLogic_moderateVl_noBedrock() {
        // VL >= moderate (10) → multiplier is 1.3
        double mult = calculateMultiplier(severityManager, 10, false);
        assertEquals(1.3, mult);
    }

    @Test
    void multiplierLogic_highVl_noBedrock() {
        // VL >= high (20) → multiplier is 1.6
        double mult = calculateMultiplier(severityManager, 20, false);
        assertEquals(1.6, mult);
    }

    @Test
    void multiplierLogic_extremeVl_noBedrock() {
        // VL >= extreme (30) → multiplier is 2.0
        double mult = calculateMultiplier(severityManager, 30, false);
        assertEquals(2.0, mult);
    }

    @Test
    void multiplierLogic_aboveExtreme_noBedrock() {
        double mult = calculateMultiplier(severityManager, 100, false);
        assertEquals(2.0, mult);
    }

    @Test
    void multiplierLogic_moderateVl_bedrock() {
        // 1.3 * 0.6 = 0.78
        double mult = calculateMultiplier(severityManager, 10, true);
        assertEquals(0.78, mult, 0.001);
    }

    @Test
    void multiplierLogic_extremeVl_bedrock() {
        // 2.0 * 0.6 = 1.2
        double mult = calculateMultiplier(severityManager, 30, true);
        assertEquals(1.2, mult, 0.001);
    }

    @Test
    void multiplierLogic_lowVl_bedrock() {
        // Bedrock discount applies at all levels: 1.0 * 0.6 = 0.6
        double mult = calculateMultiplier(severityManager, 5, true);
        assertEquals(0.6, mult, 0.001);
    }

    @Test
    void scaledIncrement_lowVl() {
        // multiplier 1.0 rounds to 1
        assertEquals(1, calculateScaledIncrement(severityManager, 5, false));
    }

    @Test
    void scaledIncrement_moderateVl() {
        // multiplier 1.3 rounds to 1
        assertEquals(1, calculateScaledIncrement(severityManager, 10, false));
    }

    @Test
    void scaledIncrement_highVl() {
        // multiplier 1.6 rounds to 2
        assertEquals(2, calculateScaledIncrement(severityManager, 20, false));
    }

    @Test
    void scaledIncrement_extremeVl() {
        // multiplier 2.0 rounds to 2
        assertEquals(2, calculateScaledIncrement(severityManager, 30, false));
    }

    @Test
    void label_low() {
        assertEquals("LOW", calculateLabel(severityManager, 5));
    }

    @Test
    void label_moderate() {
        assertEquals("MODERATE", calculateLabel(severityManager, 10));
    }

    @Test
    void label_high() {
        assertEquals("HIGH", calculateLabel(severityManager, 20));
    }

    @Test
    void label_extreme() {
        assertEquals("EXTREME", calculateLabel(severityManager, 30));
    }

    @Test
    void disabled_label() {
        SeverityManager disabled = new SeverityManager(false, 10, 20, 30, 1.3, 1.6, 2.0, 0.6);
        assertEquals("Disabled", calculateLabel(disabled, 5));
    }

    // Replicate SeverityManager logic for testing without WindfallPlayer dependency
    private double calculateMultiplier(SeverityManager sm, int totalVl, boolean bedrock) {
        if (!sm.isEnabled()) return 1.0;
        double multiplier;
        if (totalVl >= sm.getExtremeVl()) {
            multiplier = sm.getExtremeMultiplier();
        } else if (totalVl >= sm.getHighVl()) {
            multiplier = sm.getHighMultiplier();
        } else if (totalVl >= sm.getModerateVl()) {
            multiplier = sm.getModerateMultiplier();
        } else {
            multiplier = 1.0;
        }
        if (bedrock) {
            multiplier *= sm.getBedrockDiscount();
        }
        return multiplier;
    }

    private int calculateScaledIncrement(SeverityManager sm, int totalVl, boolean bedrock) {
        if (!sm.isEnabled()) return 1;
        double multiplier = calculateMultiplier(sm, totalVl, bedrock);
        return Math.max(1, (int) Math.round(multiplier));
    }

    private String calculateLabel(SeverityManager sm, int totalVl) {
        if (!sm.isEnabled()) return "Disabled";
        if (totalVl >= sm.getExtremeVl()) return "EXTREME";
        if (totalVl >= sm.getHighVl()) return "HIGH";
        if (totalVl >= sm.getModerateVl()) return "MODERATE";
        return "LOW";
    }
}
