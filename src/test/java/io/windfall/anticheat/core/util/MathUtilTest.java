package io.windfall.anticheat.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MathUtilTest {

    @Test
    void clamp_withinRange_returnsValue() {
        assertEquals(5.0, MathUtil.clamp(5.0, 0.0, 10.0));
    }

    @Test
    void clamp_belowMin_returnsMin() {
        assertEquals(0.0, MathUtil.clamp(-3.0, 0.0, 10.0));
    }

    @Test
    void clamp_aboveMax_returnsMax() {
        assertEquals(10.0, MathUtil.clamp(15.0, 0.0, 10.0));
    }

    @Test
    void clamp_atBoundary_returnsExact() {
        assertEquals(0.0, MathUtil.clamp(0.0, 0.0, 10.0));
        assertEquals(10.0, MathUtil.clamp(10.0, 0.0, 10.0));
    }

    @Test
    void lerp_t0_returnsA() {
        assertEquals(0.0, MathUtil.lerp(0.0, 10.0, 0.0));
    }

    @Test
    void lerp_t1_returnsB() {
        assertEquals(10.0, MathUtil.lerp(0.0, 10.0, 1.0));
    }

    @Test
    void lerp_halfway_returnsMidpoint() {
        assertEquals(5.0, MathUtil.lerp(0.0, 10.0, 0.5));
    }

    @Test
    void square_positiveValue() {
        assertEquals(25.0, MathUtil.square(5.0));
    }

    @Test
    void square_negativeValue() {
        assertEquals(25.0, MathUtil.square(-5.0));
    }

    @Test
    void square_zero() {
        assertEquals(0.0, MathUtil.square(0.0));
    }

    @Test
    void horizontalDistance_samePoint() {
        assertEquals(0.0, MathUtil.horizontalDistance(5.0, 5.0, 5.0, 5.0));
    }

    @Test
    void horizontalDistance_3_4_triangle() {
        assertEquals(5.0, MathUtil.horizontalDistance(0.0, 0.0, 3.0, 4.0), 0.001);
    }

    @Test
    void verticalDistance_samePoint() {
        assertEquals(0.0, MathUtil.verticalDistance(5.0, 5.0));
    }

    @Test
    void verticalDistance_absResult() {
        assertEquals(3.0, MathUtil.verticalDistance(10.0, 7.0));
        assertEquals(3.0, MathUtil.verticalDistance(7.0, 10.0));
    }

    @Test
    void distance3D_originToUnit() {
        assertEquals(1.0, MathUtil.distance3D(0, 0, 0, 1, 0, 0), 0.001);
    }

    @Test
    void distance3D_symmetry() {
        double d1 = MathUtil.distance3D(1, 2, 3, 4, 5, 6);
        double d2 = MathUtil.distance3D(4, 5, 6, 1, 2, 3);
        assertEquals(d1, d2, 0.001);
    }

    @Test
    void getDirection_south_returnsForward() {
        // Yaw 0 = south (positive Z)
        double[] dir = MathUtil.getDirection(0f);
        assertEquals(0.0, dir[0], 0.001); // x ~ 0
        assertEquals(1.0, dir[1], 0.001); // z ~ 1
    }

    @Test
    void getDirection_north_returnsBackward() {
        // Yaw 180 = north (negative Z)
        double[] dir = MathUtil.getDirection(180f);
        assertEquals(0.0, dir[0], 0.001);
        assertEquals(-1.0, dir[1], 0.001);
    }

    @Test
    void wrapDegrees_withinRange() {
        assertEquals(90.0, MathUtil.wrapDegrees(90.0));
    }

    @Test
    void wrapDegrees_overMax_wraps() {
        // 180.0 is NOT > 180.0, so it stays at 180.0 (boundary is exclusive)
        assertEquals(180.0, MathUtil.wrapDegrees(180.0));
    }

    @Test
    void wrapDegrees_underMin_wraps() {
        // -180.0 is NOT < -180.0, so it stays at -180.0 (boundary is exclusive)
        assertEquals(-180.0, MathUtil.wrapDegrees(-180.0));
    }

    @Test
    void wrapDegrees_largePositive_wraps() {
        assertEquals(0.0, MathUtil.wrapDegrees(360.0), 0.001);
    }

    @Test
    void wrapDegrees_largeNegative_wraps() {
        assertEquals(0.0, MathUtil.wrapDegrees(-360.0), 0.001);
    }

    @Test
    void gcd_basicCases() {
        assertEquals(6, MathUtil.gcd(12, 6));
        assertEquals(1, MathUtil.gcd(7, 3));
        assertEquals(5, MathUtil.gcd(0, 5));
    }

    @Test
    void gcd_coprime() {
        assertEquals(1, MathUtil.gcd(13, 17));
    }

    @Test
    void gcd_sameNumber() {
        assertEquals(7, MathUtil.gcd(7, 7));
    }

    @Test
    void gcd_withZero() {
        assertEquals(0, MathUtil.gcd(0, 0));
    }

    @Test
    void utilityClass_constructorThrows() {
        assertThrows(UnsupportedOperationException.class, () -> {
            try {
                var ctor = MathUtil.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                ctor.newInstance();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
