package io.windfall.anticheat.core.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionBracketTest {

    // ── fromProtocol() known mappings ──────────────────────────────

    @Test
    void fromProtocol_legacy_protocols() {
        assertEquals(VersionBracket.LEGACY, VersionBracket.fromProtocol(4));
        assertEquals(VersionBracket.LEGACY, VersionBracket.fromProtocol(5));
        assertEquals(VersionBracket.LEGACY, VersionBracket.fromProtocol(47));
    }

    @Test
    void fromProtocol_combat_protocols() {
        assertEquals(VersionBracket.COMBAT, VersionBracket.fromProtocol(110));
        assertEquals(VersionBracket.COMBAT, VersionBracket.fromProtocol(210));
        assertEquals(VersionBracket.COMBAT, VersionBracket.fromProtocol(340));
    }

    @Test
    void fromProtocol_flat_protocols() {
        assertEquals(VersionBracket.FLAT, VersionBracket.fromProtocol(404));
        assertEquals(VersionBracket.FLAT, VersionBracket.fromProtocol(477));
    }

    @Test
    void fromProtocol_world_protocols() {
        assertEquals(VersionBracket.WORLD, VersionBracket.fromProtocol(573));
        assertEquals(VersionBracket.WORLD, VersionBracket.fromProtocol(736));
        assertEquals(VersionBracket.WORLD, VersionBracket.fromProtocol(758));
    }

    @Test
    void fromProtocol_modern_protocols() {
        assertEquals(VersionBracket.MODERN, VersionBracket.fromProtocol(760));
        assertEquals(VersionBracket.MODERN, VersionBracket.fromProtocol(766));
    }

    @Test
    void fromProtocol_latest_protocols() {
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(767));
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(769));
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(800));
    }

    // ── fromProtocol() edge cases ──────────────────────────────────

    @Test
    void fromProtocol_zero_fallsBackToLegacy() {
        assertEquals(VersionBracket.LEGACY, VersionBracket.fromProtocol(0));
    }

    @Test
    void fromProtocol_betweenLegacyAndCombat_fallsToLatest() {
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(100));
    }

    @Test
    void fromProtocol_justBeforeFlat_fallsToFlat() {
        assertEquals(VersionBracket.FLAT, VersionBracket.fromProtocol(399));
    }

    @Test
    void fromProtocol_betweenCombatAndFlat_fallsToLatest() {
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(350));
    }

    @Test
    void fromProtocol_betweenFlatAndWorld_fallsToLatest() {
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(550));
    }

    @Test
    void fromProtocol_futureProtocol_returnsLatest() {
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(999));
    }

    @Test
    void fromProtocol_negativeProtocol_returnsLegacy() {
        assertEquals(VersionBracket.LEGACY, VersionBracket.fromProtocol(-1));
    }

    // ── getBracketDistance() ───────────────────────────────────────

    @Test
    void bracketDistance_sameBracket_returnsZero() {
        assertEquals(0, VersionBracket.getBracketDistance(4, 47));
    }

    @Test
    void bracketDistance_legacyToCombat_returnsOne() {
        assertEquals(1, VersionBracket.getBracketDistance(4, 110));
    }

    @Test
    void bracketDistance_legacyToFlat_returnsTwo() {
        assertEquals(2, VersionBracket.getBracketDistance(4, 404));
    }

    @Test
    void bracketDistance_legacyToLatest_returnsFive() {
        assertEquals(5, VersionBracket.getBracketDistance(4, 767));
    }

    @Test
    void bracketDistance_flatToWorld_returnsOne() {
        assertEquals(1, VersionBracket.getBracketDistance(404, 573));
    }

    @Test
    void bracketDistance_modernToLatest_returnsOne() {
        assertEquals(1, VersionBracket.getBracketDistance(760, 767));
    }

    @Test
    void bracketDistance_isSymmetric() {
        assertEquals(
                VersionBracket.getBracketDistance(4, 767),
                VersionBracket.getBracketDistance(767, 4)
        );
    }

    // ── contains() ─────────────────────────────────────────────────

    @Test
    void contains_legacyMinAndMax() {
        assertTrue(VersionBracket.LEGACY.contains(4));
        assertTrue(VersionBracket.LEGACY.contains(47));
    }

    @Test
    void contains_legacyDoesNotContainCombatProtocol() {
        assertFalse(VersionBracket.LEGACY.contains(110));
    }

    @Test
    void contains_combatRange() {
        assertTrue(VersionBracket.COMBAT.contains(110));
        assertTrue(VersionBracket.COMBAT.contains(340));
    }

    @Test
    void contains_combatDoesNotContainFlatProtocol() {
        assertFalse(VersionBracket.COMBAT.contains(404));
    }

    @Test
    void contains_flatRange() {
        assertTrue(VersionBracket.FLAT.contains(404));
        assertTrue(VersionBracket.FLAT.contains(498));
    }

    @Test
    void contains_flatContainsMinProtocol() {
        assertTrue(VersionBracket.FLAT.contains(393));
    }

    @Test
    void contains_worldRange() {
        assertTrue(VersionBracket.WORLD.contains(573));
        assertTrue(VersionBracket.WORLD.contains(758));
    }

    @Test
    void contains_modernRange() {
        assertTrue(VersionBracket.MODERN.contains(760));
        assertTrue(VersionBracket.MODERN.contains(766));
    }

    @Test
    void contains_latestRange() {
        assertTrue(VersionBracket.LATEST.contains(767));
        assertTrue(VersionBracket.LATEST.contains(800));
        assertTrue(VersionBracket.LATEST.contains(99999));
    }

    // ── enum structure ─────────────────────────────────────────────

    @Test
    void enumValues_hasExactlySixValues() {
        assertEquals(6, VersionBracket.values().length);
    }

    @Test
    void enumValues_areInCorrectOrder() {
        VersionBracket[] values = VersionBracket.values();
        assertEquals(VersionBracket.LEGACY, values[0]);
        assertEquals(VersionBracket.COMBAT, values[1]);
        assertEquals(VersionBracket.FLAT, values[2]);
        assertEquals(VersionBracket.WORLD, values[3]);
        assertEquals(VersionBracket.MODERN, values[4]);
        assertEquals(VersionBracket.LATEST, values[5]);
    }

    @Test
    void enumOrdinals_matchArrayPositions() {
        assertEquals(0, VersionBracket.LEGACY.ordinal());
        assertEquals(1, VersionBracket.COMBAT.ordinal());
        assertEquals(2, VersionBracket.FLAT.ordinal());
        assertEquals(3, VersionBracket.WORLD.ordinal());
        assertEquals(4, VersionBracket.MODERN.ordinal());
        assertEquals(5, VersionBracket.LATEST.ordinal());
    }

    // ── range boundary coverage ────────────────────────────────────

    @Test
    void eachBracketCoversItsExactMinMax() {
        for (VersionBracket bracket : VersionBracket.values()) {
            assertTrue(bracket.contains(bracket.getMinProtocol()),
                    bracket + ".contains(minProtocol=" + bracket.getMinProtocol() + ") should be true");
            assertTrue(bracket.contains(bracket.getMaxProtocol()),
                    bracket + ".contains(maxProtocol=" + bracket.getMaxProtocol() + ") should be true");
        }
    }

    @Test
    void adjacentBracketsDoNotOverlap() {
        VersionBracket[] values = VersionBracket.values();
        for (int i = 0; i < values.length - 1; i++) {
            VersionBracket current = values[i];
            VersionBracket next = values[i + 1];
            assertFalse(current.contains(next.getMinProtocol()),
                    current + " should not contain " + next + "'s minProtocol=" + next.getMinProtocol());
        }
    }
}
