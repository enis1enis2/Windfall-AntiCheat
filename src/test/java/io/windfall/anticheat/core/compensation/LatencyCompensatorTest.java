package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LatencyCompensatorTest {

    @Mock private World mockWorld;
    private LatencyCompensator compensator;

    @BeforeEach
    void setUp() {
        compensator = new LatencyCompensator();
    }

    @Test
    void onBlockChange_queuesChange() throws Exception {
        UUID uuid = UUID.randomUUID();
        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);
        assertEquals(1, compensator.getPendingChangeCount(uuid));
    }

    @Test
    void onBlockChange_multipleChanges() throws Exception {
        UUID uuid = UUID.randomUUID();
        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);
        compensator.onBlockChange(uuid, 11, 64, 20, Material.STONE, mockWorld);
        compensator.onBlockChange(uuid, 12, 64, 20, Material.DIRT, mockWorld);
        assertEquals(3, compensator.getPendingChangeCount(uuid));
    }

    @Test
    void onBlockChange_capsAt1000Entries() throws Exception {
        UUID uuid = UUID.randomUUID();
        for (int i = 0; i < 1100; i++) {
            compensator.onBlockChange(uuid, i, 64, 0, Material.AIR, mockWorld);
        }
        assertTrue(compensator.getPendingChangeCount(uuid) <= 1000);
    }

    @Test
    void updateLatency_storesLatency() throws Exception {
        UUID uuid = UUID.randomUUID();
        compensator.updateLatency(uuid, 50);

        Field field = LatencyCompensator.class.getDeclaredField("latencyCache");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> cache = (Map<UUID, Integer>) field.get(compensator);
        assertEquals(50, cache.get(uuid));
    }

    @Test
    void getPendingChangeCount_returnsZeroForUnknownPlayer() {
        assertEquals(0, compensator.getPendingChangeCount(UUID.randomUUID()));
    }

    @Test
    void isBlockBroken_returnsFalseForNoWorld() {
        assertFalse(compensator.isBlockBroken(UUID.randomUUID(), 0, 0, 0));
    }

    @Test
    void getWorld_createsNewCompensatedWorld() {
        UUID uuid = UUID.randomUUID();
        CompensatedWorld world = compensator.getWorld(uuid, mockWorld);
        assertNotNull(world);
    }

    @Test
    void getWorld_returnsSameInstanceForSameUuid() {
        UUID uuid = UUID.randomUUID();
        CompensatedWorld world1 = compensator.getWorld(uuid, mockWorld);
        CompensatedWorld world2 = compensator.getWorld(uuid, mockWorld);
        assertSame(world1, world2);
    }

    @Test
    void processDeferredChanges_appliesOldChanges() throws Exception {
        UUID uuid = UUID.randomUUID();
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getTransactionPing()).thenReturn(0);

        CompensatedWorld world = compensator.getWorld(uuid, mockWorld);
        compensator.updateLatency(uuid, 0);

        // Add a change with timestamp in the past (> latency window)
        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);

        // Artificially backdate the timestamp
        Field deferredField = LatencyCompensator.class.getDeclaredField("deferredChanges");
        deferredField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Queue<?>> deferred = (Map<UUID, Queue<?>>) deferredField.get(compensator);
        Queue<?> queue = deferred.get(uuid);
        Object change = queue.peek();
        Field timestampField = change.getClass().getDeclaredField("timestamp");
        timestampField.setAccessible(true);
        timestampField.setLong(change, System.currentTimeMillis() - 5000);

        compensator.processDeferredChanges(uuid, player);

        assertEquals(0, compensator.getPendingChangeCount(uuid));
        Material mat = world.getBlock(10, 64, 20);
        assertEquals(Material.AIR, mat);
    }

    @Test
    void processDeferredChanges_keepsRecentChanges() throws Exception {
        UUID uuid = UUID.randomUUID();
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getTransactionPing()).thenReturn(10000);

        compensator.getWorld(uuid, mockWorld);
        compensator.updateLatency(uuid, 10000);

        // Recent change — should be deferred
        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);
        compensator.processDeferredChanges(uuid, player);

        assertEquals(1, compensator.getPendingChangeCount(uuid));
    }

    @Test
    void processDeferredChanges_appliesChangesOlderThanMaxDeferMs() throws Exception {
        UUID uuid = UUID.randomUUID();
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getTransactionPing()).thenReturn(0);

        compensator.getWorld(uuid, mockWorld);
        compensator.updateLatency(uuid, 0);

        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);

        // Backdate beyond MAX_DEFER_MS (2000ms)
        Field deferredField = LatencyCompensator.class.getDeclaredField("deferredChanges");
        deferredField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Queue<?>> deferred = (Map<UUID, Queue<?>>) deferredField.get(compensator);
        Queue<?> queue = deferred.get(uuid);
        Object change = queue.peek();
        Field timestampField = change.getClass().getDeclaredField("timestamp");
        timestampField.setAccessible(true);
        timestampField.setLong(change, System.currentTimeMillis() - 3000);

        compensator.processDeferredChanges(uuid, player);

        assertEquals(0, compensator.getPendingChangeCount(uuid));
    }

    @Test
    void onPlayerQuit_cleansUp() throws Exception {
        UUID uuid = UUID.randomUUID();
        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);
        compensator.getWorld(uuid, mockWorld);
        compensator.updateLatency(uuid, 50);

        compensator.onPlayerQuit(uuid);

        assertEquals(0, compensator.getPendingChangeCount(uuid));
        assertFalse(compensator.isBlockBroken(uuid, 10, 64, 20));
    }

    @Test
    void processDeferredChanges_noWorld_doesNotThrow() {
        UUID uuid = UUID.randomUUID();
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getTransactionPing()).thenReturn(0);

        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, mockWorld);

        // No world created — should not throw
        assertDoesNotThrow(() -> compensator.processDeferredChanges(uuid, player));
    }

    // === NEW: Tick-based tracking tests ===

    @Test
    void recordChange_storesTickChange() {
        UUID uuid = UUID.randomUUID();
        WorldChange change = WorldChange.blockBreak(50, 10, 64, 20);
        compensator.recordChange(uuid, 50, change);

        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 49, 51);
        assertEquals(1, result.size());
        assertEquals(WorldChange.Type.BLOCK_BREAK, result.get(0).getType());
    }

    @Test
    void getUnconfirmedChanges_returnsOnlyUnconfirmedRange() {
        UUID uuid = UUID.randomUUID();
        // Changes at ticks 50, 51, 52
        compensator.recordChange(uuid, 50, WorldChange.blockBreak(50, 10, 64, 20));
        compensator.recordChange(uuid, 51, WorldChange.blockBreak(51, 11, 64, 20));
        compensator.recordChange(uuid, 52, WorldChange.blockBreak(52, 12, 64, 20));

        // Confirmed tick 50, current tick 52 → only tick 51 is unconfirmed
        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 50, 52);
        assertEquals(1, result.size());
        assertEquals(51, result.get(0).getTick());
    }

    @Test
    void getUnconfirmedChanges_returnsEmptyForConfirmedRange() {
        UUID uuid = UUID.randomUUID();
        compensator.recordChange(uuid, 50, WorldChange.blockBreak(50, 10, 64, 20));

        // Confirmed tick 50, current tick 50 → nothing unconfirmed
        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 50, 50);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUnconfirmedChanges_returnsEmptyForUnknownPlayer() {
        List<WorldChange> result = compensator.getUnconfirmedChanges(UUID.randomUUID(), 0, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    void onBlockChange_withTick_recordsTickChange() {
        UUID uuid = UUID.randomUUID();
        compensator.onBlockChange(uuid, 10, 64, 20, Material.AIR, 42, mockWorld);

        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 41, 43);
        assertEquals(1, result.size());
        assertEquals(WorldChange.Type.BLOCK_BREAK, result.get(0).getType());
    }

    @Test
    void recordChange_multipleChangesAtSameTick() {
        UUID uuid = UUID.randomUUID();
        compensator.recordChange(uuid, 50, WorldChange.blockBreak(50, 10, 64, 20));
        compensator.recordChange(uuid, 50, WorldChange.velocity(50, 0.5, 0, 0));

        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 49, 51);
        assertEquals(2, result.size());
    }

    @Test
    void pruneTickHistory_removesOldTicks() {
        UUID uuid = UUID.randomUUID();
        compensator.recordChange(uuid, 10, WorldChange.blockBreak(10, 10, 64, 20));
        compensator.recordChange(uuid, 50, WorldChange.blockBreak(50, 11, 64, 20));
        compensator.recordChange(uuid, 100, WorldChange.blockBreak(100, 12, 64, 20));

        // cutoff = 150 - 100 = 50, so ticks < 50 are pruned
        compensator.pruneTickHistory(150);

        List<WorldChange> at10 = compensator.getUnconfirmedChanges(uuid, 9, 11);
        List<WorldChange> at50 = compensator.getUnconfirmedChanges(uuid, 49, 51);
        List<WorldChange> at100 = compensator.getUnconfirmedChanges(uuid, 99, 101);

        assertTrue(at10.isEmpty(), "Tick 10 should be pruned (10 < 50 cutoff)");
        assertFalse(at50.isEmpty(), "Tick 50 should still exist (50 == cutoff, not < cutoff)");
        assertFalse(at100.isEmpty(), "Tick 100 should still exist");
    }

    @Test
    void onPlayerQuit_cleansUpTickChanges() {
        UUID uuid = UUID.randomUUID();
        compensator.recordChange(uuid, 50, WorldChange.blockBreak(50, 10, 64, 20));

        compensator.onPlayerQuit(uuid);

        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 49, 51);
        assertTrue(result.isEmpty(), "Tick changes should be cleaned up on quit");
    }

    @Test
    void recordChange_sentinelTickZero_isIgnored() {
        UUID uuid = UUID.randomUUID();
        // tick=0 is a sentinel from the legacy onBlockChange overload
        compensator.recordChange(uuid, 0, WorldChange.blockBreak(0, 10, 64, 20));

        List<WorldChange> result = compensator.getUnconfirmedChanges(uuid, 0, 1);
        assertTrue(result.isEmpty(), "Tick 0 should be ignored");
    }
}
