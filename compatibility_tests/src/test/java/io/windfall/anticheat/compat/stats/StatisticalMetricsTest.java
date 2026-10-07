package io.windfall.anticheat.compat.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticalMetricsTest {

    private static final double[] LINEAR = {1.0D, 2.0D, 3.0D, 4.0D, 5.0D};
    private static final double[] UNIFORM3 = {1.0D, 2.0D, 3.0D};
    private static final double[] WITH_MODE = {1.0D, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D};

    @Test
    void basicDescriptiveStats() {
        assertEquals(15.0D, StatisticalMetrics.sum(LINEAR), 1.0E-9D, "sum");
        assertEquals(3.0D, StatisticalMetrics.mean(LINEAR), 1.0E-9D, "mean");
        assertEquals(1.0D, StatisticalMetrics.min(LINEAR), 0.0D, "min");
        assertEquals(5.0D, StatisticalMetrics.max(LINEAR), 0.0D, "max");
        assertEquals(4.0D, StatisticalMetrics.range(LINEAR), 0.0D, "range");
        assertEquals(3.0D, StatisticalMetrics.midpoint(LINEAR), 0.0D, "midpoint");
    }

    @Test
    void varianceIsPopulationByDefault() {
        assertEquals(2.0D, StatisticalMetrics.variance(LINEAR), 1.0E-9D);
        assertEquals(2.0D, StatisticalMetrics.variance(LINEAR, false), 1.0E-9D);
        assertEquals(2.5D, StatisticalMetrics.variance(LINEAR, true), 1.0E-9D, "sample variance");
        assertEquals(Math.sqrt(2.0D), StatisticalMetrics.stdDev(LINEAR), 1.0E-9D);
    }

    @Test
    void varianceAcceptsPrecomputedMean() {
        assertEquals(2.0D, StatisticalMetrics.variance(LINEAR, 3.0D, false), 1.0E-9D);
    }

    @Test
    void skewnessOfSymmetricDataIsZero() {
        assertEquals(0.0D, StatisticalMetrics.skewness(LINEAR), 1.0E-9D);
        assertEquals(0.0D, StatisticalMetrics.skewness(new double[]{1, 2, 3}), 1.0E-9D);
    }

    @Test
    void skewnessRequiresThreeSamples() {
        assertTrue(Double.isNaN(StatisticalMetrics.skewness(new double[]{1, 2})));
    }

    @Test
    void kurtosisOfLinearSet() {
        assertEquals(2.625D, StatisticalMetrics.kurtosis(LINEAR), 1.0E-9D);
    }

    @Test
    void kurtosisRequiresFourSamples() {
        assertTrue(Double.isNaN(StatisticalMetrics.kurtosis(new double[]{1, 2, 3})));
    }

    @Test
    void entropyOfUniformDistribution() {
        double expected = Math.log(3.0D) / Math.log(2.0D);
        assertEquals(expected, StatisticalMetrics.entropy(UNIFORM3), 1.0E-9D);
    }

    @Test
    void entropyRequiresThreeSamples() {
        assertTrue(Double.isNaN(StatisticalMetrics.entropy(new double[]{1.0D, 2.0D})));
    }

    @Test
    void modeFrequencyReturnsMaxFrequency() {
        assertEquals(2.0D, StatisticalMetrics.modeFrequency(WITH_MODE), 0.0D);
        assertEquals(1.0D, StatisticalMetrics.modeFrequency(UNIFORM3), 0.0D);
        assertEquals(3.0D, StatisticalMetrics.modeFrequency(new double[]{0.1D, 0.1D, 0.1D}), 0.0D);
    }

    @Test
    void emptyInputYieldsNaN() {
        double[] empty = new double[0];
        assertTrue(Double.isNaN(StatisticalMetrics.mean(empty)));
        assertTrue(Double.isNaN(StatisticalMetrics.variance(empty)));
        assertTrue(Double.isNaN(StatisticalMetrics.entropy(empty)));
        assertTrue(Double.isNaN(StatisticalMetrics.modeFrequency(empty)));
    }
}