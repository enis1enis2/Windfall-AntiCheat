package io.windfall.anticheat.core.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhysicsConstantsTest {

    @Test
    void gravity_isCorrect() {
        assertEquals(0.08, PhysicsConstants.GRAVITY);
    }

    @Test
    void playerJumpMomentum_isCorrect() {
        assertEquals(0.42, PhysicsConstants.PLAYER_JUMP_MOMENTUM);
    }

    @Test
    void airDrag_isCorrect() {
        assertEquals(0.98, PhysicsConstants.AIR_DRAG);
    }

    @Test
    void waterDrag_ieeePrecision() {
        assertEquals(0.800000011920929, PhysicsConstants.WATER_DRAG);
    }

    @Test
    void lavaDrag_isCorrect() {
        assertEquals(0.5, PhysicsConstants.LAVA_DRAG);
    }

    @Test
    void groundFriction_isCorrect() {
        assertEquals(0.91, PhysicsConstants.GROUND_FRICTION);
    }

    @Test
    void playerWidth_isCorrect() {
        assertEquals(0.6, PhysicsConstants.PLAYER_WIDTH);
    }

    @Test
    void playerHeight_isCorrect() {
        assertEquals(1.8, PhysicsConstants.PLAYER_HEIGHT_NORMAL);
        assertEquals(1.5, PhysicsConstants.PLAYER_HEIGHT_SNEAKING);
    }

    @Test
    void playerEyeHeight_isCorrect() {
        assertEquals(1.62, PhysicsConstants.PLAYER_EYE_HEIGHT_NORMAL);
        assertEquals(1.27, PhysicsConstants.PLAYER_EYE_HEIGHT_SNEAKING);
    }

    @Test
    void playerWalkSpeed_isCorrect() {
        assertEquals(0.1, PhysicsConstants.PLAYER_WALK_SPEED);
    }

    @Test
    void sprintMultiplier_isCorrect() {
        assertEquals(1.3, PhysicsConstants.PLAYER_SPRINT_MULTIPLIER);
    }

    @Test
    void crouchMultiplier_isCorrect() {
        assertEquals(0.3, PhysicsConstants.PLAYER_CROUCH_MULTIPLIER);
    }

    @Test
    void stepHeight_isCorrect() {
        assertEquals(0.6, PhysicsConstants.STEP_HEIGHT);
    }

    @Test
    void getBlockFriction_nullReturnsGroundFriction() {
        assertEquals(PhysicsConstants.GROUND_FRICTION, PhysicsConstants.getBlockFriction(null));
    }

    @Test
    void getJumpBoostHorizontal_level0() {
        assertEquals(0.1, PhysicsConstants.getJumpBoostHorizontal(0));
    }

    @Test
    void getJumpBoostHorizontal_level2() {
        assertEquals(0.3, PhysicsConstants.getJumpBoostHorizontal(2), 0.0001);
    }

    @Test
    void getJumpBoostVertical_pre19() {
        assertEquals(0.15, PhysicsConstants.getJumpBoostVertical(0, false));
        assertEquals(0.30, PhysicsConstants.getJumpBoostVertical(1, false));
    }

    @Test
    void getJumpBoostVertical_post19() {
        assertEquals(0.1, PhysicsConstants.getJumpBoostVertical(0, true));
        assertEquals(0.2, PhysicsConstants.getJumpBoostVertical(1, true));
    }

    @Test
    void getSpeedEffectMultiplier_level0() {
        assertEquals(1.2, PhysicsConstants.getSpeedEffectMultiplier(0));
    }

    @Test
    void getSpeedEffectMultiplier_level2() {
        assertEquals(1.6, PhysicsConstants.getSpeedEffectMultiplier(2));
    }

    @Test
    void getSlownessEffectMultiplier_level0() {
        assertEquals(0.85, PhysicsConstants.getSlownessEffectMultiplier(0), 0.001);
    }

    @Test
    void getSlownessEffectMultiplier_highLevel_clampsToZero() {
        assertEquals(0.0, PhysicsConstants.getSlownessEffectMultiplier(7), 0.001);
    }

    @Test
    void frictionMap_valuesExist() {
        // Soul sand should be loaded from static block
        assertNotEquals(PhysicsConstants.GROUND_FRICTION, PhysicsConstants.SOUL_SAND_FRICTION);
        assertNotEquals(PhysicsConstants.GROUND_FRICTION, PhysicsConstants.ICE_FRICTION);
        assertNotEquals(PhysicsConstants.GROUND_FRICTION, PhysicsConstants.WEB_FRICTION);
    }
}
