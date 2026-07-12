package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.player.WindfallPlayer.Pose;
import org.bukkit.Material;

/**
 * Version-dependent physics values — handles all Minecraft version differences.
 *
 * <p>Minecraft's physics engine has changed significantly across versions. This class
 * centralises every version-specific difference so checks don't need to branch on
 * protocol numbers themselves.
 *
 * <p>Key version breakpoints:
 * <ul>
 *   <li><b>1.7-1.8</b> (protocol ≤47): no attack cooldown, 4-block reach, 1.62 sneaking height</li>
 *   <li><b>1.9</b> (107): combat update — 3-block reach, attack cooldown, shields, elytra</li>
 *   <li><b>1.13</b> (393): flattening — new fluid system, riptide, swim boost 0.04</li>
 *   <li><b>1.14</b> (477): sneaking height changed to 1.5, step height 0.6</li>
 *   <li><b>1.18</b> (757): world height expanded (-64 to 320)</li>
 *   <li><b>1.21.2</b> (768): gravity code path refactored (value unchanged)</li>
 *   <li><b>1.21.5</b> (770): new input packets</li>
 * </ul>
 *
 * <p>Protocol version numbers are from {@link com.github.retrooper.packetevents.protocol.player.ClientVersion}.
 *
 * @see PhysicsConstants for absolute values that never change
 * @see PredictionEngine for how these values are consumed
 */
public final class VersionPhysics {

    // === PROTOCOL VERSION CONSTANTS ===
    // These map protocol numbers to MC versions — used for branching

    private static final int PROTOCOL_1_7 = 5;
    private static final int PROTOCOL_1_8 = 47;
    private static final int PROTOCOL_1_9 = 107;      // Combat update
    private static final int PROTOCOL_1_11 = 315;
    private static final int PROTOCOL_1_12 = 340;
    private static final int PROTOCOL_1_13 = 393;      // Flattening
    private static final int PROTOCOL_1_14 = 477;      // Sneaking height change
    private static final int PROTOCOL_1_15 = 573;
    private static final int PROTOCOL_1_16 = 736;
    private static final int PROTOCOL_1_17 = 756;
    private static final int PROTOCOL_1_18 = 757;      // World height expansion
    private static final int PROTOCOL_1_18_2 = 758;
    private static final int PROTOCOL_1_19 = 759;
    private static final int PROTOCOL_1_20 = 763;
    private static final int PROTOCOL_1_20_5 = 766;    // Modern protocol threshold
    private static final int PROTOCOL_1_21 = 767;
    private static final int PROTOCOL_1_21_2 = 768;    // Gravity code path change
    private static final int PROTOCOL_1_21_5 = 770;    // New input packets

    private VersionPhysics() {}

    // === GRAVITY ===

    /**
     * Returns the gravity value for this protocol version.
     * 1.21.2+ uses the same value (0.08) but via a different code path in the server.
     */
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

    /**
     * Returns player hitbox height based on sneaking state and protocol version.
     * 1.14+: sneaking height is 1.5 (was 1.62 pre-1.14).
     */
    public static double getPlayerHeight(boolean sneaking, int protocol) {
        if (protocol >= PROTOCOL_1_14) {
            return sneaking ? 1.5 : 1.8;
        }
        return sneaking ? 1.62 : 1.8;
    }

    /**
     * Returns player hitbox height for all poses.
     * Added in 1.14+ — swimming/flying poses are 0.6, sleeping is 0.2, dying is 0.0.
     * Pre-1.14 only had standing (1.8) and sneaking (1.62).
     */
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

    /**
     * Returns player eye height for camera/tracing calculations.
     * Version-dependent: 1.14+ (1.27 sneaking / 1.62 standing),
     * 1.9-1.13 (1.54 sneaking / 1.62 standing), pre-1.9 (1.54 sneaking / 1.62 standing).
     */
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

    /** Returns the block step-up height: 0.5 (pre-1.9), 0.6 (1.9+). Allows walking up slabs/stairs. */
    public static double getStepHeight(int protocol) {
        if (protocol >= PROTOCOL_1_14) return 0.6;
        if (protocol >= PROTOCOL_1_9) return 0.6;
        return 0.5;
    }

