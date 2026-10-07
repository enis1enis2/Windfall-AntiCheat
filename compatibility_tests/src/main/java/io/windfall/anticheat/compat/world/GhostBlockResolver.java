package io.windfall.anticheat.compat.world;

public final class GhostBlockResolver {

    public interface WorldView {
        boolean isChunkLoaded(int chunkX, int chunkZ);

        boolean isAir(int x, int y, int z);
    }

    private boolean allow;
    private int distance;

    public GhostBlockResolver(boolean allow, int distance) {
        configure(allow, distance);
    }

    public void configure(boolean allow, int distance) {
        this.allow = allow;
        if (distance < 2 || distance > 4) {
            distance = 2;
        }
        this.distance = distance;
    }

    public boolean isEnabled() {
        return !allow;
    }

    public int getDistance() {
        return distance;
    }

    public boolean shouldResync(WorldView world, int x, int y, int z) {
        if (allow) {
            return false;
        }

        for (int i = x - distance; i <= x + distance; i++) {
            for (int j = y - distance; j <= y + distance; j++) {
                for (int k = z - distance; k <= z + distance; k++) {
                    if (i == x && j == y && k == z) {
                        continue;
                    }

                    if (!world.isChunkLoaded(i >> 4, k >> 4)) {
                        continue;
                    }

                    if (!world.isAir(i, j, k)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}