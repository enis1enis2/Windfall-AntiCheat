package io.windfall.anticheat.compat.stats;

import java.util.HashMap;
import java.util.Map;

public final class StatisticalMetrics {

    private static final double LN_2 = Math.log(2.0D);

    private StatisticalMetrics() {
    }

    public static double sum(double[] data) {
        double total = 0.0D;
        for (double value : data) {
            total += value;
        }
        return total;
    }

    public static double mean(double[] data) {
        if (data.length == 0) {
            return Double.NaN;
        }
        return sum(data) / data.length;
    }

    public static double min(double[] data) {
        double min = Double.POSITIVE_INFINITY;
        for (double value : data) {
            min = Math.min(min, value);
        }
        return min;
    }

    public static double max(double[] data) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : data) {
            max = Math.max(max, value);
        }
        return max;
    }

    public static double range(double[] data) {
        return max(data) - min(data);
    }

    public static double midpoint(double[] data) {
        return (max(data) + min(data)) / 2.0D;
    }

    public static double variance(double[] data, double mean, boolean biasCorrected) {
        int n = data.length;
        double sumSquares = 0.0D;
        for (double value : data) {
            double delta = value - mean;
            sumSquares += delta * delta;
        }
        return sumSquares / (biasCorrected ? (double) (n - 1) : (double) n);
    }

    public static double variance(double[] data, boolean biasCorrected) {
        return variance(data, mean(data), biasCorrected);
    }

    public static double variance(double[] data) {
        return variance(data, false);
    }

    public static double stdDev(double[] data) {
        return Math.sqrt(variance(data, false));
    }

    public static double skewness(double[] data) {
        double n = data.length;
        if (n < 3.0D) {
            return Double.NaN;
        }

        double mean = mean(data);
        double variance = variance(data, mean, false);
        double stdDev = Math.sqrt(variance);

        double cubicDeltaSum = 0.0D;
        for (double value : data) {
            double delta = value - mean;
            cubicDeltaSum += delta * delta * delta;
        }

        double skewness = n / (n - 1.0D) / (n - 2.0D);
        skewness *= cubicDeltaSum / (variance * stdDev);
        return skewness;
    }

    public static double kurtosis(double[] data) {
        double n = data.length;
        if (n < 4.0D) {
            return Double.NaN;
        }

        double mean = mean(data);
        double variance = variance(data, mean, false);
        double stdDev = Math.sqrt(variance);

        double quarticDeltaSum = 0.0D;
        for (double value : data) {
            double delta = value - mean;
            quarticDeltaSum += delta * delta * delta * delta;
        }

        double kurtosis = n * (n + 1.0D) / (n - 1.0D) / (n - 2.0D) / (n - 3.0D);
        kurtosis *= quarticDeltaSum / (stdDev * stdDev * stdDev * stdDev);
        kurtosis -= 3.0D * ((n - 1.0D) * (n - 1.0D)) / (n * (n - 3.0D) - 2.0D * (n - 3.0D));
        return kurtosis;
    }

    public static double entropy(double[] data) {
        double n = data.length;
        if (n < 3.0D) {
            return Double.NaN;
        }

        Map<Double, Integer> valueCounts = new HashMap<>();
        for (double value : data) {
            valueCounts.merge(value, 1, Integer::sum);
        }

        double entropy = 0.0D;
        for (int frequency : valueCounts.values()) {
            double probability = frequency / n;
            entropy += probability * (Math.log(probability) / LN_2);
        }
        return -entropy;
    }

    public static double modeFrequency(double[] data) {
        if (data.length == 0) {
            return Double.NaN;
        }

        int best = 0;
        for (int i = 0; i < data.length; i++) {
            int count = 0;
            for (int j = 0; j < data.length; j++) {
                if (data[j] == data[i]) {
                    count++;
                }
            }
            best = Math.max(best, count);
        }
        return best;
    }
}