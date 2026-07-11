package io.windfall.anticheat.core.check;

import io.windfall.anticheat.core.check.impl.combat.AutoclickerCheck;
import io.windfall.anticheat.core.check.impl.combat.CriticalsCheck;
import io.windfall.anticheat.core.check.impl.combat.KillAuraCheck;
import io.windfall.anticheat.core.check.impl.movement.SimulationCheck;
import io.windfall.anticheat.core.check.impl.movement.VelocityCheck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckDataCompatTest {

    private CheckData getCheckData(Class<?> checkClass) {
        CheckData data = checkClass.getAnnotation(CheckData.class);
        assertNotNull(data, checkClass.getSimpleName() + " missing @CheckData annotation");
        return data;
    }

    @Test
    void velocityCheckHasRelaxOnMismatchAndRelaxMultiplierAboveOne() {
        CheckData data = getCheckData(VelocityCheck.class);
        assertTrue(ArraysContains(data.compat(), CompatFlag.RELAX_ON_MISMATCH),
                "VelocityCheck should have RELAX_ON_MISMATCH");
        assertTrue(data.relaxMultiplier() > 1.0,
                "VelocityCheck relaxMultiplier should be > 1.0, was " + data.relaxMultiplier());
    }

    @Test
    void simulationCheckHasRelaxOnMismatch() {
        CheckData data = getCheckData(SimulationCheck.class);
        assertTrue(ArraysContains(data.compat(), CompatFlag.RELAX_ON_MISMATCH),
                "SimulationCheck should have RELAX_ON_MISMATCH");
    }

    @Test
    void killAuraCheckHasRelaxOnMismatch() {
        CheckData data = getCheckData(KillAuraCheck.class);
        assertTrue(ArraysContains(data.compat(), CompatFlag.RELAX_ON_MISMATCH),
                "KillAuraCheck should have RELAX_ON_MISMATCH");
    }

    @Test
    void autoclickerCheckRelaxMultiplierAboveOne() {
        CheckData data = getCheckData(AutoclickerCheck.class);
        assertTrue(data.relaxMultiplier() > 1.0,
                "AutoclickerCheck relaxMultiplier should be > 1.0, was " + data.relaxMultiplier());
    }

    @Test
    void checkWithoutCompatFlagsHasDefaultEmptyCompat() {
        CheckData data = getCheckData(CriticalsCheck.class);
        assertEquals(0, data.compat().length,
                "CriticalsCheck should have default empty compat array");
    }

    @Test
    void disableOnFoliaDefaultsToFalse() {
        CheckData data = getCheckData(CriticalsCheck.class);
        assertFalse(data.disableOnFolia(),
                "disableOnFolia should default to false");
    }

    @Test
    void disableOnPurpurDefaultsToFalse() {
        CheckData data = getCheckData(CriticalsCheck.class);
        assertFalse(data.disableOnPurpur(),
                "disableOnPurpur should default to false");
    }

    @Test
    void relaxMultiplierDefaultsToOnePointZero() {
        CheckData data = getCheckData(CriticalsCheck.class);
        assertEquals(1.0, data.relaxMultiplier(), 0.001,
                "relaxMultiplier should default to 1.0");
    }

    private static boolean ArraysContains(CompatFlag[] array, CompatFlag target) {
        for (CompatFlag flag : array) {
            if (flag == target) return true;
        }
        return false;
    }
}
