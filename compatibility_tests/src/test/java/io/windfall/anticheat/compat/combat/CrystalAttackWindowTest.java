package io.windfall.anticheat.compat.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalAttackWindowTest {

    private static final long T0 = 1_000_000L;

    @Test
    void fastPlaceBreakAccumulatesAndFlags() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        boolean flagged = false;

        for (int i = 0; i < 10 && !flagged; i++) {
            long place = T0 + (long) i * 1000L;
            window.onCrystalPlace(place);

            CrystalAttackWindow.Flag flag = window.onAttack(
                    place + 25L,
                    true,
                    true,
                    2
            );

            if (flag == CrystalAttackWindow.Flag.FAST_PLACE_BREAK) {
                flagged = true;
                assertEquals(3.0D, window.getBuffer(), 1.0E-9D, "buffer resets to 3 after place->break flag");
            }
        }

        assertTrue(flagged, "fourth sub-40ms place->break cycle must exceed threshold");
    }

    @Test
    void softWindowDecaysBuffer() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        window.onCrystalPlace(T0);
        window.onAttack(T0 + 25L, true, true, 2);
        assertEquals(2.0D, window.getBuffer(), 1.0E-9D);

        window.onCrystalPlace(T0 + 1000L);
        window.onAttack(T0 + 1000L + 60L, true, true, 2);
        assertEquals(1.75D, window.getBuffer(), 1.0E-9D, "40-100ms decays by 0.25");
    }

    @Test
    void slowBreakDecaysBuffer() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        window.onCrystalPlace(T0);
        window.onAttack(T0 + 200L, true, true, 2);
        assertEquals(0.0D, window.getBuffer(), 1.0E-9D, "buffer must never go negative");
    }

    @Test
    void predictAttackFlagsWithoutPlace() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        boolean flagged = false;

        for (int i = 0; i < 6 && !flagged; i++) {
            CrystalAttackWindow.Flag flag = window.onAttack(
                    T0 + (long) i * 1000L,
                    true,
                    true,
                    0
            );

            if (flag == CrystalAttackWindow.Flag.ID_PREDICT) {
                flagged = true;
                assertEquals(5.7D, window.getBuffer(), 1.0E-9D);
            }
        }

        assertTrue(flagged, "fourth 0-tick ID prediction must exceed threshold");
    }

    @Test
    void nonCrystalTargetsNeverPredictFlag() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        for (int i = 0; i < 8; i++) {
            CrystalAttackWindow.Flag flag = window.onAttack(
                    T0 + (long) i * 1000L,
                    true,
                    false,
                    0
            );
            assertEquals(CrystalAttackWindow.Flag.NONE, flag);
        }
    }

    @Test
    void spawnTrackingWindowWorks() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        window.onCrystalSpawn(42, T0);
        assertTrue(window.wasSpawnedWithin(42, T0 + 5000L, 10_000L));
        assertTrue(!window.wasSpawnedWithin(42, T0 + 20_000L, 10_000L));
        assertTrue(!window.wasSpawnedWithin(99, T0, 10_000L));
    }

    @Test
    void resolveOnlyCrystalTargetsRespectTickLife() {
        CrystalAttackWindow window = new CrystalAttackWindow();
        CrystalAttackWindow.Flag flag = window.onAttack(T0, true, true, 1);
        assertEquals(CrystalAttackWindow.Flag.NONE, flag);
    }
}