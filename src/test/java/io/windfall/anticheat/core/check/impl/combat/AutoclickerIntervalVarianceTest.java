package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.impl.combat.AutoclickerCheck.PlayerState;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for issue #122: variance must be measured across adjacent
 * inter-click intervals, not across offsets from the first timestamp.
 */
class AutoclickerIntervalVarianceTest {

    private static final double DELTA = 1e-9;

    private PlayerState state(long... timestamps) {
        PlayerState state = new PlayerState();
        for (long ts : timestamps) {
            state.clickTimestamps.addLast(ts);
        }
        return state;
    }

    /** 20 clicks, every 125 ms apart, starting at t=0. */
    private PlayerState regular8Cps() {
        long[] ts = new long[20];
        for (int i = 0; i < 20; i++) {
            ts[i] = i * 125L;
        }
        return state(ts);
    }

    @Test
    void regularClicking_hasZeroIntervalDeviation() {
        assertEquals(0.0, AutoclickerCheck.calculateStdDev(regular8Cps()), DELTA);
    }

    @Test
    void regularClicking_isBelowAutoclickerThreshold() {
        double stdDev = AutoclickerCheck.calculateStdDev(regular8Cps());
        assertTrue(stdDev < 3.0,
            "perfectly regular 8 CPS clicking must be detectable, got " + stdDev);
    }

    /**
     * The bug report computed ~684.65 ms for this sample under the old offset-based
     * formula. Guard the magnitude so a future refactor cannot silently reintroduce it.
     */
    @Test
    void regularClicking_doesNotInflateDeviationWithWindowLength() {
        PlayerState state = regular8Cps();
        double stdDev = AutoclickerCheck.calculateStdDev(state);
        assertTrue(stdDev < 1.0,
            "deviation must not scale with sample index, got " + stdDev);
    }

    @Test
    void jitteredIntervals_showProportionalDeviation() {
        Random random = new Random(20261003L);
        long[] ts = new long[20];
        ts[0] = 0;
        for (int i = 1; i < 20; i++) {
            ts[i] = ts[i - 1] + 125L + random.nextInt(21) - 10;
        }
        double stdDev = AutoclickerCheck.calculateStdDev(state(ts));
        assertTrue(stdDev > 3.0 && stdDev < 15.0,
            "10 ms of jitter on a 125 ms base should land in the moderate band, got " + stdDev);
    }

    @Test
    void humanJitter_exceedsModerateThreshold() {
        Random random = new Random(4242L);
        long[] ts = new long[20];
        ts[0] = 0;
        for (int i = 1; i < 20; i++) {
            ts[i] = ts[i - 1] + 100L + random.nextInt(101) - 50;
        }
        double stdDev = AutoclickerCheck.calculateStdDev(state(ts));
        assertTrue(stdDev >= 15.0,
            "wide jitter should be treated as human, got " + stdDev);
    }

    @Test
    void meanInterval_reflectsSustainedRate() {
        assertEquals(125.0, AutoclickerCheck.meanInterval(regular8Cps()), DELTA);
    }

    @Test
    void meanInterval_supportsHighRateSignal() {
        long[] ts = new long[100];
        for (int i = 0; i < 100; i++) {
            ts[i] = i * 10L;
        }
        assertEquals(10.0, AutoclickerCheck.meanInterval(state(ts)), DELTA);
    }

    @Test
    void singleSample_returnsMaxValue() {
        assertEquals(Double.MAX_VALUE, AutoclickerCheck.calculateStdDev(state(1000L)), DELTA);
        assertEquals(Double.MAX_VALUE, AutoclickerCheck.meanInterval(state(1000L)), DELTA);
    }

    /**
     * Network batching collapses several clicks into one millisecond. Those pairs produce
     * zero-length intervals that must not drag the mean below the impossible-rate floor,
     * otherwise legitimate packet bursts would escalate the high-rate branch.
     */
    @Test
    void batchedClicks_doNotTripImpossibleMean() {
        long[] ts = new long[30];
        for (int i = 0; i < 30; i++) {
            ts[i] = (i / 3) * 200L;
        }
        double meanMs = AutoclickerCheck.meanInterval(state(ts));
        assertTrue(meanMs >= 25.0,
            "batched clicks should stay above the impossible-rate floor, got " + meanMs);
    }
}