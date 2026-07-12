package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.player.WindfallPlayer.Pose;
import org.bukkit.Material;

public final class VersionPhysics {

    private static final int PROTOCOL_1_7 = 5;
    private static final int PROTOCOL_1_8 = 47;
    private static final int PROTOCOL_1_9 = 107;
    private static final int PROTOCOL_1_11 = 315;
    private static final int PROTOCOL_1_12 = 340;
    private static final int PROTOCOL_1_13 = 393;
    private static final int PROTOCOL_1_14 = 477;
    private static final int PROTOCOL_1_15 = 573;
    private static final int PROTOCOL_1_16 = 736;
    private static final int PROTOCOL_1_17 = 756;
    private static final int PROTOCOL_1_18 = 757;
    private static final int PROTOCOL_1_18_2 = 758;
    private static final int PROTOCOL_1_19 = 759;
    private static final int PROTOCOL_1_20 = 763;
    private static final int PROTOCOL_1_20_5 = 766;
    private static final int PROTOCOL_1_21 = 767;
    private static final int PROTOCOL_1_21_2 = 768;
    private static final int PROTOCOL_1_21_5 = 770;

    private VersionPhysics() {}

    // === GRAVITY ===

    public static double getGravity(WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        // Gravity changed to 0.08 in 1.21.2; same value but different code path
        if (protocol >= PROTOCOL_1_21_2) {
            return 0.08;
        }
        return PhysicsConstants.GRAVITY;
    }

    // === MOVEMENT SPEED ===

    public static double getBaseMovementSpeed() {
        return PhysicsConstants.PLAYER_WALK_SPEED;
    }

    public static double getSprintMultiplier() {
        return PhysicsConstants.PLAYER_SPRINT_MULTIPLIER;
    }

    public static double getCrouchMultiplier() {
        return PhysicsConstants.PLAYER_CROUCH_MULTIPLIER;
    }

    public static double getGroundFriction(Material material) {
        return PhysicsConstants.getBlockFriction(material);
    }

    public static double getAirDrag() {
        return PhysicsConstants.AIR_DRAG;
    }

    // === PLAYER DIMENSIONS ===

    public static double getPlayerHeight(boolean sneaking, int protocol) {
        if (protocol >= PROTOCOL_1_14) {
            return sneaking ? 1.5 : 1.8;
        }
        return sneaking ? 1.62 : 1.8;
    }

    public static double getPlayerHeight(Pose pose, int protocol) {
        switch (pose) {
            case FALL_FLYING: return 0.6;
            case SWIMMING: return 0.6;
            case SPIN_ATTACK: return 0.6;
            case SLEEPING: return 0.2;
            case DYING: return 0.0;
            case SNEAKING: return protocol >= PROTOCOL_1_14 ? 1.5 : 1.62;
            case LONG_JUMPING: return protocol >= PROTOCOL_1_14 ? 1.5 : 1.8;
            case STANDING:
            default: return 1.8;
        }
    }

    public static double getPlayerEyeHeight(boolean sneaking, int protocol) {
        if (protocol >= PROTOCOL_1_14) {
            return sneaking ? 1.27 : 1.62;
        }
        if (protocol >= PROTOCOL_1_9) {
            return sneaking ? 1.54 : 1.62;
        }
        return sneaking ? 1.54 : 1.62;
    }

    // Player width (0.6) has never changed across versions — method exists for API completeness
    public static double getPlayerWidth(int protocol) {
        return PhysicsConstants.PLAYER_WIDTH;
    }

    public static double getStepHeight(int protocol) {
        if (protocol >= PROTOCOL_1_14) return 0.6;
        if (protocol >= PROTOCOL_1_9) return 0.6;
        return 0.5;
    }

    public static boolean canStepHeightDiffer(int protocol) {
        return protocol >= PROTOCOL_1_14;
    }

    // === REACH ===

    // Pre-1.9 reach was 4 blocks; reduced to 3 in the combat update
    public static double getMaxReach(int protocol) {
        if (protocol < PROTOCOL_1_9) {
            return 4.0;
        }
        return 3.0;
    }

    public static double getSprintReachBonus(int protocol) {
        if (protocol < PROTOCOL_1_9) return 0.0;
        return 0.05;
    }

    public static double getCooldownReachBonus(int protocol) {
        if (protocol < PROTOCOL_1_9) return 0.0;
        return 0.5;
    }

    // === ATTACK ===

