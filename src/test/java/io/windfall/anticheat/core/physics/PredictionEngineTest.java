package io.windfall.anticheat.core.physics;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PredictionEngineTest {

    // === calculateHorizontalSpeed ===

    @Test
    void testCalculateHorizontalSpeed_zeroComponents() {
        assertEquals(0.0, PredictionEngine.calculateHorizontalSpeed(0, 0), 1e-9);
    }

    @Test
    void testCalculateHorizontalSpeed_pureX() {
        assertEquals(3.0, PredictionEngine.calculateHorizontalSpeed(3.0, 0), 1e-9);
    }

    @Test
    void testCalculateHorizontalSpeed_pureZ() {
        assertEquals(4.0, PredictionEngine.calculateHorizontalSpeed(0, 4.0), 1e-9);
    }

    @Test
    void testCalculateHorizontalSpeed_pythagorean() {
        assertEquals(5.0, PredictionEngine.calculateHorizontalSpeed(3.0, 4.0), 1e-9);
    }

    @Test
    void testCalculateHorizontalSpeed_negative() {
        assertEquals(5.0, PredictionEngine.calculateHorizontalSpeed(-3.0, -4.0), 1e-9);
    }

    // === calculateBaseSpeed ===

    @Test
    void testCalculateBaseSpeed_walking() {
        double speed = PredictionEngine.calculateBaseSpeed(false, false, 1.0, 1.0);
        assertEquals(PhysicsConstants.PLAYER_WALK_SPEED, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_sprinting() {
        double speed = PredictionEngine.calculateBaseSpeed(true, false, 1.0, 1.0);
        assertEquals(PhysicsConstants.PLAYER_WALK_SPEED * PhysicsConstants.PLAYER_SPRINT_MULTIPLIER, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_sneaking() {
        double speed = PredictionEngine.calculateBaseSpeed(false, true, 1.0, 1.0);
        assertEquals(PhysicsConstants.PLAYER_WALK_SPEED * PhysicsConstants.PLAYER_CROUCH_MULTIPLIER, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_sprintAndCrouch() {
        double speed = PredictionEngine.calculateBaseSpeed(true, true, 1.0, 1.0);
        double expected = PhysicsConstants.PLAYER_WALK_SPEED
                * PhysicsConstants.PLAYER_SPRINT_MULTIPLIER
                * PhysicsConstants.PLAYER_CROUCH_MULTIPLIER;
        assertEquals(expected, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_withSpeedPotion() {
        double speed = PredictionEngine.calculateBaseSpeed(false, false, 1.2, 1.0);
        assertEquals(PhysicsConstants.PLAYER_WALK_SPEED * 1.2, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_withSlownessPotion() {
        double speed = PredictionEngine.calculateBaseSpeed(false, false, 1.0, 0.85);
        assertEquals(PhysicsConstants.PLAYER_WALK_SPEED * 0.85, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_withBothPotions() {
        double speed = PredictionEngine.calculateBaseSpeed(false, false, 1.2, 0.85);
        assertEquals(PhysicsConstants.PLAYER_WALK_SPEED * 1.2 * 0.85, speed, 1e-9);
    }

    @Test
    void testCalculateBaseSpeed_sprintWithSpeed5() {
        double speed = PredictionEngine.calculateBaseSpeed(true, false, 2.0, 1.0);
        double expected = PhysicsConstants.PLAYER_WALK_SPEED
                * PhysicsConstants.PLAYER_SPRINT_MULTIPLIER * 2.0;
        assertEquals(expected, speed, 1e-9);
    }

    // === calculateMaxHorizontalSpeed (no deltaY overload) ===

    @Test
    void testCalculateMaxHorizontalSpeed_groundZeroLast() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double maxSpeed = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.0, true, false, false, 767);
        assertTrue(maxSpeed > 0, "Ground max speed from zero should be positive");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_airZeroLast() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double maxSpeed = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.0, false, false, false, 767);
        assertTrue(maxSpeed > 0, "Air max speed from zero should be positive");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_groundVsAir() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double groundMax = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, true, false, false, 767);
        double airMax = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, false, false, 767);
        assertTrue(groundMax > airMax,
                "Ground acceleration should produce higher max speed than air");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_inWebSlower() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double normalMax = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, false, false, 767);
        double webMax = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, true, false, 767);
        assertTrue(webMax < normalMax,
                "Web friction should reduce max speed");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_swimmingSlower() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double normalMax = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, true, false, false, 767);
        double swimMax = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, true, false, true, 767);
        assertTrue(swimMax < normalMax,
                "Swimming acceleration should be reduced");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_swimBoostIncreasesWithDeltaY() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double maxNoBoost = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, false, true, 393, 0.0);
        double maxWithBoost = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, false, true, 393, 0.5);
        assertTrue(maxWithBoost > maxNoBoost,
                "Positive deltaY should add swim boost");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_swimBoostZeroForNegativeDeltaY() {
        double baseSpeed = PhysicsConstants.PLAYER_WALK_SPEED;
        double maxNoBoost = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, false, true, 393, 0.0);
        double maxNegDelta = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, 0.1, false, false, true, 393, -0.5);
        assertEquals(maxNoBoost, maxNegDelta, 1e-9,
                "Negative deltaY should not add swim boost");
    }

    @Test
    void testCalculateMaxHorizontalSpeed_sprintFasterThanWalk() {
        double walkMax = PredictionEngine.calculateMaxHorizontalSpeed(
                0.1, 0.1, true, false, false, 767);
        double sprintMax = PredictionEngine.calculateMaxHorizontalSpeed(
                0.1 * PhysicsConstants.PLAYER_SPRINT_MULTIPLIER, 0.1, true, false, false, 767);
        assertTrue(sprintMax > walkMax);
    }

    // === predictDeltaY ===

    @Test
    void testPredictDeltaY_normalFall() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, false, 1.0, false, false);
        double expected = (-0.5 - PhysicsConstants.GRAVITY) * PhysicsConstants.AIR_DRAG;
        assertEquals(expected, predicted, 1e-9);
    }

    @Test
    void testPredictDeltaY_waterDrag() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, true, false, false, false, false, false, 1.0, false, false);
        assertTrue(predicted < 0, "Water drag should reduce downward velocity");
    }

    @Test
    void testPredictDeltaY_lavaDrag() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, false, 1.0, false, false);
        double lavaPredicted = PredictionEngine.predictDeltaY(
                -0.5, false, true, false, false, false, false, 1.0, false, false);
        assertTrue(Math.abs(lavaPredicted) < Math.abs(predicted),
                "Lava drag should reduce velocity more than air");
    }

    @Test
    void testPredictDeltaY_climbing() {
        double predicted = PredictionEngine.predictDeltaY(
                0.1, false, false, true, false, false, false, 1.0, false, false);
        assertEquals(0.1, predicted, 1e-9, "Climbing should preserve small positive deltaY");
    }

    @Test
    void testPredictDeltaY_climbingMaxCap() {
        double predicted = PredictionEngine.predictDeltaY(
                0.5, false, false, true, false, false, false, 1.0, false, false);
        assertEquals(0.15, predicted, 1e-9, "Climbing should cap deltaY at 0.15");
    }

    @Test
    void testPredictDeltaY_honeyBlock() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, true, false, false, 1.0, false, false);
        assertEquals(-0.5, predicted, 1e-9, "Honey should cap at -0.5");
    }

    @Test
    void testPredictDeltaY_honeyBlockAllowsDown() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.3, false, false, false, true, false, false, 1.0, false, false);
        assertEquals(-0.3, predicted, 1e-9, "Honey should not affect values above -0.5");
    }

    @Test
    void testPredictDeltaY_slowFalling() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, true, false, 1.0, false, false);
        double expected = (-0.5 - 0.01) * PhysicsConstants.AIR_DRAG;
        assertEquals(expected, predicted, 1e-9);
    }

    @Test
    void testPredictDeltaY_levitation() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, true, 1.0, false, false);
        double expected = -0.5 + 0.05 * 1.0;
        assertEquals(expected, predicted, 1e-9);
    }

    @Test
    void testPredictDeltaY_levitationHighAmplifier() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, true, 3.0, false, false);
        double expected = -0.5 + 0.05 * 3.0;
        assertEquals(expected, predicted, 1e-9);
    }

    @Test
    void testPredictDeltaY_fallFlyingUnchanged() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, false, 1.0, true, false);
        assertEquals(-0.5, predicted, 1e-9, "Fall flying should preserve deltaY");
    }

    @Test
    void testPredictDeltaY_riptideUnchanged() {
        double predicted = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, false, 1.0, false, true);
        assertEquals(-0.5, predicted, 1e-9, "Riptide should preserve deltaY");
    }

    @Test
    void testPredictDeltaY_waterOverridesAirDrag() {
        double airPrediction = PredictionEngine.predictDeltaY(
                -0.5, false, false, false, false, false, false, 1.0, false, false);
        double waterPrediction = PredictionEngine.predictDeltaY(
                -0.5, true, false, false, false, false, false, 1.0, false, false);
        assertTrue(Math.abs(waterPrediction) != Math.abs(airPrediction),
                "Water and air predictions should differ");
    }

    // === PhysicsConstants integration ===

    @Test
    void testPhysicsConstants_gravity() {
        assertEquals(0.08, PhysicsConstants.GRAVITY, 1e-9);
    }

    @Test
    void testPhysicsConstants_airDrag() {
        assertEquals(0.98, PhysicsConstants.AIR_DRAG, 1e-9);
    }

    @Test
    void testPhysicsConstants_waterDrag() {
        assertEquals(0.8, PhysicsConstants.WATER_DRAG, 1e-5);
    }

    @Test
    void testPhysicsConstants_lavaDrag() {
        assertEquals(0.5, PhysicsConstants.LAVA_DRAG, 1e-9);
    }

    @Test
    void testPhysicsConstants_walkSpeed() {
        assertEquals(0.1, PhysicsConstants.PLAYER_WALK_SPEED, 1e-9);
    }

    @Test
    void testPhysicsConstants_sprintMultiplier() {
        assertEquals(1.3, PhysicsConstants.PLAYER_SPRINT_MULTIPLIER, 1e-9);
    }

    @Test
    void testPhysicsConstants_crouchMultiplier() {
        assertEquals(0.3, PhysicsConstants.PLAYER_CROUCH_MULTIPLIER, 1e-9);
    }

    @Test
    void testPhysicsConstants_groundFriction() {
        assertEquals(0.91, PhysicsConstants.GROUND_FRICTION, 1e-9);
    }

    // === PredictionContext fields are computed ===

    @Test
    void testPredictionContext_horizontalSpeedFromDeltas() {
        double dx = 3.0, dz = 4.0;
        double expected = Math.sqrt(dx * dx + dz * dz);
        assertEquals(expected, PredictionEngine.calculateHorizontalSpeed(dx, dz), 1e-9);
    }
}
