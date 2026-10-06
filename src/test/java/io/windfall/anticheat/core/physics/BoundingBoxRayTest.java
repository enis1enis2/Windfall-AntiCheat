package io.windfall.anticheat.core.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the slab-method ray/AABB intersection used by the cursor checks.
 */
class BoundingBoxRayTest {

    private static final double DELTA = 1e-9;

    /** Unit box occupying the unit cube at the origin. */
    private BoundingBox unitCube() {
        return new BoundingBox(0, 0, 0, 1, 1, 1);
    }

    @Test
    void rayStraightThroughBox_hits() {
        BoundingBox box = unitCube();
        assertTrue(box.intersectsRay(-1, 0.5, 0.5, 1, 0, 0, 5.0));
    }

    @Test
    void rayPointedAway_misses() {
        BoundingBox box = unitCube();
        assertFalse(box.intersectsRay(-1, 0.5, 0.5, -1, 0, 0, 5.0));
    }

    @Test
    void rayPassesBesideBox_misses() {
        BoundingBox box = unitCube();
        /* Parallel to the box, offset by 2 blocks on Z. */
        assertFalse(box.intersectsRay(-1, 0.5, 2.5, 1, 0, 0, 5.0));
    }

    @Test
    void rayTooShort_misses() {
        BoundingBox box = unitCube();
        /* Box starts at x=0, ray starts at x=-1 heading +X; needs at least 1.0 length. */
        assertFalse(box.intersectsRay(-1, 0.5, 0.5, 1, 0, 0, 0.5));
        assertTrue(box.intersectsRay(-1, 0.5, 0.5, 1, 0, 0, 1.0));
    }

    @Test
    void rayStartingInsideBox_hits() {
        BoundingBox box = unitCube();
        assertTrue(box.intersectsRay(0.5, 0.5, 0.5, 0, 1, 0, 5.0));
    }

    @Test
    void zeroDirection_neverHits() {
        BoundingBox box = unitCube();
        assertFalse(box.intersectsRay(0.5, 0.5, 0.5, 0, 0, 0, 100.0));
    }

    @Test
    void diagonalRay_hits() {
        BoundingBox box = unitCube();
        /* Slope 0.05 clears the top of the box with 0.2 blocks of margin, so this is a
         * clean hit rather than a corner tangent. Slope 0.1 would graze the box exactly
         * at (0, 1.0), giving tMin == tMax and a result that depends on rounding. */
        assertTrue(box.intersectsRay(-5, 0.5, 0.5, 1, 0.05, 0, 20.0));
    }

    @Test
    void rayGrazingCornerMisses() {
        BoundingBox box = unitCube();
        /* Slope 0.1 passes exactly through the corner (0, 1.0): tangent, zero depth. */
        assertFalse(box.intersectsRay(-5, 0.5, 0.5, 1, 0.1, 0, 20.0));
    }

    @Test
    void rayDownwardIntoBox_hits() {
        BoundingBox box = unitCube();
        assertTrue(box.intersectsRay(0.5, 5, 0.5, 0, -1, 0, 10.0));
    }

    @Test
    void unnormalizedDirection_respectsMaxDistance() {
        BoundingBox box = unitCube();
        /* Direction (1000, 0, 0) has length 1000, so maxDistance 10 blocks covers x=-1..9. */
        assertTrue(box.intersectsRay(-1, 0.5, 0.5, 1000, 0, 0, 10.0));
        /* maxDistance 0.5 blocks cannot reach a box 1 block away. */
        assertFalse(box.intersectsRay(-1, 0.5, 0.5, 1000, 0, 0, 0.5));
    }

    @Test
    void axisAlignedRay_parallelToMissedSlab_misses() {
        BoundingBox box = new BoundingBox(10, 10, 10, 11, 11, 11);
        /* Ray travels along X at y=0, which is below the box on Y. */
        assertFalse(box.intersectsRay(0, 0, 10.5, 1, 0, 0, 50.0));
    }

    @Test
    void expandedBox_catchesEdgeClick() {
        BoundingBox strict = unitCube();
        BoundingBox padded = strict.expand(0.15);
        /* y = 1.1 sits above the strict box (top 1.0) but inside the padded box (top 1.15). */
        assertFalse(strict.intersectsRay(-1, 1.1, 0.5, 1, 0, 0, 5.0));
        assertTrue(padded.intersectsRay(-1, 1.1, 0.5, 1, 0, 0, 5.0));
    }

    @Test
    void paddingBelowTolerance_stillMisses() {
        BoundingBox strict = unitCube();
        BoundingBox padded = strict.expand(0.15);
        /* y = 1.2 exceeds even the padded top of 1.15, so padding must not catch it. */
        assertFalse(padded.intersectsRay(-1, 1.2, 0.5, 1, 0, 0, 5.0));
    }

    @Test
    void diagonalThroughCorner_hits() {
        BoundingBox box = unitCube();
        assertTrue(box.intersectsRay(-1, -1, 0.5, 1, 1, 0, 5.0));
    }

    @Test
    void rayMissesBelowBox() {
        BoundingBox box = unitCube();
        assertFalse(box.intersectsRay(0.5, 3, 0.5, 0, 1, 0, 1.0));
    }

    @Test
    void maxDistanceExactlyReachesBox() {
        BoundingBox box = unitCube();
        double dist = Math.sqrt(1.0 + 0.25 + 0.25);
        assertTrue(box.intersectsRay(0.0, 0.5, 0.5, 0, 0, 1, dist + DELTA));
    }
}