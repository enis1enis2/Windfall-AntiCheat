package io.windfall.anticheat.core.check;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CompatFlagTest {

    @Test
    void enumHasCorrectNumberOfValues() {
        assertEquals(13, CompatFlag.values().length);
    }

    @Test
    void versionFlagsExist() {
        assertNotNull(CompatFlag.VERSION_LEGACY);
        assertNotNull(CompatFlag.VERSION_COMBAT);
        assertNotNull(CompatFlag.VERSION_FLAT);
        assertNotNull(CompatFlag.VERSION_WORLD);
        assertNotNull(CompatFlag.VERSION_MODERN);
        assertNotNull(CompatFlag.VERSION_LATEST);
    }

    @Test
    void loaderFlagsExist() {
        assertNotNull(CompatFlag.FOLIA_UNSAFE);
        assertNotNull(CompatFlag.PURPUR_KB_DEPENDENT);
        assertNotNull(CompatFlag.PAPER_CHUNK_DEPENDENT);
    }

    @Test
    void pluginFlagsExist() {
        assertNotNull(CompatFlag.VIAVERSION_SENSITIVE);
        assertNotNull(CompatFlag.GEYSEIR_SENSITIVE);
    }

    @Test
    void mismatchFlagsExist() {
        assertNotNull(CompatFlag.DISABLE_ON_MISMATCH);
        assertNotNull(CompatFlag.RELAX_ON_MISMATCH);
    }

    @Test
    void allFlagValuesAreUnique() {
        Set<String> names = Arrays.stream(CompatFlag.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(CompatFlag.values().length, names.size(),
                "Duplicate enum constant names detected");
    }

    @Test
    void nameReturnsNonNullNonEmptyStrings() {
        for (CompatFlag flag : CompatFlag.values()) {
            assertNotNull(flag.name(), "name() returned null for " + flag);
            assertFalse(flag.name().isEmpty(), "name() returned empty string for " + flag);
        }
    }
}