    /** Whether the client supports per-block step height differences (1.14+) */
    public static boolean canStepHeightDiffer(int protocol) {
        return protocol >= PROTOCOL_1_14;
    }

    // === REACH ===

    /**
     * Maximum interaction reach: 4 blocks (pre-1.9), 3 blocks (1.9+).
     * The 1.9 combat update reduced reach to balance the new cooldown system.
     */
    public static double getMaxReach(int protocol) {
        if (protocol < PROTOCOL_1_9) {
            return 4.0;
        }
        return 3.0;
    }

    /** Extra reach when sprinting: 0.05 blocks (1.9+), 0.0 (pre-1.9) */
    public static double getSprintReachBonus(int protocol) {
        if (protocol < PROTOCOL_1_9) return 0.0;
        return 0.05;
    }

    /** Extra reach from attack cooldown completion: 0.5 blocks (1.9+), 0.0 (pre-1.9) */
    public static double getCooldownReachBonus(int protocol) {
        if (protocol < PROTOCOL_1_9) return 0.0;
        return 0.5;
    }

    // === ATTACK ===

    /** Whether this version has the attack cooldown system (1.9+) */
    public static boolean hasAttackCooldown(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    /**
     * Damage multiplier based on attack cooldown progress (1.9+).
     * Formula: 0.2 + 0.8 × min(ticks/20, 1.0)
     * Full damage at 20 ticks (1 second), minimum 20% damage when spam-clicking.
     */
    public static double getAttackCooldownMultiplier(int protocol, double cooldownTicks) {
        if (protocol < PROTOCOL_1_9) return 1.0;
        double progress = Math.min(cooldownTicks / 20.0, 1.0);
        return 0.2 + 0.8 * progress;
    }

    /** Sprint critical hit bonus: 1.1x (1.9+), 1.5x (pre-1.9) */
    public static double getSprintCritMultiplier(int protocol) {
        if (protocol >= PROTOCOL_1_9) return 1.1;
        return 1.5;
    }

    // === CRITICAL HITS ===

    /** Critical hits have been possible since 1.0 — always returns true */
    public static boolean canCritical(int protocol) {
        return true;
    }

    /** Base critical hit damage multiplier: always 1.5x */
    public static double getCriticalDamageMultiplier(int protocol) {
        return 1.5;
    }

    /** Sharpness damage per level: 0.5 (1.9+), 1.0 (pre-1.9) — changed in combat update */
    public static double getSharpnessDamagePerLevel(int protocol) {
        if (protocol >= PROTOCOL_1_9) return 0.5;
        return 1.0;
    }

    // === FLUID SYSTEM ===

    /** Whether the server uses the new fluid system (1.13+ flattening) */
    public static boolean hasNewFluidSystem(int protocol) {
        return protocol >= PROTOCOL_1_13;
    }

    public static double getWaterDrag(int protocol) {
        return PhysicsConstants.WATER_DRAG;
    }

    public static double getLavaDrag(int protocol) {
        return PhysicsConstants.LAVA_DRAG;
    }

    /** Swim boost when swimming upward: 0.04 (1.13+), 0.03 (pre-1.13) */
    public static double getSwimBoost(int protocol) {
        if (protocol >= PROTOCOL_1_13) return 0.04;
        return 0.03;
    }

    // === WORLD HEIGHT ===

    /** Whether the world height was expanded (1.18+): -64 to 320 (was 0 to 256) */
    public static boolean hasWorldHeightExpansion(int protocol) {
        return protocol >= PROTOCOL_1_18;
    }

    // 1.18 introduced extended world height: -64 to 320 (was 0 to 256)
    /** Minimum world height: -64 (1.18+), 0 (pre-1.18) */
    public static int getMinWorldHeight(int protocol) {
        if (protocol >= PROTOCOL_1_18) return -64;
        return 0;
    }

    /** Maximum world height: 320 (1.18+), 256 (pre-1.18) */
    public static int getMaxWorldHeight(int protocol) {
        if (protocol >= PROTOCOL_1_18) return 320;
        if (protocol >= PROTOCOL_1_17) return 256;
        return 256;
    }

    // === ENTITY INTERACTION ===

    /** Entity interaction range: 4.0 (pre-1.9), 3.0 (1.9+) */
    public static double getEntityInteractionRange(int protocol) {
        if (protocol < PROTOCOL_1_9) return 4.0;
        if (protocol < PROTOCOL_1_20_5) return 3.0;
        return 3.0;
    }

    /** Whether sprint-blocking exists (pre-1.9 only — removed in combat update) */
    public static boolean hasSprintBlocking(int protocol) {
        return protocol < PROTOCOL_1_9;
    }

    /** Whether shields exist (1.9+ combat update) */
    public static boolean hasShieldBlocking(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    /** Whether auto-attack exists (1.9+ — attack cooldown system) */
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

    /** Sword block damage reduction: 50% (pre-1.9), 0% (1.9+ — shields replace blocking) */
    public static double getSwordBlockDamageReduction(int protocol) {
        if (protocol < PROTOCOL_1_9) return 0.5;
        return 0.0;
    }

    // === ELYTRA / TRIDENT ===

    /** Whether elytra exist (1.9+) */
    public static boolean hasElytra(int protocol) {
        return protocol >= PROTOCOL_1_9;
    }

    /** Whether Riptide tridents exist (1.13+) */
    public static boolean hasRiptide(int protocol) {
        return protocol >= PROTOCOL_1_13;
    }

    /** Elytra fall speed (when gliding without input): 0.01 blocks/tick */
    public static double getElytraFallSpeed(int protocol) {
        return 0.01;
    }

    // === MOVEMENT FEATURES ===

    /** Whether sprinting exists — always true (exists since 1.0, kept for API symmetry) */
    public static boolean hasSprinting(int protocol) {
        return true;
    }

    public static boolean hasCrouching(int protocol) {
        return true;
    }

    /** Water/lava bobbing vertical boost: 0.04 (1.13+), 0.0 (pre-1.13) */
    public static double getBobbingVerticalBoost(int protocol) {
        if (protocol >= PROTOCOL_1_13) return 0.04;
        return 0.0;
    }

    /** Whether auto-jump is enabled by default (1.13+) */
    public static boolean hasAutoJump(int protocol) {
        return protocol >= PROTOCOL_1_13;
    }

    // === BOAT PHYSICS ===

    /** Maximum boat speed: 0.4 blocks/tick */
    public static double getBoatMaxSpeed(int protocol) {
        return 0.4;
    }

    /** Boat fly exploit exists in 1.9-1.13 only (fixed in 1.14) */
    public static boolean hasBoatFly(int protocol) {
        return protocol >= PROTOCOL_1_9 && protocol < PROTOCOL_1_14;
    }

    // === PISTON / BLOCK CLIPPING ===

    /** Piston push speed: 0.2 blocks/tick */
    public static double getPistonPushSpeed(int protocol) {
        return 0.2;
    }

    /** Piston clipping exploit exists in 1.8 and below */
    public static boolean hasPistonClipping(int protocol) {
        return protocol <= PROTOCOL_1_8;
    }

    // === VERSION HELPERS ===

    /** Whether this is a legacy protocol (1.8 or below) */
    public static boolean isLegacyProtocol(int protocol) {
        return protocol <= PROTOCOL_1_8;
    }

    /** Whether this is a modern protocol (1.20.5+) */
    public static boolean isModernProtocol(int protocol) {
        return protocol >= PROTOCOL_1_20_5;
    }

    /** Whether this version uses new input packets (1.21.5+) */
    public static boolean hasInputPackets(int protocol) {
        return protocol >= PROTOCOL_1_21_5;
    }

    /** Whether this is pre-combat update (before 1.9) */
    public static boolean isPreCombatUpdate(int protocol) {
        return protocol < PROTOCOL_1_9;
    }

    /** Whether this is pre-flattening (before 1.13) */
    public static boolean isPreFlattening(int protocol) {
        return protocol < PROTOCOL_1_13;
    }

    /** Whether this is pre-world-height expansion (before 1.18) */
    public static boolean isPreWorldHeight(int protocol) {
        return protocol < PROTOCOL_1_18;
    }
}
