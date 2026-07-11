package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.physics.BoundingBox;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.physics.VersionPhysics;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CompensatedWorld {

    // Only caches changed blocks; unchanged blocks fall through to live world lookup
    private final Map<Long, Material> blockChanges = new ConcurrentHashMap<>();
    private final World world;

    public CompensatedWorld(World world) {
        this.world = world;
    }

    public void onBlockChange(int x, int y, int z, Material material) {
        long key = blockKey(x, y, z);
        blockChanges.put(key, material);
    }

    public void onChunkUnload(int chunkX, int chunkZ) {
        // Key packing mirrors MC internal chunk serialization format
        long chunkKey = chunkKey(chunkX, chunkZ);
        blockChanges.entrySet().removeIf(entry -> {
            long blockKey = entry.getKey();
            int bx = (int) (blockKey >> 38);
            int bz = (int) (blockKey & 0x3FFFFFF);
            int cx = bx >> 4;
            int cz = bz >> 4;
            return ((long) cx << 32 | (cz & 0xFFFFFFFFL)) == chunkKey;
        });
    }

    public Material getBlock(int x, int y, int z) {
        long key = blockKey(x, y, z);
        Material cached = blockChanges.get(key);
        if (cached != null) return cached;

        Block block = world.getBlockAt(x, y, z);
        return block.getType();
    }

    // Friction sampled from the block below the player (y-1), matching MC movement code
    public double getBlockFriction(int x, int y, int z) {
        Material material = getBlock(x, y, z - 1);
        if (material == null) material = getBlock(x, y, z);
        return PhysicsConstants.getBlockFriction(material);
    }

    public boolean isOnClimbable(int x, int y, int z) {
        Material material = getBlock(x, y, z);
        if (material == null) return false;
        switch (material.name()) {
            case "LADDER":
            case "VINE":
            case "TWISTING_VINES":
            case "TWISTING_VINES_PLANT":
            case "WEEPING_VINES":
            case "WEEPING_VINES_PLANT":
            case "SCULK_VEIN":
                return true;
            default:
                return false;
        }
    }

    public boolean isInWater(int x, int y, int z) {
        Material material = getBlock(x, y, z);
        return material != null && material.name().contains("WATER");
    }

    public boolean isInLava(int x, int y, int z) {
        Material material = getBlock(x, y, z);
        return material != null && material.name().contains("LAVA");
    }

    public boolean isOnSlime(int x, int y, int z) {
        Material material = getBlock(x, y - 1, z);
        return material != null && material.name().contains("SLIME");
    }

    public boolean isOnHoney(int x, int y, int z) {
        Material material = getBlock(x, y - 1, z);
        return material != null && material.name().contains("HONEY");
    }

    public boolean isOnWeb(int x, int y, int z) {
        Material material = getBlock(x, y, z);
        if (material == null) return false;
        String name = material.name();
        return name.contains("WEB") || name.equals("STRING");
    }

    public boolean isOnSoulSand(int x, int y, int z) {
        Material material = getBlock(x, y - 1, z);
        if (material == null) return false;
        String name = material.name();
        return name.equals("SOUL_SAND") || name.equals("SOUL_SOIL");
    }

    public boolean isOnIce(int x, int y, int z) {
        Material material = getBlock(x, y - 1, z);
        if (material == null) return false;
        String name = material.name();
        return name.equals("ICE") || name.equals("PACKED_ICE") || name.equals("BLUE_ICE");
    }

    public boolean isBubbleColumn(int x, int y, int z) {
        Material material = getBlock(x, y, z);
        return material != null && material.name().contains("BUBBLE");
    }

    public boolean isPowderSnow(int x, int y, int z) {
        Material material = getBlock(x, y, z);
        return material != null && material.name().equals("POWDER_SNOW");
    }

    // Enumerates all solid blocks in an AABB region for collision detection
    public List<BoundingBox> getCollisionBoxes(BoundingBox box, int protocolVersion) {
        List<BoundingBox> boxes = new ArrayList<>();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material material = getBlock(x, y, z);
                    if (material != null && material.isSolid()) {
                        double stepHeight = VersionPhysics.getStepHeight(protocolVersion);
                        boxes.add(new BoundingBox(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return boxes;
    }

    public World getWorld() {
        return world;
    }

    // Minecraft-style bit-packed coordinates avoid HashMap overhead
    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (long) (z & 0x3FFFFFF);
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }
}
