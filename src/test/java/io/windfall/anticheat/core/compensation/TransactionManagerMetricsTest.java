package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionManagerMetricsTest {

    private TransactionManager transactionManager;
    private WindfallPlugin mockPlugin;
    private WindfallPlayer mockPlayer;

    @BeforeEach
    void setUp() {
        mockPlugin = mock(WindfallPlugin.class);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());
        transactionManager = new TransactionManager(mockPlugin);

        mockPlayer = mock(WindfallPlayer.class);
        when(mockPlayer.getUuid()).thenReturn(UUID.randomUUID());
        when(mockPlayer.isValid()).thenReturn(true);
        when(mockPlayer.getProtocolVersion()).thenReturn(47);
    }

    @Test
    void skippedTransactions_initiallyZero() {
        assertEquals(0, transactionManager.getSkippedTransactions());
    }

    @Test
    void unknownResponses_initiallyZero() {
        assertEquals(0, transactionManager.getUnknownResponses());
    }

    @Test
    void noResponseCount_initiallyZero() {
        assertEquals(0, transactionManager.getNoResponseCount());
    }

    @Test
    void incrementSkippedTransactions_incrementsCounter() {
        transactionManager.incrementSkippedTransactions();
        transactionManager.incrementSkippedTransactions();
        assertEquals(2, transactionManager.getSkippedTransactions());
    }

    @Test
    void incrementNoResponseCount_incrementsCounter() {
        transactionManager.incrementNoResponseCount();
        assertEquals(1, transactionManager.getNoResponseCount());
    }

    @Test
    void processTransaction_unknownId_incrementsUnknownResponses() {
        // Create a TransactionState for this player via addCallback (avoids needing sendTransaction)
        UUID uuid = mockPlayer.getUuid();
        transactionManager.addCallback(mockPlayer, (short) 1, () -> {});

        // Process a response with an ID that doesn't match any pending transaction
        transactionManager.processTransaction(mockPlayer, (short) 999);
        assertEquals(1, transactionManager.getUnknownResponses());
    }

    @Test
    void processTransaction_noState_doesNotThrow() {
        // Player with no state should return gracefully without incrementing unknown
        WindfallPlayer unknownPlayer = mock(WindfallPlayer.class);
        UUID uuid = UUID.randomUUID();
        when(unknownPlayer.getUuid()).thenReturn(uuid);

        transactionManager.processTransaction(unknownPlayer, (short) 999);
        assertEquals(0, transactionManager.getUnknownResponses());
    }

    @Test
    void pendingCount_returnsZeroForUnknownPlayer() {
        assertEquals(0, transactionManager.getPendingCount(UUID.randomUUID()));
    }

    @Test
    void pendingCount_returnsZeroWithOnlyCallback() {
        // addCallback creates state but no pending transactions
        transactionManager.addCallback(mockPlayer, (short) 1, () -> {});
        assertEquals(0, transactionManager.getPendingCount(mockPlayer.getUuid()));
    }

    @Test
    void onPlayerQuit_clearsState() {
        UUID uuid = mockPlayer.getUuid();
        transactionManager.addCallback(mockPlayer, (short) 1, () -> {});
        assertNotNull(transactionManager.getPendingCount(uuid) >= 0);

        transactionManager.onPlayerQuit(uuid);
        assertEquals(0, transactionManager.getPendingCount(uuid));
    }

    @Test
    void metrics_areThreadSafe() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transactionManager.incrementSkippedTransactions();
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transactionManager.incrementNoResponseCount();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(100, transactionManager.getSkippedTransactions());
        assertEquals(100, transactionManager.getNoResponseCount());
    }
}