    public static boolean hasAttackCooldown(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    public static double getAttackCooldownMultiplier(int protocol, double cooldownTicks) {
        if (protocol < PROTOCOL_1_9) return 1.0;
        double progress = Math.min(cooldownTicks / 20.0, 1.0);
        return 0.2 + 0.8 * progress;
    }

    public static double getSprintCritMultiplier(int protocol) {
        if (protocol >= PROTOCOL_1_9) return 1.1;
        return 1.5;
    }

    // === CRITICAL HITS ===

    public static boolean canCritical(int protocol) {
        return true;
    }

    public static double getCriticalDamageMultiplier(int protocol) {
        return 1.5;
    }

    public static double getSharpnessDamagePerLevel(int protocol) {
        if (protocol >= PROTOCOL_1_9) return 0.5;
        return 1.0;
    }

    // === FLUID SYSTEM ===

    public static boolean hasNewFluidSystem(int protocol) {
        return protocol >= PROTOCOL_1_13;
    }

    public static double getWaterDrag(int protocol) {
        return PhysicsConstants.WATER_DRAG;
    }

    public static double getLavaDrag(int protocol) {
        return PhysicsConstants.LAVA_DRAG;
    }

    public static double getSwimBoost(int protocol) {
        if (protocol >= PROTOCOL_1_13) return 0.04;
        return 0.03;
    }

    // === WORLD HEIGHT ===

    public static boolean hasWorldHeightExpansion(int protocol) {
        return protocol >= PROTOCOL_1_18;
    }

    // 1.18 introduced extended world height: -64 to 320 (was 0 to 256)
    public static int getMinWorldHeight(int protocol) {
        if (protocol >= PROTOCOL_1_18) return -64;
        return 0;
    }

    public static int getMaxWorldHeight(int protocol) {
        if (protocol >= PROTOCOL_1_18) return 320;
        if (protocol >= PROTOCOL_1_17) return 256;
        return 256;
    }

    // === ENTITY INTERACTION ===

    public static double getEntityInteractionRange(int protocol) {
        if (protocol < PROTOCOL_1_9) return 4.0;
        if (protocol < PROTOCOL_1_20_5) return 3.0;
        return 3.0;
    }

    public static boolean hasSprintBlocking(int protocol) {
        return protocol < PROTOCOL_1_9;
    }

    public static boolean hasShieldBlocking(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    public static boolean hasAutoAttack(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    // === COMBAT SPECIFICS ===

    public static double getBowChargeSpeed(int protocol) {
        return 1.0;
    }

    public static boolean hasSwordBlocking(int protocol) {
        return protocol < PROTOCOL_1_9;
    }

    public static double getSwordBlockDamageReduction(int protocol) {
        if (protocol < PROTOCOL_1_9) return 0.5;
        return 0.0;
    }

    // === ELYTRA / TRIDENT ===

    public static boolean hasElytra(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    public static boolean hasRiptide(int protocol) {
        return protocol >= PROTOCOL_1_13;
    }

    public static double getElytraFallSpeed(int protocol) {
        return 0.01;
    }

    // === MOVEMENT FEATURES ===

    // Exists for API symmetry only — sprinting has been in MC since 1.0
    public static boolean hasSprinting(int protocol) {
        return true;
    }

    public static boolean hasCrouching(int protocol) {
        return true;
    }

    public static double getBobbingVerticalBoost(int protocol) {
        if (protocol >= PROTOCOL_1_13) return 0.04;
        return 0.0;
    }

    public static boolean hasAutoJump(int protocol) {
        return protocol >= PROTOCOL_1_13;
    }

    // === BOAT PHYSICS ===

    public static double getBoatMaxSpeed(int protocol) {
        return 0.4;
    }

    public static boolean hasBoatFly(int protocol) {
        return protocol >= PROTOCOL_1_9 && protocol < PROTOCOL_1_14;
    }

    // === PISTON / BLOCK CLIPPING ===

    public static double getPistonPushSpeed(int protocol) {
        return 0.2;
    }

    public static boolean hasPistonClipping(int protocol) {
        return protocol <= PROTOCOL_1_8;
    }

    // === VERSION HELPERS ===

    public static boolean isLegacyProtocol(int protocol) {
        return protocol <= PROTOCOL_1_8;
    }

    public static boolean isModernProtocol(int protocol) {
        return protocol >= PROTOCOL_1_20_5;
    }

    public static boolean hasInputPackets(int protocol) {
        return protocol >= PROTOCOL_1_21_5;
    }

    public static boolean isPreCombatUpdate(int protocol) {
        return protocol < PROTOCOL_1_9;
    }

    public static boolean isPreFlattening(int protocol) {
        return protocol < PROTOCOL_1_13;
    }

    public static boolean isPreWorldHeight(int protocol) {
        return protocol < PROTOCOL_1_18;
    }
}
