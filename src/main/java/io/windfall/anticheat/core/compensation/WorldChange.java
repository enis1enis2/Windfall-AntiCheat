package io.windfall.anticheat.core.compensation;

import org.bukkit.Material;

/**
 * Represents a single world change that may affect player movement simulation.
 *
 * <p>Each change is tagged with the server tick when it occurred, enabling the
 * {@link SimulationEngine} to replay changes that the client may not have seen yet.
 *
 * <p>Change types cover all world modifications that affect movement physics:
 * <ul>
 *   <li>{@link Type#BLOCK_BREAK} — block removed, affects ground support</li>
 *   <li>{@link Type#BLOCK_PLACE} — block placed, creates new surface</li>
 *   <li>{@link Type#BLOCK_SHIFT} — piston push, block moved from old to new position</li>
 *   <li>{@link Type#VELOCITY} — server-sent velocity (knockback, explosion, pearl)</li>
 *   <li>{@link Type#POTION_EFFECT} — potion applied/expired, modifies gravity or drag</li>
 * </ul>
 *
 * <p>All fields are final — changes are immutable once created.
 *
 * @see SimulationEngine for how changes are replayed during movement validation
 * @see LatencyCompensator for how changes are recorded and tracked per player
 */
public final class WorldChange {

    /** The type of world modification */
    public enum Type {
        /** Block removed (became air) — may remove ground support */
        BLOCK_BREAK,
        /** Block placed — creates a new solid surface */
        BLOCK_PLACE,
        /** Piston pushed a block from one position to another */
        BLOCK_SHIFT,
        /** Server-sent velocity (knockback, explosion, ender pearl) */
        VELOCITY,
        /** Potion effect applied or expired — modifies gravity or drag */
        POTION_EFFECT
    }

    private final Type type;
    private final int tick;

    // Block position data (BLOCK_BREAK, BLOCK_PLACE, BLOCK_SHIFT target)
    private final int blockX, blockY, blockZ;
    private final Material blockMaterial;

    // BLOCK_SHIFT source position
    private final int oldX, oldY, oldZ;

    // VELOCITY data
    private final double velocityX, velocityY, velocityZ;

    // POTION_EFFECT modifiers
    private final double gravityMod;
    private final double airDragMod;

    private WorldChange(Type type, int tick,
                        int blockX, int blockY, int blockZ, Material blockMaterial,
                        int oldX, int oldY, int oldZ,
                        double velocityX, double velocityY, double velocityZ,
                        double gravityMod, double airDragMod) {
        this.type = type;
        this.tick = tick;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockMaterial = blockMaterial;
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldZ = oldZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.gravityMod = gravityMod;
        this.airDragMod = airDragMod;
    }

    /**
     * Creates a block break change.
     * The block at (x,y,z) was removed (became air).
     */
    public static WorldChange blockBreak(int tick, int x, int y, int z) {
        return new WorldChange(Type.BLOCK_BREAK, tick, x, y, z, Material.AIR, 0, 0, 0, 0, 0, 0, 1.0, 1.0);
    }

    /**
     * Creates a block place change.
     * A block of the given material was placed at (x,y,z).
     */
    public static WorldChange blockPlace(int tick, int x, int y, int z, Material material) {
        return new WorldChange(Type.BLOCK_PLACE, tick, x, y, z, material, 0, 0, 0, 0, 0, 0, 1.0, 1.0);
    }

    /**
     * Creates a block shift change (piston push).
     * A block moved from (oldX,oldY,oldZ) to (newX,newY,newZ).
     */
    public static WorldChange blockShift(int tick, int oldX, int oldY, int oldZ,
                                         int newX, int newY, int newZ, Material material) {
        return new WorldChange(Type.BLOCK_SHIFT, tick, newX, newY, newZ, material, oldX, oldY, oldZ, 0, 0, 0, 1.0, 1.0);
    }

    /**
     * Creates a velocity change.
     * Server sent knockback/explosion/pearl velocity to the player.
     */
    public static WorldChange velocity(int tick, double vx, double vy, double vz) {
        return new WorldChange(Type.VELOCITY, tick, 0, 0, 0, null, 0, 0, 0, vx, vy, vz, 1.0, 1.0);
    }

    /**
     * Creates a potion effect change.
     * Gravity and/or air drag modifiers apply until the next change of this type.
     */
    public static WorldChange potionEffect(int tick, double gravityMod, double airDragMod) {
        return new WorldChange(Type.POTION_EFFECT, tick, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, gravityMod, airDragMod);
    }

    public Type getType() { return type; }
    public int getTick() { return tick; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public Material getBlockMaterial() { return blockMaterial; }
    public int getOldX() { return oldX; }
    public int getOldY() { return oldY; }
    public int getOldZ() { return oldZ; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public double getGravityMod() { return gravityMod; }
    public double getAirDragMod() { return airDragMod; }
}
