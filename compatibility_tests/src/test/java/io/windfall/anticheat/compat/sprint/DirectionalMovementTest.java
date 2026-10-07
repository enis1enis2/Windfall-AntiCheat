package io.windfall.anticheat.compat.sprint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionalMovementTest {

    @Test
    void movingForwardMatchesForwardSector() {
        DirectionalMovement move = DirectionalMovement.predict(0.0D, 0.3D, 0.0D);
        assertTrue(move.isMoving());
        assertEquals(DirectionalMovement.MovementSector.FORWARD, move.sector);
        assertEquals(DirectionalMovement.RelativeMove.FORWARD, move.toSimpleRelativeMove());
        assertEquals(1.0D, move.dot, 1.0E-9D);
        assertEquals(0.0D, move.signedAngle, 1.0E-9D);
    }

    @Test
    void movingBackwardsMatchesBackwardSector() {
        DirectionalMovement move = DirectionalMovement.predict(0.0D, -0.3D, 0.0D);
        assertEquals(DirectionalMovement.MovementSector.BACKWARD, move.sector);
        assertEquals(DirectionalMovement.RelativeMove.BACKWARD, move.toSimpleRelativeMove());
        assertEquals(-1.0D, move.dot, 1.0E-9D);
    }

    @Test
    void movingRightMatchesRightSector() {
        DirectionalMovement move = DirectionalMovement.predict(-0.3D, 0.0D, 0.0D);
        assertEquals(DirectionalMovement.MovementSector.RIGHT, move.sector);
        assertEquals(DirectionalMovement.RelativeMove.RIGHT, move.toSimpleRelativeMove());
    }

    @Test
    void movingLeftMatchesLeftSector() {
        DirectionalMovement move = DirectionalMovement.predict(0.3D, 0.0D, 0.0D);
        assertEquals(DirectionalMovement.MovementSector.LEFT, move.sector);
        assertEquals(DirectionalMovement.RelativeMove.LEFT, move.toSimpleRelativeMove());
    }

    @Test
    void nearZeroIsStationary() {
        DirectionalMovement move = DirectionalMovement.predict(0.0D, 0.0D, 12.0F);
        assertEquals(DirectionalMovement.MovementSector.STATIONARY, move.sector);
        assertEquals(DirectionalMovement.RelativeMove.STATIONARY, move.toSimpleRelativeMove());
        assertEquals(0.0D, move.dot, 0.0D);
        assertEquals(0.0D, move.absoluteAngle, 0.0D);
    }

    @Test
    void normalizedMoveComponentsSumToUnitLength() {
        DirectionalMovement move = DirectionalMovement.predict(1.7D, 2.4D, 33.0D);
        double length = Math.hypot(move.moveX, move.moveZ);
        assertEquals(1.0D, length, 1.0E-9D);
    }

    @Test
    void dotAndCrossAreClamped() {
        DirectionalMovement move = DirectionalMovement.predict(0.001D, 0.0D, 0.0D);
        assertTrue(move.dot <= 1.0D && move.dot >= -1.0D);
        assertTrue(move.cross <= 1.0D && move.cross >= -1.0D);
    }

    @Test
    void wrapDegreesNormalizesToSymmetricRange() {
        assertEquals(0.0F, DirectionalMovement.wrapDegrees(360.0F), 1.0E-4F);
        assertEquals(-170.0F, DirectionalMovement.wrapDegrees(190.0F), 1.0E-4F);
        assertEquals(170.0F, DirectionalMovement.wrapDegrees(-190.0F), 1.0E-4F);
        assertEquals(20.0F, DirectionalMovement.yawDifference(200.0F, 180.0F), 1.0E-4F);
    }
}