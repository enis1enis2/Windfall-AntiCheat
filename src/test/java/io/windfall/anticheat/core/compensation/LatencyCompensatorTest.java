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
import java.lang.reflect.Method;
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
}
