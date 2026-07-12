package io.windfall.anticheat.core.physics;

import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hardcoded Minecraft physics values — the "truth table" for all movement prediction.
 *
 * <p>Every constant here is sourced from decompiled Minecraft server code (Mojang mappings).
 * Values use IEEE 754 double precision to match the exact floating-point arithmetic
 * the vanilla server performs. This is critical: rounding these values will produce
 * false positives in movement checks.
 *
 * <p>Key categories:
 * <ul>
 *   <li><b>Gravity</b>: {@link #GRAVITY} (0.08 blocks/tick²)</li>
 *   <li><b>Drag</b>: {@link #AIR_DRAG}, {@link #WATER_DRAG}, {@link #LAVA_DRAG}</li>
 *   <li><b>Friction</b>: block-dependent via {@link #getBlockFriction(Material)}</li>
 *   <li><b>Player dimensions</b>: {@link #PLAYER_WIDTH}, {@link #PLAYER_HEIGHT_*}</li>
 *   <li><b>Movement speeds</b>: {@link #PLAYER_WALK_SPEED}, {@link #PLAYER_SPRINT_MULTIPLIER}</li>
 *   <li><b>Potion effects</b>: {@link #getJumpBoostVertical}, {@link #getSpeedEffectMultiplier}</li>
 * </ul>
 *
 * <p>The friction map is populated at class load time using string-based Material matching
 * to avoid compile-time dependencies on version-specific material names.
 *
 * @see PredictionEngine for how these constants are used in movement prediction
 * @see VersionPhysics for version-dependent overrides
 */
public final class PhysicsConstants {

    // === CORE MOVEMENT CONSTANTS ===

    /** Gravitational acceleration: 0.08 blocks/tick² (Minecraft server value) */
    public static final double GRAVITY = 0.08;

    /** Upward momentum added when jumping: 0.42 blocks/tick */
    public static final double PLAYER_JUMP_MOMENTUM = 0.42;

    /** Air drag applied each tick: velocity * 0.98 (1.0 - 0.02 air resistance) */
    public static final double AIR_DRAG = 0.98;

    /**
     * Water drag — IEEE 754 double from decompiled MC.
     * DO NOT "round" this to 0.8 — the exact float matters for prediction accuracy.
     */
    public static final double WATER_DRAG = 0.800000011920929;

    /** Lava drag: 50% of velocity retained each tick */
    public static final double LAVA_DRAG = 0.5;

    /** Ground friction — applied when player is standing on a block */
    public static final double GROUND_FRICTION = 0.91;

    // === BLOCK FRICTION VALUES ===

    /** Soul sand friction — significantly reduces movement speed */
    public static final double SOUL_SAND_FRICTION = 0.6;

    /** Ice friction — slightly higher than normal ground, enables sliding */
    public static final double ICE_FRICTION = 0.98;

    /** Slime block friction */
    public static final double SLIME_FRICTION = 0.8;

    /** Honey block friction — heavy slowdown, max deltaY capped at -0.5 */
    public static final double HONEY_FRICTION = 0.4;

    /** Cobweb friction — extreme slowdown (25% of normal movement) */
    public static final double WEB_FRICTION = 0.25;

    // === PLAYER MOVEMENT SPEEDS ===

    /** Base walk speed: 0.1 blocks/tick */
    public static final double PLAYER_WALK_SPEED = 0.1;

    /** Sprint multiplier: walk_speed * 1.3 */
    public static final double PLAYER_SPRINT_MULTIPLIER = 1.3;

    /** Sneak multiplier: walk_speed * 0.3 (applied after sprint) */
    public static final double PLAYER_CROUCH_MULTIPLIER = 0.3;

    /** Minimum movement per tick to consider the player as moving (avoids floating-point noise) */
    public static final double MIN_MOVEMENT_THRESHOLD = 0.003;

    // === PLAYER DIMENSIONS ===

    /** Player hitbox width: always 0.6 blocks (unchanged since 1.0) */
    public static final double PLAYER_WIDTH = 0.6;

    /** Standing player height: 1.8 blocks */
    public static final double PLAYER_HEIGHT_NORMAL = 1.8;

    /** Sneaking player height: 1.5 blocks (1.14+) */
    public static final double PLAYER_HEIGHT_SNEAKING = 1.5;

    /** Eye height when standing: 1.62 blocks */
    public static final double PLAYER_EYE_HEIGHT_NORMAL = 1.62;

    /** Eye height when sneaking: 1.27 blocks (1.14+) */
    public static final double PLAYER_EYE_HEIGHT_SNEAKING = 1.27;

    // === SPECIAL MOVEMENT ===

    /**
     * Swim boost — IEEE 754 double from decompiled MC.
     * Same precision concern as {@link #WATER_DRAG}.
     */
    public static final double SWIM_BOOST = 0.03999999910593033;

    /** Cobweb slowdown applied to horizontal movement */
    public static final double WEB_SLOWDOWN = 0.25;

    /** Ladder climbing speed: 0.15 blocks/tick upward */
    public static final double LADDER_CLIMB_SPEED = 0.15;

    /** Bubble column upward speed — same IEEE value as swim boost */
    public static final double BUBBLE_COLUMN_SPEED = 0.03999999910593033;

    /** Powder snow slowdown multiplier */
    public static final double POWDER_SNOW_SLOWDOWN = 0.9;

    /** Vertical motion applied when swimming downward: -0.02 blocks/tick */
    public static final double WATER_VERTICAL_MOTION = -0.02;

    /** Vertical motion applied when sinking in lava: -0.02 blocks/tick */
    public static final double LAVA_VERTICAL_MOTION = -0.02;

    /** Maximum step-up height: 0.6 blocks (allows walking up slabs without jumping) */
    public static final double STEP_HEIGHT = 0.6;

    /** Horizontal boost from sprint-jumping: +0.2 blocks/tick */
    public static final double SPRINT_JUMP_BOOST_HORIZONTAL = 0.2;

    /**
     * Block friction lookup table — maps Material to friction coefficient.
     * Populated at class load via string-based matching to avoid cross-version import issues.
     */
    private static final Map<Material, Double> FRICTION_MAP = new ConcurrentHashMap<>();

    // String-based matching avoids compile-time Material import dependencies across MC versions
    // Null-safe: Material.matchMaterial returns null for unknown names, addFriction skips them
    static {
        addFriction("SOUL_SAND", SOUL_SAND_FRICTION);
        addFriction("SOUL_SOIL", SOUL_SAND_FRICTION);
        addFriction("HONEY_BLOCK", HONEY_FRICTION);
        addFriction("COBWEB", WEB_FRICTION);
        addFriction("STRING", WEB_FRICTION);
        addFriction("ICE", ICE_FRICTION);
        addFriction("PACKED_ICE", ICE_FRICTION);
        addFriction("BLUE_ICE", ICE_FRICTION);
        addFriction("SLIME_BLOCK", SLIME_FRICTION);
    }

    /**
     * Registers a block friction value by material name.
     * Silently skips materials that don't exist on the running server version.
     */
    private static void addFriction(String materialName, double friction) {
        try {
            Material mat = Material.matchMaterial(materialName);
            if (mat != null) {
                FRICTION_MAP.put(mat, friction);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Returns the friction coefficient for the block at the player's feet.
     * Defaults to {@link #GROUND_FRICTION} (0.91) for unknown/null blocks.
     *
     * @param material the block material the player is standing on
     * @return friction value between 0.0 and 1.0
     */
    public static double getBlockFriction(Material material) {
        if (material == null) return GROUND_FRICTION;
        return FRICTION_MAP.getOrDefault(material, GROUND_FRICTION);
    }

    /**
     * Horizontal boost from Jump Boost potion: 0.1 * (amplifier + 1).
     * Applied as additional horizontal momentum, not a multiplier.
     */
    public static double getJumpBoostHorizontal(int amplifier) {
        return 0.1 * (amplifier + 1);
    }

    /**
     * Vertical boost from Jump Boost potion — version-dependent.
     * Pre-1.9: 0.15 per level. Post-1.9: 0.1 per level.
     *
     * @param amplifier potion amplifier (0-based)
     * @param is19Plus true if client protocol >= 107 (1.9+)
     */
    public static double getJumpBoostVertical(int amplifier, boolean is19Plus) {
        if (is19Plus) {
            return 0.1 * (amplifier + 1);
        }
        return 0.15 * (amplifier + 1);
    }

    /**
     * Speed potion multiplier: 1.0 + 0.2 * (amplifier + 1).
     * Level I = 1.2x, Level II = 1.4x, etc. Max useful level: V (2.0x).
     */
    public static double getSpeedEffectMultiplier(int amplifier) {
        return 1.0 + 0.2 * (amplifier + 1);
    }

    /**
     * Slowness potion multiplier: max(0.0, 1.0 - 0.15 * (amplifier + 1)).
     * Level I = 0.85x, Level II = 0.70x, Level III = 0.55x, Level IV = 0.40x.
     * Capped at 0.0 to prevent negative speed.
     */
    public static double getSlownessEffectMultiplier(int amplifier) {
        return Math.max(0.0, 1.0 - 0.15 * (amplifier + 1));
    }
}
