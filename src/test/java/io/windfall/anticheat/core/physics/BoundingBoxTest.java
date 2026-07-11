package io.windfall.anticheat.core.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundingBoxTest {

    private final BoundingBox box = new BoundingBox(0, 0, 0, 1, 1, 1);

    @Test
    void dimensions_correct() {
        assertEquals(1.0, box.getWidth());
        assertEquals(1.0, box.getHeight());
        assertEquals(1.0, box.getDepth());
    }

    @Test
    void center_correct() {
        assertEquals(0.5, box.getCenterX());
        assertEquals(0.5, box.getCenterY());
        assertEquals(0.5, box.getCenterZ());
    }

    @Test
    void expand_byMargin() {
        BoundingBox expanded = box.expand(0.5);
        assertEquals(-0.5, expanded.minX);
        assertEquals(-0.5, expanded.minY);
        assertEquals(-0.5, expanded.minZ);
        assertEquals(1.5, expanded.maxX);
        assertEquals(1.5, expanded.maxY);
        assertEquals(1.5, expanded.maxZ);
    }

    @Test
    void expand_byAxes() {
        BoundingBox expanded = box.expand(1.0, 2.0, 3.0);
        assertEquals(-1.0, expanded.minX);
        assertEquals(-2.0, expanded.minY);
        assertEquals(-3.0, expanded.minZ);
        assertEquals(2.0, expanded.maxX);
        assertEquals(3.0, expanded.maxY);
        assertEquals(4.0, expanded.maxZ);
    }

    @Test
    void offset_translatesBox() {
        BoundingBox moved = box.offset(5, 3, 1);
        assertEquals(5.0, moved.minX);
        assertEquals(3.0, moved.minY);
        assertEquals(1.0, moved.minZ);
        assertEquals(6.0, moved.maxX);
        assertEquals(4.0, moved.maxY);
        assertEquals(2.0, moved.maxZ);
    }

    @Test
    void intersects_overlappingBoxes() {
        BoundingBox other = new BoundingBox(0.5, 0.5, 0.5, 1.5, 1.5, 1.5);
        assertTrue(box.intersects(other));
    }

    @Test
    void intersects_adjacentBoxes_notIntersecting() {
        BoundingBox other = new BoundingBox(1.0, 0, 0, 2.0, 1.0, 1.0);
        assertFalse(box.intersects(other));
    }

    @Test
    void intersects_separatedBoxes() {
        BoundingBox other = new BoundingBox(5, 5, 5, 6, 6, 6);
        assertFalse(box.intersects(other));
    }

    @Test
    void intersects_containedBox() {
        BoundingBox small = new BoundingBox(0.2, 0.2, 0.2, 0.8, 0.8, 0.8);
        assertTrue(box.intersects(small));
        assertTrue(small.intersects(box));
    }

    @Test
    void contains_pointInside() {
        assertTrue(box.contains(0.5, 0.5, 0.5));
    }

    @Test
    void contains_pointOnEdge() {
        assertTrue(box.contains(0.0, 0.0, 0.0));
        assertTrue(box.contains(1.0, 1.0, 1.0));
    }

    @Test
    void contains_pointOutside() {
        assertFalse(box.contains(1.5, 0.5, 0.5));
        assertFalse(box.contains(0.5, -0.1, 0.5));
    }

    @Test
    void containsXZ_inside() {
        assertTrue(box.containsXZ(0.5, 0.5));
    }

    @Test
    void containsXZ_outside() {
        assertFalse(box.containsXZ(1.5, 0.5));
    }

    @Test
    void distanceTo_pointInside_returnsZero() {
        assertEquals(0.0, box.distanceTo(0.5, 0.5, 0.5));
    }

    @Test
    void distanceTo_pointOutside() {
        // Point at (3, 0.5, 0.5), closest edge is at x=1
        assertEquals(2.0, box.distanceTo(3.0, 0.5, 0.5), 0.001);
    }

    @Test
    void distanceTo_pointDiagonal() {
        // Point at (2, 2, 2), closest corner is (1, 1, 1)
        assertEquals(Math.sqrt(3.0), box.distanceTo(2.0, 2.0, 2.0), 0.001);
    }

    @Test
    void addCoords_expandsToEnclose() {
        BoundingBox result = box.addCoords(-5, -5, -5, 10, 10, 10);
        assertEquals(-5.0, result.minX);
        assertEquals(-5.0, result.minY);
        assertEquals(-5.0, result.minZ);
        assertEquals(10.0, result.maxX);
        assertEquals(10.0, result.maxY);
        assertEquals(10.0, result.maxZ);
    }

    @Test
    void addCoords_shrinksIfInside() {
        BoundingBox result = box.addCoords(0.3, 0.3, 0.3, 0.7, 0.7, 0.7);
        // Box already contains these points, so no change
        assertEquals(0.0, result.minX);
        assertEquals(1.0, result.maxX);
    }

    @Test
    void fromPlayer_createsCorrectBox() {
        BoundingBox playerBox = BoundingBox.fromPlayer(10.0, 64.0, 20.0, false, 47);
        // Half width = 0.3, height = 1.8
        assertEquals(9.7, playerBox.minX, 0.001);
        assertEquals(64.0, playerBox.minY);
        assertEquals(19.7, playerBox.minZ, 0.001);
        assertEquals(10.3, playerBox.maxX, 0.001);
        assertEquals(65.8, playerBox.maxY, 0.001);
        assertEquals(20.3, playerBox.maxZ, 0.001);
    }
}
