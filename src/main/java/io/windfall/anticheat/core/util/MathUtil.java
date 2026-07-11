package io.windfall.anticheat.core.util;

// Pure math helpers — no Bukkit dependencies so they're safe to use in any context
public final class MathUtil {

    private MathUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static double clamp(double val, double min, double max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    // Linear interpolation — t=0 returns a, t=1 returns b
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double square(double val) {
        return val * val;
    }

    // XZ-only distance, used by reach and speed checks
    public static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double verticalDistance(double y1, double y2) {
        return Math.abs(y1 - y2);
    }

    public static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Converts Minecraft yaw (degrees, clockwise from south) to an XZ direction vector
    public static double[] getDirection(float yaw) {
        double rad = Math.toRadians(yaw);
        double x = -Math.sin(rad);
        double z = Math.cos(rad);
        return new double[]{x, z};
    }

    // Normalizes angle to [-180, 180] — used by aim/snap angle comparisons
    public static double wrapDegrees(double deg) {
        deg %= 360.0;
        if (deg > 180.0) {
            deg -= 360.0;
        } else if (deg < -180.0) {
            deg += 360.0;
        }
        return deg;
    }

    // Recursive GCD — used by timer check to detect repeating packet patterns
    public static int gcd(int a, int b) {
        if (b == 0) return Math.abs(a);
        return gcd(b, a % b);
    }
}
