package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Axis-Aligned Bounding Box (AABB) — the core collision primitive used by all movement checks.
 *
 * <p>Represents an immutable rectangular prism in world space. Used for:
 * <ul>
 *   <li>Collision detection between players and blocks</li>
 *   <li>Hitbox checks in combat (reach, aim)</li>
 *   <li>Phase detection (clipping through walls)</li>
 *   <li>Ray tracing (block breaking, block placing)</li>
 * </ul>
 *
 * <p>All coordinates are in blocks (1 unit = 1 block). The box is defined by its
 * minimum and maximum corners — {@code minY} is always the player's feet, {@code maxY}
 * is the top of the hitbox.
 *
 * <p>Use {@link #fromPlayer} to construct a player hitbox from position and pose.
 *
 * @see PhysicsConstants for PLAYER_WIDTH, PLAYER_HEIGHT_*
 * @see VersionPhysics for version-dependent height calculations
 */
public final class BoundingBox {

    /** Minimum corner (south-west-bottom) of the box */
    public final double minX, minY, minZ;

    /** Maximum corner (north-east-top) of the box */
    public final double maxX, maxY, maxZ;

    /**
     * Creates an axis-aligned bounding box from two opposite corners.
     * Order of min/max doesn't matter — constructor normalises via field assignment.
     */
    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public BoundingBox expand(double x, double y, double z) {
        return new BoundingBox(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
    }

    /** Expands the box uniformly by {@code margin} on all six faces */
    public BoundingBox expand(double margin) {
        return expand(margin, margin, margin);
    }

    /** Translates the box by the given offset — returns a new instance */
    public BoundingBox offset(double x, double y, double z) {
        return new BoundingBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    // Standard AABB overlap: all three axes must overlap for intersection
    public boolean intersects(BoundingBox other) {
        return maxX > other.minX && minX < other.maxX
                && maxY > other.minY && minY < other.maxY
                && maxZ > other.minZ && minZ < other.maxZ;
    }

    /** Returns the X centre of the box (midpoint of minX and maxX) */
    public double getCenterX() { return (minX + maxX) * 0.5; }
    /** Returns the Y centre of the box */
    public double getCenterY() { return (minY + maxY) * 0.5; }
    /** Returns the Z centre of the box */
    public double getCenterZ() { return (minZ + maxZ) * 0.5; }

    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getDepth() { return maxZ - minZ; }

    /** Checks if a 3D point is inside (or on the surface of) this box */
    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /** Checks if a 2D XZ point is inside the box (ignoring Y) — used for horizontal checks */
    public boolean containsXZ(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    // Point-to-box distance: returns 0 if point is inside the box
    public double distanceTo(double x, double y, double z) {
        double dx = Math.max(0, Math.max(minX - x, x - maxX));
        double dy = Math.max(0, Math.max(minY - y, y - maxY));
        double dz = Math.max(0, Math.max(minZ - z, z - maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Alias for {@link #distanceTo(double, double, double)} — kept for readability in check code */
    public double distanceToEdge(double x, double y, double z) {
        return distanceTo(x, y, z);
    }

    // Expands bounding volume to encompass this box AND the two given points
    public BoundingBox addCoords(double x1, double y1, double z1, double x2, double y2, double z2) {
        double nminX = Math.min(minX, Math.min(x1, x2));
        double nminY = Math.min(minY, Math.min(y1, y2));
        double nminZ = Math.min(minZ, Math.min(z1, z2));
        double nmaxX = Math.max(maxX, Math.max(x1, x2));
        double nmaxY = Math.max(maxY, Math.max(y1, y2));
        double nmaxZ = Math.max(maxZ, Math.max(z1, z2));
        return new BoundingBox(nminX, nminY, nminZ, nmaxX, nmaxY, nmaxZ);
    }

    /**
     * Creates a player hitbox at the given position and pose.
     * Width is always 0.6; height varies by pose and protocol version.
     *
     * @param x player X coordinate (feet)
     * @param y player Y coordinate (feet)
     * @param z player Z coordinate (feet)
     * @param pose current player pose (standing, sneaking, swimming, etc.)
     * @param protocol client protocol version number
     * @see VersionPhysics#getPlayerHeight(Pose, int)
     */
    public static BoundingBox fromPlayer(double x, double y, double z, WindfallPlayer.Pose pose, int protocol) {
        double halfWidth = PhysicsConstants.PLAYER_WIDTH * 0.5;
        double height = VersionPhysics.getPlayerHeight(pose, protocol);
        return new BoundingBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
    }

    /** Legacy overload for backwards compatibility — converts boolean to pose internally */
    public static BoundingBox fromPlayer(double x, double y, double z, boolean sneaking, int protocol) {
        double halfWidth = PhysicsConstants.PLAYER_WIDTH * 0.5;
        double height = VersionPhysics.getPlayerHeight(sneaking, protocol);
        return new BoundingBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
    }

    /**
     * Creates a hitbox incorporating look direction for eye-trace checks.
     * Currently ignores yaw/pitch — returns standard hitbox. Kept as placeholder
     * for future ray-based aim checks.
     */
    public static BoundingBox fromPlayerLook(double x, double y, double z, float yaw, float pitch,
                                              boolean sneaking, int protocol) {
        return fromPlayer(x, y, z, sneaking, protocol);
    }
}
