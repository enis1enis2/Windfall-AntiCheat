package io.windfall.anticheat.core.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PurpurCompatTest {

    private PurpurCompat purpurCompat;

    @BeforeEach
    void setUp() {
        purpurCompat = new PurpurCompat();
    }

    @Test
    void isCustomKnockbackDisabledInTestEnv() {
        assertFalse(purpurCompat.isCustomKnockbackEnabled());
    }

    @Test
    void isPurpurFalseByDefault() {
        assertFalse(purpurCompat.isPurpur());
    }

    @Test
    void adjustHorizontalKBPassesThroughWhenDisabled() {
        assertEquals(1.0, purpurCompat.adjustHorizontalKB(1.0));
    }

    @Test
    void adjustHorizontalKBPassesThroughZeroPointFive() {
        assertEquals(0.5, purpurCompat.adjustHorizontalKB(0.5));
    }

    @Test
    void adjustHorizontalKBPassesThroughNegative() {
        assertEquals(-1.0, purpurCompat.adjustHorizontalKB(-1.0));
    }

    @Test
    void adjustVerticalKBPassesThroughWhenDisabled() {
        assertEquals(0.8, purpurCompat.adjustVerticalKB(0.8));
    }

    @Test
    void adjustVerticalKBPassesThroughOne() {
        assertEquals(1.0, purpurCompat.adjustVerticalKB(1.0));
    }

    @Test
    void adjustHorizontalKBDoesNotThrowWithZero() {
        assertDoesNotThrow(() -> purpurCompat.adjustHorizontalKB(0.0));
    }

    @Test
    void adjustHorizontalKBDoesNotThrowWithNegative() {
        assertDoesNotThrow(() -> purpurCompat.adjustHorizontalKB(-1.0));
    }

    @Test
    void adjustHorizontalKBDoesNotThrowWithMaxValue() {
        assertDoesNotThrow(() -> purpurCompat.adjustHorizontalKB(Double.MAX_VALUE));
    }

    @Test
    void adjustVerticalKBDoesNotThrowWithZero() {
        assertDoesNotThrow(() -> purpurCompat.adjustVerticalKB(0.0));
    }

    @Test
    void adjustVerticalKBDoesNotThrowWithMaxValue() {
        assertDoesNotThrow(() -> purpurCompat.adjustVerticalKB(Double.MAX_VALUE));
    }

    @Test
    void adjustHorizontalKBReturnsExactValueWhenDisabled() {
        double input = 0.5;
        assertEquals(input, purpurCompat.adjustHorizontalKB(input));
    }

    @Test
    void adjustVerticalKBReturnsExactValueWhenDisabled() {
        double input = 0.8;
        assertEquals(input, purpurCompat.adjustVerticalKB(input));
    }

    @Test
    void defaultMultipliersAreOne() {
        assertEquals(1.0, purpurCompat.getAttackKnockbackMultiplier());
        assertEquals(1.0, purpurCompat.getKnockbackVerticalMultiplier());
    }

    @Test
    void initWithNonPurpurForkDisablesCustomKB() {
        purpurCompat.init(io.windfall.anticheat.core.version.ServerFork.BUKKIT, java.util.logging.Logger.getLogger("test"));
        assertFalse(purpurCompat.isCustomKnockbackEnabled());
    }

    @Test
    void adjustReturnsUnchangedAfterNonPurpurInit() {
        purpurCompat.init(io.windfall.anticheat.core.version.ServerFork.BUKKIT, java.util.logging.Logger.getLogger("test"));
        assertEquals(1.0, purpurCompat.adjustHorizontalKB(1.0));
        assertEquals(0.5, purpurCompat.adjustHorizontalKB(0.5));
        assertEquals(0.8, purpurCompat.adjustVerticalKB(0.8));
    }

    @Test
    void toStringReturnsNonNull() {
        assertNotNull(purpurCompat.toString());
    }

    @Test
    void toStringReturnsNonEmpty() {
        assertFalse(purpurCompat.toString().isEmpty());
    }
}
