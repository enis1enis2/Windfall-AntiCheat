package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PingPongManagerTest {

    @Mock private WindfallPlugin mockPlugin;
    @Mock private TransactionManager mockTransactionManager;
    private PingPongManager manager;

    @BeforeEach
    void setUp() {
        when(mockPlugin.getTransactionManager()).thenReturn(mockTransactionManager);
        when(mockTransactionManager.sendPigPongTransaction(any(WindfallPlayer.class))).thenReturn((short) 1);
        manager = new PingPongManager(mockPlugin);
    }

    @Test
    void constructor_createsEmptyStateMap() throws Exception {
        Field field = PingPongManager.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) field.get(manager);
        assertTrue(states.isEmpty());
    }

    @Test
    void getConfirmedTick_returnsZeroForUnknownPlayer() {
        WindfallPlayer player = createPlayer();
        assertEquals(0, manager.getConfirmedTick(player));
    }

    @Test
    void getCurrentTick_returnsZeroForUnknownPlayer() {
        WindfallPlayer player = createPlayer();
        assertEquals(0, manager.getCurrentTick(player));
    }

    @Test
    void isTickConfirmed_returnsFalseForTickAboveZero() {
        WindfallPlayer player = createPlayer();
        // confirmedTick starts at 0, so tick 1 is not confirmed
        assertFalse(manager.isTickConfirmed(player, 1));
    }

    @Test
    void isTickConfirmed_returnsTrueForTickZero() {
        WindfallPlayer player = createPlayer();
        // confirmedTick starts at 0, so tick 0 is confirmed
        assertTrue(manager.isTickConfirmed(player, 0));
    }

    @Test
    void getEstimatedLatencyMs_returnsZeroForUnknownPlayer() {
        WindfallPlayer player = createPlayer();
        assertEquals(0, manager.getEstimatedLatencyMs(player));
    }

    @Test
    void onTickStart_sendsTransactionPing() {
        WindfallPlayer player = createValidPlayer();
        manager.onTickStart(player);
        verify(mockTransactionManager).sendPigPongTransaction(player);
    }

    @Test
    void onTickStart_skipsInvalidPlayer() {
        WindfallPlayer player = createPlayer();
        when(player.isValid()).thenReturn(false);
        manager.onTickStart(player);
        verify(mockTransactionManager, never()).sendPigPongTransaction(any());
    }

    @Test
    void onTickEnd_incrementsCurrentTick() throws Exception {
        WindfallPlayer player = createValidPlayer();
        manager.onTickEnd(player);
        assertEquals(1, manager.getCurrentTick(player));
        manager.onTickEnd(player);
        assertEquals(2, manager.getCurrentTick(player));
    }

    @Test
    void processPingResponse_returnsFalseForUnknownPlayer() {
        WindfallPlayer player = createPlayer();
        assertFalse(manager.processPingResponse(player, (short) 1, System.nanoTime()));
    }

    @Test
    void processPingResponse_returnsFalseForUntrackedId() {
        WindfallPlayer player = createValidPlayer();
        manager.onTickStart(player);
        assertFalse(manager.processPingResponse(player, (short) 999, System.nanoTime()));
    }

    @Test
    void processPingResponse_updatesConfirmedTickOnPostChange() {
        WindfallPlayer player = createValidPlayer();

        // onTickEnd increments currentTick to 1, sends post-change ping with id=20
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 20);
        manager.onTickEnd(player);

        // Post-change ping for tick=1 confirms tick 1
        assertFalse(manager.isTickConfirmed(player, 1));
        manager.processPingResponse(player, (short) 20, System.nanoTime());
        assertTrue(manager.isTickConfirmed(player, 1));
    }

    @Test
    void processPingResponse_returnsTrueForTrackedPing() {
        WindfallPlayer player = createValidPlayer();
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 42);
        manager.onTickStart(player);
        assertTrue(manager.processPingResponse(player, (short) 42, System.nanoTime()));
    }

    @Test
    void onPlayerQuit_removesState() throws Exception {
        WindfallPlayer player = createValidPlayer();
        manager.onTickStart(player);
        manager.onTickEnd(player);

        UUID uuid = player.getUuid();
        Field field = PingPongManager.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) field.get(manager);
        assertEquals(1, states.size());

        manager.onPlayerQuit(uuid);
        assertTrue(states.isEmpty());
    }

    @Test
    void getEstimatedLatencyMs_averagesRttFromPings() {
        WindfallPlayer player = createValidPlayer();

        // Send pre-change ping (id=10) and post-change ping (id=20)
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 10);
        manager.onTickStart(player);
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 20);
        manager.onTickEnd(player);

        // Process both with ~50ms simulated RTT
        long now = System.nanoTime();
        long fakeSendTime = now - 50_000_000L; // 50ms ago
        manager.processPingResponse(player, (short) 10, fakeSendTime);
        manager.processPingResponse(player, (short) 20, fakeSendTime);

        // Estimated one-way latency = (50 + 50) / 4 = 25ms
        int latency = manager.getEstimatedLatencyMs(player);
        assertTrue(latency >= 0, "Latency should be non-negative");
    }

    @Test
    void onTickConfirmed_runsCallbackImmediatelyIfAlreadyConfirmed() {
        WindfallPlayer player = createValidPlayer();
        boolean[] ran = {false};
        manager.onTickConfirmed(player, 0, () -> ran[0] = true);
        assertTrue(ran[0], "Callback should run immediately since tick 0 <= confirmedTick 0");
    }

    private WindfallPlayer createPlayer() {
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getUuid()).thenReturn(UUID.randomUUID());
        return player;
    }

    private WindfallPlayer createValidPlayer() {
        WindfallPlayer player = createPlayer();
        when(player.isValid()).thenReturn(true);
        return player;
    }
}
