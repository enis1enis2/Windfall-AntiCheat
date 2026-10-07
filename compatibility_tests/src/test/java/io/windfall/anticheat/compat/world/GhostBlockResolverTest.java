package io.windfall.anticheat.compat.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhostBlockResolverTest {

    private static class CountingWorld implements GhostBlockResolver.WorldView {
        final Set<Integer> unloadedChunks = new HashSet<>();
        final Set<Long> solidBlocks = new HashSet<>();
        int scannedBlocks;

        private static long key(int x, int y, int z) {
            return ((long) x << 42) ^ ((long) y << 21) ^ (long) z;
        }

        @Override
        public boolean isChunkLoaded(int chunkX, int chunkZ) {
            return !unloadedChunks.contains(chunkX) && !unloadedChunks.contains(chunkZ);
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            scannedBlocks++;
            return !solidBlocks.contains(key(x, y, z));
        }
    }

    @Test
    void allowModeDisablesResync() {
        GhostBlockResolver resolver = new GhostBlockResolver(true, 2);
        assertFalse(resolver.isEnabled());

        CountingWorld world = new CountingWorld();
        assertFalse(resolver.shouldResync(world, 0, 64, 0));
        assertEquals(0, world.scannedBlocks);
    }

    @Test
    void distanceOutsideRangeResetsToTwo() {
        GhostBlockResolver tooSmall = new GhostBlockResolver(false, 1);
        assertEquals(2, tooSmall.getDistance());

        GhostBlockResolver tooLarge = new GhostBlockResolver(false, 5);
        assertEquals(2, tooLarge.getDistance());
    }

    @Test
    void allAirNeighborhoodResyncs() {
        GhostBlockResolver resolver = new GhostBlockResolver(false, 2);
        CountingWorld world = new CountingWorld();

        assertTrue(resolver.shouldResync(world, 10, 64, 10));

        int cube = (2 * 2 + 1) * (2 * 2 + 1) * (2 * 2 + 1);
        assertEquals(cube - 1, world.scannedBlocks, "center block is skipped");
    }

    @Test
    void oneSolidNeighborBlocksResync() {
        GhostBlockResolver resolver = new GhostBlockResolver(false, 2);
        CountingWorld world = new CountingWorld();
        world.solidBlocks.add(CountingWorld.key(11, 64, 10));

        assertFalse(resolver.shouldResync(world, 10, 64, 10));
    }

    @Test
    void distanceFourScansFullCube() {
        GhostBlockResolver resolver = new GhostBlockResolver(false, 4);
        CountingWorld world = new CountingWorld();

        assertTrue(resolver.shouldResync(world, 0, 64, 0));

        int cube = (2 * 4 + 1) * (2 * 4 + 1) * (2 * 4 + 1);
        assertEquals(cube - 1, world.scannedBlocks);
    }

    @Test
    void unloadedChunksAreSkipped() {
        GhostBlockResolver resolver = new GhostBlockResolver(false, 2);
        CountingWorld world = new CountingWorld();
        world.unloadedChunks.add(2);

        assertTrue(resolver.shouldResync(world, 30, 64, 30));

        int cube = (2 * 2 + 1) * (2 * 2 + 1) * (2 * 2 + 1);
        int xInChunk2 = 0;
        for (int x = 28; x <= 32; x++) {
            if (x >> 4 == 2) {
                xInChunk2++;
            }
        }
        int zInChunk2 = xInChunk2;
        int perYLayer = (xInChunk2 * 5) + (zInChunk2 * 5) - (xInChunk2 * zInChunk2);
        int inUnloadedChunk = perYLayer * 5;

        assertEquals(cube - 1 - inUnloadedChunk, world.scannedBlocks);
    }

    @Test
    void recentConfigurationTakesEffect() {
        GhostBlockResolver resolver = new GhostBlockResolver(false, 2);
        CountingWorld world = new CountingWorld();
        world.solidBlocks.add(CountingWorld.key(3, 64, 0));

        assertTrue(resolver.shouldResync(world, 0, 64, 0), "solid at distance 3 is outside the radius-2 scan");

        resolver.configure(false, 4);
        assertEquals(4, resolver.getDistance());
        assertFalse(resolver.shouldResync(world, 0, 64, 0), "wider radius 4 now covers the solid block");
    }
}