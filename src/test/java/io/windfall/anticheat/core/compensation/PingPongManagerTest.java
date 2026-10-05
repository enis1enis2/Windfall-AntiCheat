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
        assertFalse(manager.processPingResponse(player, (short) 1));
    }

    @Test
    void processPingResponse_returnsFalseForUntrackedId() {
        WindfallPlayer player = createValidPlayer();
        manager.onTickStart(player);
        assertFalse(manager.processPingResponse(player, (short) 999));
    }

    @Test
    void processPingResponse_updatesConfirmedTickOnPostChange() {
        WindfallPlayer player = createValidPlayer();

        // onTickEnd increments currentTick to 1, sends post-change ping with id=20
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 20);
        manager.onTickEnd(player);

        // Post-change ping for tick=1 confirms tick 1
        assertFalse(manager.isTickConfirmed(player, 1));
        manager.processPingResponse(player, (short) 20);
        assertTrue(manager.isTickConfirmed(player, 1));
    }

    @Test
    void processPingResponse_returnsTrueForTrackedPing() {
        WindfallPlayer player = createValidPlayer();
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 42);
        manager.onTickStart(player);
        assertTrue(manager.processPingResponse(player, (short) 42));
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
    void getEstimatedLatencyMs_isDerivedFromRecordedSendTimes() throws Exception {
        WindfallPlayer player = createValidPlayer();

        // Send pre-change ping (id=10) and post-change ping (id=20)
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 10);
        manager.onTickStart(player);
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 20);
        manager.onTickEnd(player);

        // Backdate both records so the measured RTT is ~50ms each
        backdatePing(player, (short) 10, 50);
        backdatePing(player, (short) 20, 50);

        manager.processPingResponse(player, (short) 10);
        manager.processPingResponse(player, (short) 20);

        // Estimated one-way latency = (50 + 50) / 4 = 25ms
        int latency = manager.getEstimatedLatencyMs(player);
        assertTrue(latency >= 20 && latency <= 40,
            "Estimated one-way latency should be ~25ms from two ~50ms RTTs, was " + latency);
    }

    @Test
    void processPingResponse_publishesLatencyToCompensator() {
        LatencyCompensator compensator = mock(LatencyCompensator.class);
        when(mockPlugin.getLatencyCompensator()).thenReturn(compensator);

        WindfallPlayer player = createValidPlayer();
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 20);
        manager.onTickEnd(player);

        manager.processPingResponse(player, (short) 20);

        verify(compensator).updateLatency(eq(player.getUuid()), anyInt());
    }

    @Test
    void onTickConfirmed_waitsUntilItsOwnTickIsConfirmed() {
        WindfallPlayer player = createValidPlayer();
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 20);

        boolean[] ran = {false};
        // Tick 1 is not confirmed yet
        manager.onTickConfirmed(player, 1, () -> ran[0] = true);
        assertFalse(ran[0], "Callback must wait for tick 1 to be confirmed");

        manager.onTickEnd(player);
        manager.processPingResponse(player, (short) 20);
        assertTrue(ran[0], "Callback should run once tick 1 is confirmed");
    }

    @Test
    void onTickConfirmed_firesOlderTicksEvenWhenNewerConfirmedFirst() {
        WindfallPlayer player = createValidPlayer();

        boolean[] oldTick = {false};
        manager.onTickConfirmed(player, 1, () -> oldTick[0] = true);

        // Confirm tick 3 directly, skipping tick 2 — tick 1's callback must still run
        when(mockTransactionManager.sendPigPongTransaction(player)).thenReturn((short) 30);
        manager.onTickEnd(player);
        manager.onTickEnd(player);
        manager.onTickEnd(player);
        manager.processPingResponse(player, (short) 30);

        assertTrue(oldTick[0], "Tick 1 callback should run when a later tick is confirmed");
    }

    @Test
    void onTickConfirmed_callbackFailure_doesNotStopOtherCallbacks() {
        WindfallPlayer player = createValidPlayer();
        when(mockPlugin.getLogger()).thenReturn(mock(java.util.logging.Logger.class));

        boolean[] second = {false};
        manager.onTickConfirmed(player, 0, () -> {
            throw new IllegalStateException("boom");
        });
        manager.onTickConfirmed(player, 0, () -> second[0] = true);

        assertTrue(second[0], "A failing callback must not prevent later callbacks from running");
    }

    /** Rewrites a pending ping's send time so the measured RTT is deterministic. */
    private void backdatePing(WindfallPlayer player, short id, long millis) throws Exception {
        Field statesField = PingPongManager.class.getDeclaredField("playerStates");
        statesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) statesField.get(manager);
        Object state = states.get(player.getUuid());

        Field pingsField = state.getClass().getDeclaredField("pendingPings");
        pingsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Short, Object> pings = (Map<Short, Object>) pingsField.get(state);

        Object record = pings.get(id);
        Field tickField = record.getClass().getDeclaredField("tick");
        tickField.setAccessible(true);
        Field startField = record.getClass().getDeclaredField("isStartPing");
        startField.setAccessible(true);

        Object replacement = record.getClass()
            .getDeclaredConstructor(int.class, boolean.class, long.class)
            .newInstance((int) tickField.get(record), (boolean) startField.get(record),
                System.nanoTime() - millis * 1_000_000L);
        pings.put(id, replacement);
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
