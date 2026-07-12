package io.windfall.anticheat.core.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaterialUtilsTest {

    private static Material mat(String name) {
        Material m = Material.matchMaterial(name);
        Assumptions.assumeTrue(m != null, "Material not found on this server version: " + name);
        return m;
    }

    // === isSlime ===

    @Test
    void isSlime_slimeBlock_returnsTrue() {
        assertTrue(MaterialUtils.isSlime(mat("SLIME_BLOCK")));
    }

    @Test
    void isSlime_honeyBlock_returnsFalse() {
        assertFalse(MaterialUtils.isSlime(mat("HONEY_BLOCK")));
    }

    // === isHoney ===

    @Test
    void isHoney_honeyBlock_returnsTrue() {
        assertTrue(MaterialUtils.isHoney(mat("HONEY_BLOCK")));
    }

    @Test
    void isHoney_slimeBlock_returnsFalse() {
        assertFalse(MaterialUtils.isHoney(mat("SLIME_BLOCK")));
    }

    // === isClimbable ===

    @Test
    void isClimbable_ladder_returnsTrue() {
        assertTrue(MaterialUtils.isClimbable(mat("LADDER")));
    }

    @Test
    void isClimbable_vine_returnsTrue() {
        assertTrue(MaterialUtils.isClimbable(mat("VINE")));
    }

    @Test
    void isClimbable_soulSand_returnsTrue() {
        assertTrue(MaterialUtils.isClimbable(mat("SOUL_SAND")));
    }

    @Test
    void isClimbable_soulSoil_returnsTrue() {
        assertTrue(MaterialUtils.isClimbable(mat("SOUL_SOIL")));
    }

    @Test
    void isClimbable_kelp_returnsTrue() {
        assertTrue(MaterialUtils.isClimbable(mat("KELP")));
    }

    @Test
    void isClimbable_stone_returnsFalse() {
        assertFalse(MaterialUtils.isClimbable(mat("STONE")));
    }

    @Test
    void isClimbable_dirt_returnsFalse() {
        assertFalse(MaterialUtils.isClimbable(mat("DIRT")));
    }

    // === isFluid ===

    @Test
    void isFluid_water_returnsTrue() {
        assertTrue(MaterialUtils.isFluid(mat("WATER")));
    }

    @Test
    void isFluid_lava_returnsTrue() {
        assertTrue(MaterialUtils.isFluid(mat("LAVA")));
    }

    @Test
    void isFluid_stone_returnsFalse() {
        assertFalse(MaterialUtils.isFluid(mat("STONE")));
    }

    // === isIce ===

    @Test
    void isIce_ice_returnsTrue() {
        assertTrue(MaterialUtils.isIce(mat("ICE")));
    }

    @Test
    void isIce_packedIce_returnsTrue() {
        assertTrue(MaterialUtils.isIce(mat("PACKED_ICE")));
    }

    @Test
    void isIce_blueIce_returnsTrue() {
        assertTrue(MaterialUtils.isIce(mat("BLUE_ICE")));
    }

    // === isWeb ===

    @Test
    void isWeb_cobweb_returnsTrue() {
        assertTrue(MaterialUtils.isWeb(mat("COBWEB")));
    }

    // === clearCaches ===

    @Test
    void clearCaches_doesNotThrow() {
        assertDoesNotThrow(MaterialUtils::clearCaches);
    }

    // === null safety ===

    @Test
    void isSlime_null_returnsFalse() {
        assertFalse(MaterialUtils.isSlime(null));
    }

    @Test
    void isHoney_null_returnsFalse() {
        assertFalse(MaterialUtils.isHoney(null));
    }

    @Test
    void isClimbable_null_returnsFalse() {
        assertFalse(MaterialUtils.isClimbable(null));
    }

    @Test
    void isFluid_null_returnsFalse() {
        assertFalse(MaterialUtils.isFluid(null));
    }

    @Test
    void isIce_null_returnsFalse() {
        assertFalse(MaterialUtils.isIce(null));
    }

    @Test
    void isWeb_null_returnsFalse() {
        assertFalse(MaterialUtils.isWeb(null));
    }
}
