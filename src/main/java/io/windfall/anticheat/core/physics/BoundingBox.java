package io.windfall.anticheat.core.physics;

public final class BoundingBox {

    public final double minX, minY, minZ;
    public final double maxX, maxY, maxZ;

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

    public BoundingBox expand(double margin) {
        return expand(margin, margin, margin);
    }

    public BoundingBox offset(double x, double y, double z) {
        return new BoundingBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    // Standard AABB overlap: all three axes must overlap for intersection
    public boolean intersects(BoundingBox other) {
        return maxX > other.minX && minX < other.maxX
                && maxY > other.minY && minY < other.maxY
                && maxZ > other.minZ && minZ < other.maxZ;
    }

    public double getCenterX() { return (minX + maxX) * 0.5; }
    public double getCenterY() { return (minY + maxY) * 0.5; }
    public double getCenterZ() { return (minZ + maxZ) * 0.5; }

    public double getWidth() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getDepth() { return maxZ - minZ; }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

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

    public static BoundingBox fromPlayer(double x, double y, double z, boolean sneaking, int protocol) {
        double halfWidth = PhysicsConstants.PLAYER_WIDTH * 0.5;
        double height = VersionPhysics.getPlayerHeight(sneaking, protocol);
        return new BoundingBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
    }

    // Currently ignores look direction — needs ray-based variant for eye-trace checks
    public static BoundingBox fromPlayerLook(double x, double y, double z, float yaw, float pitch,
                                              boolean sneaking, int protocol) {
        return fromPlayer(x, y, z, sneaking, protocol);
    }
}
