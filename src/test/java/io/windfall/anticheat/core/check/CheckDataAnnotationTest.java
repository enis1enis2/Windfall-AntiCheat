package io.windfall.anticheat.core.check;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

class CheckDataAnnotationTest {

    @Test
    void defaultDecay() {
        CheckData data = getAnnotation(FakeCheckA.class);
        assertEquals(0.02, data.decay());
    }

    @Test
    void defaultSetbackVl() {
        CheckData data = getAnnotation(FakeCheckA.class);
        assertEquals(20, data.setbackVl());
    }

    @Test
    void defaultMinVersion() {
        CheckData data = getAnnotation(FakeCheckA.class);
        assertEquals(4, data.minVersion());
    }

    @Test
    void defaultMaxVersion() {
        CheckData data = getAnnotation(FakeCheckA.class);
        assertEquals(99999, data.maxVersion());
    }

    @Test
    void customValues() {
        CheckData data = getAnnotation(FakeCheckB.class);
        assertEquals("Test B", data.name());
        assertEquals("windfall.test.b", data.stableKey());
        assertEquals(0.005, data.decay());
        assertEquals(5, data.setbackVl());
        assertEquals(110, data.minVersion());
        assertEquals(763, data.maxVersion());
    }

    @Test
    void nameRequired() {
        CheckData data = getAnnotation(FakeCheckA.class);
        assertEquals("Test A", data.name());
    }

    @Test
    void stableKeyRequired() {
        CheckData data = getAnnotation(FakeCheckA.class);
        assertEquals("windfall.test.a", data.stableKey());
    }

    @Test
    void versionRange_swordBlock() {
        // SwordBlock should be limited to pre-1.9 (protocol < 110)
        CheckData data = getAnnotation(SwordBlockReference.class);
        assertEquals(4, data.minVersion());
        assertEquals(107, data.maxVersion());
    }

    @Test
    void versionRange_fastHeal() {
        // FastHeal should be limited to 1.8.x only (47-47)
        CheckData data = getAnnotation(FastHealReference.class);
        assertEquals(47, data.minVersion());
        assertEquals(47, data.maxVersion());
    }

    private CheckData getAnnotation(Class<?> clazz) {
        return clazz.getAnnotation(CheckData.class);
    }

    // Fake check classes to test annotation defaults and custom values
    @CheckData(name = "Test A", stableKey = "windfall.test.a")
    static class FakeCheckA {}

    @CheckData(name = "Test B", stableKey = "windfall.test.b",
               decay = 0.005, setbackVl = 5, minVersion = 110, maxVersion = 763)
    static class FakeCheckB {}

    // Simulates SwordBlock's actual annotation
    @CheckData(name = "SwordBlock A", stableKey = "windfall.combat.swordblock",
               decay = 0.015, setbackVl = 10, minVersion = 4, maxVersion = 107)
    static class SwordBlockReference {}

    // Simulates FastHeal's actual annotation
    @CheckData(name = "FastHeal A", stableKey = "windfall.combat.fastheal",
               decay = 0.02, setbackVl = 10, minVersion = 47, maxVersion = 47)
    static class FastHealReference {}
}
