package io.windfall.anticheat.core.util;

/**
 * Pure math helper methods with no Bukkit dependencies.
 *
 * <p>Safe to use in any context (checks, physics calculations, compensation, tests).
 * Provides commonly used mathematical operations for anti-cheat detection algorithms.
 *
 * <p>Key methods:
 * <ul>
 *   <li>{@link #horizontalDistance(double, double, double, double)} &mdash; XZ-only distance
 *       for speed/reach checks (ignores Y to prevent vertical false positives)</li>
 *   <li>{@link #wrapDegrees(double)} &mdash; normalizes angles to [-180, 180] for aim/snap comparisons</li>
 *   <li>{@link #getDirection(float)} &mdash; converts Minecraft yaw to XZ direction vector</li>
 *   <li>{@link #gcd(int, int)} &mdash; GCD for timer check pattern detection</li>
 * </ul>
 *
 * @see MaterialUtils for block type classification utilities
 */
// Pure math helpers — no Bukkit dependencies so they're safe to use in any context
public final class MathUtil {

    /** Private constructor — utility class, not instantiable */
    private MathUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Clamps a value to the range [min, max].
     *
     * @param val the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return val if within range, min if val &lt; min, max if val &gt; max
     */
    public static double clamp(double val, double min, double max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    /**
     * Linear interpolation between two values.
     * t=0 returns a, t=1 returns b, values outside [0,1] extrapolate.
     *
     * @param a the start value (returned when t=0)
     * @param b the end value (returned when t=1)
     * @param t interpolation factor
     * @return the interpolated value
     */
    // Linear interpolation — t=0 returns a, t=1 returns b
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Squares a value (val * val).
     * Avoids repeated multiplication in distance calculations.
     *
     * @param val the value to square
     * @return val squared
     */
    public static double square(double val) {
        return val * val;
    }

    /**
     * Computes horizontal (XZ-only) Euclidean distance between two points.
     * Used by reach and speed checks where vertical distance should be ignored.
     *
     * @param x1 first point X
     * @param z1 first point Z
     * @param x2 second point X
     * @param z2 second point Z
     * @return the horizontal distance sqrt(dx² + dz²)
     */
    // XZ-only distance, used by reach and speed checks
    public static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Computes absolute vertical distance between two Y coordinates.
     *
     * @param y1 first Y coordinate
     * @param y2 second Y coordinate
     * @return |y1 - y2|
     */
    public static double verticalDistance(double y1, double y2) {
        return Math.abs(y1 - y2);
    }

    /**
     * Computes 3D Euclidean distance between two points.
     *
     * @param x1 first point X
     * @param y1 first point Y
     * @param z1 first point Z
     * @param x2 second point X
     * @param y2 second point Y
     * @param z2 second point Z
     * @return the 3D distance sqrt(dx² + dy² + dz²)
     */
    public static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Converts Minecraft yaw (degrees, clockwise from south) to an XZ direction vector.
     * The returned array is [x, z] where x = -sin(yaw) and z = cos(yaw).
     *
     * <p>Minecraft yaw convention: 0 = south (+Z), 90 = west (-X), 180/-180 = north (-Z),
     * -90 = east (+X).
     *
     * @param yaw the yaw angle in degrees
     * @return a two-element array [directionX, directionZ]
     */
    // Converts Minecraft yaw (degrees, clockwise from south) to an XZ direction vector
    public static double[] getDirection(float yaw) {
        double rad = Math.toRadians(yaw);
        double x = -Math.sin(rad);
        double z = Math.cos(rad);
        return new double[]{x, z};
    }

    /**
     * Normalizes an angle to the range [-180, 180] degrees.
     * Used by aim/snap angle comparisons to handle wraparound correctly.
     *
     * @param deg the angle in degrees (any value)
     * @return the normalized angle in [-180, 180]
     */
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

    /**
     * Computes the Greatest Common Divisor (GCD) of two integers using the Euclidean algorithm.
     * Used by timer checks to detect repeating packet patterns — if the GCD of packet intervals
     * is large, it suggests an automated periodic sender rather than natural gameplay.
     *
     * @param a the first integer
     * @param b the second integer
     * @return the GCD of |a| and |b|
     */
    // Recursive GCD — used by timer check to detect repeating packet patterns
    public static int gcd(int a, int b) {
        if (b == 0) return Math.abs(a);
        return gcd(b, a % b);
    }
}
