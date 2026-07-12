package io.windfall.anticheat.core.util;

import org.bukkit.Material;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cached material classification utility.
 * Replaces scattered string-matching with centralized, cached lookups.
 */
public final class MaterialUtils {

    private static final Map<Material, Boolean> COLLISION_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> FLUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> CLIMBABLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> ICE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> SLIME_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> HONEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> WEB_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> SOUL_SAND_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> BUBBLE_COLUMN_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> POWDER_SNOW_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> REPLACABLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Material, Boolean> NON_FULL_SHAPE_CACHE = new ConcurrentHashMap<>();

    private MaterialUtils() {}

    // Core classification methods with caching

    public static boolean isFluid(Material material) {
        if (material == null || material == Material.AIR) return false;
        return FLUID_CACHE.computeIfAbsent(material, m -> {
            String name = normalize(m.name());
            return name.equals("WATER") || name.equals("LAVA")
                || name.equals("STATIONARY_WATER") || name.equals("STATIONARY_LAVA");
        });
    }

    public static boolean isWater(Material material) {
        if (material == null) return false;
        String name = normalize(material.name());
        return name.equals("WATER") || name.equals("STATIONARY_WATER");
    }

    public static boolean isLava(Material material) {
        if (material == null) return false;
        String name = normalize(material.name());
        return name.equals("LAVA") || name.equals("STATIONARY_LAVA");
    }

    public static boolean hasCollision(Material material) {
        if (material == null || material == Material.AIR) return false;
        return COLLISION_CACHE.computeIfAbsent(material, m -> {
            if (isFluid(m) || isAirLike(m)) return false;
            try {
                return m.isSolid();
            } catch (Throwable e) {
                return false;
            }
        });
    }

    public static boolean isClimbable(Material material) {
        if (material == null) return false;
        return CLIMBABLE_CACHE.computeIfAbsent(material, m -> {
            String name = normalize(m.name());
            return name.contains("LADDER") || name.contains("VINE")
                || name.contains("SCAFFOLDING") || name.contains("TWISTING_VINES")
                || name.contains("WEEPING_VINES") || name.equals("KELP")
                || name.equals("KELP_PLANT") || name.equals("SOUL_SAND");
        });
    }

    public static boolean isIce(Material material) {
        if (material == null) return false;
        return ICE_CACHE.computeIfAbsent(material, m -> {
            String name = normalize(m.name());
            return name.equals("ICE") || name.equals("PACKED_ICE")
                || name.equals("BLUE_ICE") || name.equals("FROSTED_ICE");
        });
    }

    public static boolean isSlime(Material material) {
        if (material == null) return false;
        return SLIME_CACHE.computeIfAbsent(material, m -> {
            String name = normalize(m.name());
            return name.equals("SLIME_BLOCK") || name.equals("HONEY_BLOCK");
        });
    }

    public static boolean isHoney(Material material) {
        if (material == null) return false;
        return HONEY_CACHE.computeIfAbsent(material, m -> {
            String name = normalize(m.name());
            return name.equals("HONEY_BLOCK") || name.equals("HONEY_BLOCK_BLOCK");
        });
    }

    public static boolean isWeb(Material material) {
        if (material == null) return false;
        return WEB_CACHE.computeIfAbsent(material, m -> {
            String name = normalize(m.name());
            return name.equals("COBWEB") || name.equals("WEB")
                || name.equals("STRING");
        });
    }

    public static boolean isSoulSand(Material material) {
        if (material == null) return false;
        return SOUL_SAND_CACHE.computeIfAbsent(material, m ->
            normalize(m.name()).equals("SOUL_SAND")
        );
    }

    public static boolean isBubbleColumn(Material material) {
        if (material == null) return false;
        return BUBBLE_COLUMN_CACHE.computeIfAbsent(material, m ->
            normalize(m.name()).equals("BUBBLE_COLUMN")
        );
    }

    public static boolean isPowderSnow(Material material) {
        if (material == null) return false;
        return POWDER_SNOW_CACHE.computeIfAbsent(material, m ->
            normalize(m.name()).equals("POWDER_SNOW")
        );
    }

    public static boolean isReplaceable(Material material) {
        if (material == null || material == Material.AIR) return true;
        return REPLACABLE_CACHE.computeIfAbsent(material, m -> {
            if (isFluid(m)) return true;
            try {
                return m == Material.AIR || !m.isSolid();
            } catch (Throwable e) {
                String name = normalize(m.name());
                return isAirLike(name) || isFluid(name);
            }
        });
    }

    public static boolean isNonFullShape(Material material) {
        if (material == null || material == Material.AIR) return false;
        return NON_FULL_SHAPE_CACHE.computeIfAbsent(material, m -> {
            if (!hasCollision(m)) return false;
            String name = normalize(m.name());
            return name.endsWith("_STAIRS") || name.endsWith("_SLAB")
                || name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE")
                || name.endsWith("_WALL") || name.endsWith("_PANE")
                || name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR")
                || name.endsWith("_PRESSURE_PLATE") || name.endsWith("_CARPET")
                || name.equals("LANTERN") || name.endsWith("_LANTERN")
                || name.equals("CHAIN") || name.endsWith("_CHAIN")
                || name.equals("IRON_BARS") || name.equals("IRON_FENCE")
                || name.equals("CAULDRON") || name.endsWith("_CAULDRON")
                || name.equals("COMPOSTER") || name.equals("SOUL_SAND")
                || name.equals("SNOW") || name.equals("LILY_PAD");
        });
    }

    // Utility methods

    public static boolean isAirLike(Material material) {
        if (material == null) return false;
        return material == Material.AIR || material.name().contains("AIR");
    }

    private static boolean isAirLike(String name) {
        return name.equals("AIR") || name.equals("CAVE_AIR")
            || name.equals("VOID_AIR") || name.endsWith(":AIR");
    }

    private static boolean isFluid(String name) {
        return name.equals("WATER") || name.equals("LAVA")
            || name.equals("STATIONARY_WATER") || name.equals("STATIONARY_LAVA");
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        String name = raw.trim();
        int namespace = name.indexOf(':');
        if (namespace >= 0) {
            name = name.substring(namespace + 1);
        }
        return name.toUpperCase(Locale.ROOT);
    }

    // Clear caches (call on reload)
    public static void clearCaches() {
        COLLISION_CACHE.clear();
        FLUID_CACHE.clear();
        CLIMBABLE_CACHE.clear();
        ICE_CACHE.clear();
        SLIME_CACHE.clear();
        HONEY_CACHE.clear();
        WEB_CACHE.clear();
        SOUL_SAND_CACHE.clear();
        BUBBLE_COLUMN_CACHE.clear();
        POWDER_SNOW_CACHE.clear();
        REPLACABLE_CACHE.clear();
        NON_FULL_SHAPE_CACHE.clear();
    }
}
