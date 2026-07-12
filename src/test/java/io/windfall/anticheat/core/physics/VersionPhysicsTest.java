package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer.Pose;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionPhysicsTest {

    // === PLAYER DIMENSIONS ===

    @Test
    void playerHeight_normal_18() {
        assertEquals(1.8, VersionPhysics.getPlayerHeight(false, 47));
    }

    @Test
    void playerHeight_sneaking_post114() {
        // Post-1.14 sneaking is 1.5
        assertEquals(1.5, VersionPhysics.getPlayerHeight(true, 498));
    }

    @Test
    void playerHeight_sneaking_pre114() {
        // Pre-1.14 sneaking was 1.62
        assertEquals(1.62, VersionPhysics.getPlayerHeight(true, 340));
    }

    @Test
    void playerWidth_always06() {
        assertEquals(0.6, VersionPhysics.getPlayerWidth(47));
        assertEquals(0.6, VersionPhysics.getPlayerWidth(763));
        assertEquals(0.6, VersionPhysics.getPlayerWidth(800));
    }

    @Test
    void playerEyeHeight_post114() {
        assertEquals(1.62, VersionPhysics.getPlayerEyeHeight(false, 498));
        assertEquals(1.27, VersionPhysics.getPlayerEyeHeight(true, 498));
    }

    @Test
    void stepHeight_post114() {
        assertEquals(0.6, VersionPhysics.getStepHeight(498));
    }

    @Test
    void stepHeight_pre19() {
        assertEquals(0.5, VersionPhysics.getStepHeight(47));
    }

    // === REACH ===

    @Test
    void maxReach_pre19_is4() {
        assertEquals(4.0, VersionPhysics.getMaxReach(47));
    }

    @Test
    void maxReach_post19_is3() {
        assertEquals(3.0, VersionPhysics.getMaxReach(110));
    }

    @Test
    void sprintReachBonus_pre19_zero() {
        assertEquals(0.0, VersionPhysics.getSprintReachBonus(47));
    }

    @Test
    void sprintReachBonus_post19() {
        assertEquals(0.05, VersionPhysics.getSprintReachBonus(110));
    }

    @Test
    void cooldownReachBonus_pre19_zero() {
        assertEquals(0.0, VersionPhysics.getCooldownReachBonus(47));
    }

    // === ATTACK ===

    @Test
    void hasAttackCooldown_pre19_false() {
        assertFalse(VersionPhysics.hasAttackCooldown(47));
    }

    @Test
    void hasAttackCooldown_post19_true() {
        assertTrue(VersionPhysics.hasAttackCooldown(110));
    }

    @Test
    void attackCooldownMultiplier_pre19_always1() {
        assertEquals(1.0, VersionPhysics.getAttackCooldownMultiplier(47, 10.0));
    }

    @Test
    void attackCooldownMultiplier_post19_fullCooldown() {
        // cooldownTicks >= 20 should return max (1.0)
        assertEquals(1.0, VersionPhysics.getAttackCooldownMultiplier(110, 20.0), 0.001);
    }

    @Test
    void attackCooldownMultiplier_post19_halfCooldown() {
        // 10 ticks = 50% progress
        double expected = 0.2 + 0.8 * 0.5;
        assertEquals(expected, VersionPhysics.getAttackCooldownMultiplier(110, 10.0), 0.001);
    }

    // === CRITICAL HITS ===

    @Test
    void criticalDamageMultiplier_isAlways15() {
        assertEquals(1.5, VersionPhysics.getCriticalDamageMultiplier(47));
        assertEquals(1.5, VersionPhysics.getCriticalDamageMultiplier(763));
    }

    @Test
    void sharpnessDamage_pre19() {
        assertEquals(1.0, VersionPhysics.getSharpnessDamagePerLevel(47));
    }

    @Test
    void sharpnessDamage_post19() {
        assertEquals(0.5, VersionPhysics.getSharpnessDamagePerLevel(110));
    }

    @Test
    void sprintCritMultiplier_pre19() {
        assertEquals(1.5, VersionPhysics.getSprintCritMultiplier(47));
    }

    @Test
    void sprintCritMultiplier_post19() {
        assertEquals(1.1, VersionPhysics.getSprintCritMultiplier(110));
    }

    // === FLUID SYSTEM ===

    @Test
    void hasNewFluidSystem_pre113_false() {
        assertFalse(VersionPhysics.hasNewFluidSystem(340));
    }

    @Test
    void hasNewFluidSystem_post113_true() {
        assertTrue(VersionPhysics.hasNewFluidSystem(404));
    }

    @Test
    void swimBoost_pre113() {
        assertEquals(0.03, VersionPhysics.getSwimBoost(340));
    }

    @Test
    void swimBoost_post113() {
        assertEquals(0.04, VersionPhysics.getSwimBoost(404));
    }

    // === WORLD HEIGHT ===

    @Test
    void worldHeight_pre118() {
        assertEquals(0, VersionPhysics.getMinWorldHeight(340));
        assertEquals(256, VersionPhysics.getMaxWorldHeight(340));
    }

    @Test
    void worldHeight_post118() {
        assertEquals(-64, VersionPhysics.getMinWorldHeight(757));
        assertEquals(320, VersionPhysics.getMaxWorldHeight(757));
    }

    @Test
    void hasWorldHeightExpansion_pre118_false() {
        assertFalse(VersionPhysics.hasWorldHeightExpansion(756));
    }

    @Test
    void hasWorldHeightExpansion_post118_true() {
        assertTrue(VersionPhysics.hasWorldHeightExpansion(757));
    }

    // === COMBAT FEATURES ===

    @Test
    void swordBlocking_pre19() {
        assertTrue(VersionPhysics.hasSwordBlocking(47));
    }

    @Test
    void swordBlocking_post19() {
        assertFalse(VersionPhysics.hasSwordBlocking(110));
    }

    @Test
    void shieldBlocking_post19() {
        assertTrue(VersionPhysics.hasShieldBlocking(110));
    }

    @Test
    void swordBlockDamageReduction_pre19() {
        assertEquals(0.5, VersionPhysics.getSwordBlockDamageReduction(47));
    }

    @Test
    void swordBlockDamageReduction_post19() {
        assertEquals(0.0, VersionPhysics.getSwordBlockDamageReduction(110));
    }

    // === ELYTRA / BOATS ===

    @Test
    void hasElytra_post19() {
        assertTrue(VersionPhysics.hasElytra(110));
    }

    @Test
    void hasElytra_pre19() {
        assertFalse(VersionPhysics.hasElytra(47));
    }

    @Test
    void boatFly_19_to_114() {
        assertTrue(VersionPhysics.hasBoatFly(110));
    }

    @Test
    void boatFly_post114() {
        assertFalse(VersionPhysics.hasBoatFly(498));
    }

    // === VERSION HELPERS ===

    @Test
    void isLegacyProtocol_18() {
        assertTrue(VersionPhysics.isLegacyProtocol(47));
    }

    @Test
    void isLegacyProtocol_19() {
        assertFalse(VersionPhysics.isLegacyProtocol(110));
    }

    @Test
    void isModernProtocol_pre1205() {
        assertFalse(VersionPhysics.isModernProtocol(763));
    }

    @Test
    void isModernProtocol_post1205() {
        assertTrue(VersionPhysics.isModernProtocol(766));
    }

    @Test
    void isPreCombatUpdate_18() {
        assertTrue(VersionPhysics.isPreCombatUpdate(47));
    }

    @Test
    void isPreCombatUpdate_19() {
        assertFalse(VersionPhysics.isPreCombatUpdate(110));
    }

    @Test
    void isPreFlattening_112() {
        assertTrue(VersionPhysics.isPreFlattening(340));
    }

    @Test
    void isPreFlattening_113() {
        assertFalse(VersionPhysics.isPreFlattening(404));
    }

    @Test
    void isPreWorldHeight_117() {
        assertTrue(VersionPhysics.isPreWorldHeight(756));
    }

    @Test
    void isPreWorldHeight_118() {
        assertFalse(VersionPhysics.isPreWorldHeight(757));
    }

    @Test
    void hasInputPackets_1215() {
        assertTrue(VersionPhysics.hasInputPackets(770));
    }

    @Test
    void hasInputPackets_1214() {
        assertFalse(VersionPhysics.hasInputPackets(769));
    }

    // === PLAYER HEIGHT BY POSE ===

    @Test
    void playerHeight_pose_standing_767() {
        assertEquals(1.8, VersionPhysics.getPlayerHeight(Pose.STANDING, 767));
    }

    @Test
    void playerHeight_pose_sneaking_767() {
        assertEquals(1.5, VersionPhysics.getPlayerHeight(Pose.SNEAKING, 767));
    }

    @Test
    void playerHeight_pose_sneaking_47() {
        assertEquals(1.62, VersionPhysics.getPlayerHeight(Pose.SNEAKING, 47));
    }

    @Test
    void playerHeight_pose_swimming_767() {
        assertEquals(0.6, VersionPhysics.getPlayerHeight(Pose.SWIMMING, 767));
    }

    @Test
    void playerHeight_pose_fallFlying_767() {
        assertEquals(0.6, VersionPhysics.getPlayerHeight(Pose.FALL_FLYING, 767));
    }

    @Test
    void playerHeight_pose_sleeping_767() {
        assertEquals(0.2, VersionPhysics.getPlayerHeight(Pose.SLEEPING, 767));
    }

    @Test
    void playerHeight_pose_dying_767() {
        assertEquals(0.0, VersionPhysics.getPlayerHeight(Pose.DYING, 767));
    }

    @Test
    void playerHeight_pose_spinAttack_767() {
        assertEquals(0.6, VersionPhysics.getPlayerHeight(Pose.SPIN_ATTACK, 767));
    }

    @Test
    void playerHeight_pose_longJumping_767() {
        assertEquals(1.5, VersionPhysics.getPlayerHeight(Pose.LONG_JUMPING, 767));
    }

    @Test
    void playerHeight_pose_longJumping_47() {
        assertEquals(1.8, VersionPhysics.getPlayerHeight(Pose.LONG_JUMPING, 47));
    }
}
