package io.windfall.anticheat.core.physics;

import org.bukkit.Material;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PhysicsConstants {

    public static final double GRAVITY = 0.08;
    public static final double PLAYER_JUMP_MOMENTUM = 0.42;
    public static final double AIR_DRAG = 0.98;
    // Exact IEEE float from decompiled MC — do not "round" this value
    public static final double WATER_DRAG = 0.800000011920929;
    public static final double LAVA_DRAG = 0.5;
    public static final double GROUND_FRICTION = 0.91;
    public static final double SOUL_SAND_FRICTION = 0.6;
    public static final double ICE_FRICTION = 0.98;
    public static final double SLIME_FRICTION = 0.8;
    public static final double HONEY_FRICTION = 0.4;
    public static final double WEB_FRICTION = 0.25;
    public static final double PLAYER_WALK_SPEED = 0.1;
    public static final double PLAYER_SPRINT_MULTIPLIER = 1.3;
    public static final double PLAYER_CROUCH_MULTIPLIER = 0.3;
    public static final double MIN_MOVEMENT_THRESHOLD = 0.003;
    public static final double PLAYER_WIDTH = 0.6;
    public static final double PLAYER_HEIGHT_NORMAL = 1.8;
    public static final double PLAYER_HEIGHT_SNEAKING = 1.5;
    public static final double PLAYER_EYE_HEIGHT_NORMAL = 1.62;
    public static final double PLAYER_EYE_HEIGHT_SNEAKING = 1.27;
    // Same IEEE float precision issue as WATER_DRAG — raw decompiled value
    public static final double SWIM_BOOST = 0.03999999910593033;
    public static final double WEB_SLOWDOWN = 0.25;
    public static final double LADDER_CLIMB_SPEED = 0.15;
    public static final double BUBBLE_COLUMN_SPEED = 0.03999999910593033;
    public static final double POWDER_SNOW_SLOWDOWN = 0.9;
    public static final double WATER_VERTICAL_MOTION = -0.02;
    public static final double LAVA_VERTICAL_MOTION = -0.02;
    public static final double STEP_HEIGHT = 0.6;
    public static final double SPRINT_JUMP_BOOST_HORIZONTAL = 0.2;

    private static final Map<Material, Double> FRICTION_MAP = new ConcurrentHashMap<>();

    // String-based matching avoids compile-time Material import dependencies across MC versions
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

    private static void addFriction(String materialName, double friction) {
        try {
            Material mat = Material.matchMaterial(materialName);
            if (mat != null) {
                FRICTION_MAP.put(mat, friction);
            }
        } catch (Exception ignored) {
        }
    }

    public static double getBlockFriction(Material material) {
        if (material == null) return GROUND_FRICTION;
        return FRICTION_MAP.getOrDefault(material, GROUND_FRICTION);
    }

    public static double getJumpBoostHorizontal(int amplifier) {
        return 0.1 * (amplifier + 1);
    }

    // Pre-1.9 jump boost: 0.15/level; post-1.9: 0.1/level
    public static double getJumpBoostVertical(int amplifier, boolean is19Plus) {
        if (is19Plus) {
            return 0.1 * (amplifier + 1);
        }
        return 0.15 * (amplifier + 1);
    }

    public static double getSpeedEffectMultiplier(int amplifier) {
        return 1.0 + 0.2 * (amplifier + 1);
    }

    public static double getSlownessEffectMultiplier(int amplifier) {
        return Math.max(0.0, 1.0 - 0.15 * (amplifier + 1));
    }
}
