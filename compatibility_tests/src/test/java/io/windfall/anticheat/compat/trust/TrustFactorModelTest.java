package io.windfall.anticheat.compat.trust;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustFactorModelTest {

    @Test
    void startsAtTrustZeroSuspicious() {
        TrustFactorModel model = new TrustFactorModel();
        assertEquals(0.0D, model.getTrust());
        assertEquals(TrustFactorModel.TrustRank.SUSPICIOUS, model.getRank());
    }

    @Test
    void rankBoundaries() {
        assertRank(-100.0D, TrustFactorModel.TrustRank.SUPER_UNTRUSTWORTHY);
        assertRank(-50.0D, TrustFactorModel.TrustRank.SUPER_UNTRUSTWORTHY);
        assertRank(-49.99D, TrustFactorModel.TrustRank.UNTRUSTWORTHY);
        assertRank(-20.0D, TrustFactorModel.TrustRank.UNTRUSTWORTHY);
        assertRank(-19.99D, TrustFactorModel.TrustRank.SUSPICIOUS);
        assertRank(19.99D, TrustFactorModel.TrustRank.SUSPICIOUS);
        assertRank(20.0D, TrustFactorModel.TrustRank.NORMAL);
        assertRank(59.99D, TrustFactorModel.TrustRank.NORMAL);
        assertRank(60.0D, TrustFactorModel.TrustRank.TRUSTED);
        assertRank(84.99D, TrustFactorModel.TrustRank.TRUSTED);
        assertRank(85.0D, TrustFactorModel.TrustRank.LEGIT);
        assertRank(100.0D, TrustFactorModel.TrustRank.LEGIT);
    }

    @Test
    void requiredBufferClamps() {
        assertBuffer(-100.0D, 0);
        assertBuffer(-21.0D, 0);
        assertBuffer(-20.0D, 5);
        assertBuffer(19.99D, 5);
        assertBuffer(20.0D, 15);
        assertBuffer(79.99D, 15);
        assertBuffer(80.0D, 20);
        assertBuffer(100.0D, 20);
    }

    @Test
    void setTrustClampsToBounds() {
        TrustFactorModel model = new TrustFactorModel();
        model.setTrust(500.0D);
        assertEquals(100.0D, model.getTrust());
        model.setTrust(-500.0D);
        assertEquals(-100.0D, model.getTrust());
    }

    @Test
    void trustChangesAreClampedAndIncremental() {
        TrustFactorModel model = new TrustFactorModel();
        for (int i = 0; i < 500; i++) {
            model.increaseTrust();
        }
        assertEquals(100.0D, model.getTrust());
        assertTrue(model.getRank() == TrustFactorModel.TrustRank.LEGIT);

        TrustFactorModel down = new TrustFactorModel();
        down.setTrust(100.0D);
        for (int i = 0; i < 500; i++) {
            down.decreaseTrust();
        }
        assertEquals(-100.0D, down.getTrust());
        assertEquals(TrustFactorModel.TrustRank.SUPER_UNTRUSTWORTHY, down.getRank());
    }

    @Test
    void increaseByNeverExceedsMax() {
        TrustFactorModel model = new TrustFactorModel();
        model.increaseTrustBy(95.0D);
        model.increaseTrustBy(10.0D);
        assertEquals(100.0D, model.getTrust());
    }

    private void assertRank(double trust, TrustFactorModel.TrustRank expected) {
        TrustFactorModel model = new TrustFactorModel();
        model.setTrust(trust);
        assertEquals(expected, model.getRank(), "rank at trust " + trust);
    }

    private void assertBuffer(double trust, int expected) {
        TrustFactorModel model = new TrustFactorModel();
        model.setTrust(trust);
        assertEquals(expected, model.getRequiredBuffer(), "buffer at trust " + trust);
    }
}