package io.windfall.anticheat.core.check.combat;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import io.windfall.anticheat.core.check.impl.combat.ReachCheck;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Verifies the shared entity cache that {@code ReachCheck} uses to resolve attack targets.
 *
 * <p>The cache used to be populated by nothing: {@code trackSpawn}/{@code trackMove}/
 * {@code trackRemove} had no callers, so every attack packet resolved to an unknown target and
 * the check returned before taking a sample. These tests pin the cache behaviour that the packet
 * listener's entity tracking now depends on.
 */
class ReachEntityTrackingTest {

    @BeforeEach
    @AfterEach
    void clearCache() throws Exception {
        cache().clear();
    }

    /** PacketEvents's EntityTypes enum needs live mappings, so tests use stand-in types. */
    private static final EntityType PLAYER = mock(EntityType.class);
    private static final EntityType ZOMBIE = mock(EntityType.class);

    @SuppressWarnings("unchecked")
    private static Map<Integer, ?> cache() throws Exception {
        Field field = ReachCheck.class.getDeclaredField("trackedEntities");
        field.setAccessible(true);
        return (Map<Integer, ?>) field.get(null);
    }

    @Test
    void trackSpawn_storesEntityPosition() throws Exception {
        ReachCheck.trackSpawn(7, PLAYER, 10.5, 64.0, -3.25);

        double[] pos = ReachCheck.getTrackedPosition(7);
        assertNotNull(pos, "spawned entity should be tracked");
        assertEquals(10.5, pos[0], 1e-9);
        assertEquals(64.0, pos[1], 1e-9);
        assertEquals(-3.25, pos[2], 1e-9);
    }

    @Test
    void trackMove_updatesExistingEntity() throws Exception {
        ReachCheck.trackSpawn(7, PLAYER, 10.0, 64.0, 0.0);
        ReachCheck.trackMove(7, 12.0, 65.0, 1.0);

        double[] pos = ReachCheck.getTrackedPosition(7);
        assertEquals(12.0, pos[0], 1e-9);
        assertEquals(65.0, pos[1], 1e-9);
        assertEquals(1.0, pos[2], 1e-9);
    }

    @Test
    void trackMove_preservesTypeLearnedAtSpawn() throws Exception {
        ReachCheck.trackSpawn(9, ZOMBIE, 0.0, 64.0, 0.0);
        ReachCheck.trackMove(9, 1.0, 64.0, 0.0);

        // ReachCheck sizes the bounding box by type, so losing it on a move would change
        // the hitbox used for every later reach measurement.
        assertEquals(ZOMBIE, trackedType(9),
            "entity type must survive a position update");
    }

    // === HELPERS ===

    /**
     * Backdates a tracked entry's timestamp.
     *
     * <p>{@code cleanup} ages entries against {@link System#currentTimeMillis()}, so asserting
     * eviction from freshly spawned entries races the clock: if spawn and cleanup land inside
     * the same millisecond the age is 0 and nothing is evicted. Injecting the age removes that
     * dependency — and lets stale and fresh entries coexist in one assertion.</p>
     */
    private static void ageEntry(int entityId, long ageMs) throws Exception {
        Class<?> nested = Class.forName(
            "io.windfall.anticheat.core.check.impl.combat.ReachCheck$TrackedEntity");
        Field timestamp = nested.getDeclaredField("timestamp");
        timestamp.setAccessible(true);

        Object entry = cache().get(entityId);
        assertNotNull(entry, "entity " + entityId + " should be tracked before ageing");
        timestamp.setLong(entry, System.currentTimeMillis() - ageMs);
    }

    private static Object trackedType(int entityId) throws Exception {
        for (Class<?> nested : ReachCheck.class.getDeclaredClasses()) {
            if (!nested.getSimpleName().equals("TrackedEntity")) continue;
            Field typeField = nested.getDeclaredField("type");
            typeField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<Integer, Object> entities = (Map<Integer, Object>) cache();
            Object te = entities.get(entityId);
            assertNotNull(te, "entity " + entityId + " should be tracked");
            return typeField.get(te);
        }
        fail("TrackedEntity class not found");
        return null;
    }

    @Test
    void trackMove_beforeSpawn_stillTracks() throws Exception {
        ReachCheck.trackMove(11, 5.0, 70.0, 6.0);

        double[] pos = ReachCheck.getTrackedPosition(11);
        assertNotNull(pos, "a move without a preceding spawn should still be tracked");
        assertEquals(5.0, pos[0], 1e-9);
    }

    @Test
    void trackRemove_dropsEntity() throws Exception {
        ReachCheck.trackSpawn(7, PLAYER, 1.0, 2.0, 3.0);
        ReachCheck.trackRemove(7);

        assertNull(ReachCheck.getTrackedPosition(7), "destroyed entity should be untracked");
        assertTrue(cache().isEmpty());
    }

    @Test
    void getTrackedPosition_returnsNullForUnknownEntity() {
        assertNull(ReachCheck.getTrackedPosition(4242));
    }

    @Test
    void cleanup_evictsOnlyStaleEntries() throws Exception {
        ReachCheck.trackSpawn(1, PLAYER, 0.0, 64.0, 0.0);
        ReachCheck.trackSpawn(2, PLAYER, 0.0, 64.0, 0.0);

        // Large maxAge keeps both.
        ReachCheck.cleanup(Long.MAX_VALUE);
        assertEquals(2, cache().size());

        // Age only entity 1 past the 5s threshold; entity 2 stays fresh.
        ageEntry(1, 6_000L);

        ReachCheck.cleanup(5_000L);

        assertEquals(1, cache().size(), "stale entry should be evicted");
        assertNull(ReachCheck.getTrackedPosition(1), "stale entity 1 should be gone");
        assertNotNull(ReachCheck.getTrackedPosition(2), "fresh entity 2 must be retained");
    }

    @Test
    void cleanup_zeroMaxAgeEvictsEverything() throws Exception {
        ReachCheck.trackSpawn(1, PLAYER, 0.0, 64.0, 0.0);
        ReachCheck.trackSpawn(2, PLAYER, 0.0, 64.0, 0.0);

        ageEntry(1, 1L);
        ageEntry(2, 1L);

        ReachCheck.cleanup(0L);
        assertTrue(cache().isEmpty(), "cleanup should evict entries older than maxAgeMs");
    }
}