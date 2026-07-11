package io.windfall.anticheat.core.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginDetectorTest {

    private PluginDetector detector;

    @BeforeEach
    void setUp() {
        detector = new PluginDetector();
    }

    @Test
    void defaultState_allDetectorsReportFalse() {
        assertFalse(detector.isViaVersionInstalled());
        assertFalse(detector.isViaBackwardsInstalled());
        assertFalse(detector.isViaRewindInstalled());
        assertFalse(detector.isGeyserInstalled());
        assertFalse(detector.isOldCombatMechanicsInstalled());
    }

    @Test
    void isAnyViaVersionPlugin_allFalse_returnsFalse() {
        assertFalse(detector.isAnyViaVersionPlugin());
    }

    @Test
    void isAnyViaVersionPlugin_viaVersionOnly_returnsTrue() throws Exception {
        setField("viaVersionInstalled", true);
        assertEquals(true, detector.isAnyViaVersionPlugin());
    }

    @Test
    void isAnyViaVersionPlugin_viaBackwardsOnly_returnsTrue() throws Exception {
        setField("viaBackwardsInstalled", true);
        assertEquals(true, detector.isAnyViaVersionPlugin());
    }

    @Test
    void isAnyViaVersionPlugin_viaRewindOnly_returnsTrue() throws Exception {
        setField("viaRewindInstalled", true);
        assertEquals(true, detector.isAnyViaVersionPlugin());
    }

    @Test
    void isGeyserInstalled_returnsFalseByDefault() {
        assertFalse(detector.isGeyserInstalled());
    }

    @Test
    void isOldCombatMechanicsInstalled_returnsFalseByDefault() {
        assertFalse(detector.isOldCombatMechanicsInstalled());
    }

    @Test
    void isViaBackwardsInstalled_returnsFalseByDefault() {
        assertFalse(detector.isViaBackwardsInstalled());
    }

    @Test
    void isViaRewindInstalled_returnsFalseByDefault() {
        assertFalse(detector.isViaRewindInstalled());
    }

    @Test
    void isViaVersionInstalled_returnsFalseByDefault() {
        assertFalse(detector.isViaVersionInstalled());
    }

    @Test
    void constructor_createsNonNullInstance() {
        assertNotNull(detector);
    }

    @Test
    void isAnyViaVersionPlugin_allViaVariantsTrue_returnsTrue() throws Exception {
        setField("viaVersionInstalled", true);
        setField("viaBackwardsInstalled", true);
        setField("viaRewindInstalled", true);
        assertEquals(true, detector.isAnyViaVersionPlugin());
    }

    @Test
    void isAnyViaVersionPlugin_allFalseWithGeyserTrue_returnsFalse() throws Exception {
        setField("geyserInstalled", true);
        assertFalse(detector.isAnyViaVersionPlugin());
    }

    private void setField(String fieldName, boolean value) throws Exception {
        Field field = PluginDetector.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(detector, value);
    }
}
