package io.windfall.anticheat.compat.trust;

public final class TrustFactorModel {

    public enum TrustRank {
        SUPER_UNTRUSTWORTHY,
        UNTRUSTWORTHY,
        SUSPICIOUS,
        NORMAL,
        TRUSTED,
        LEGIT
    }

    private static final double MIN_TRUST = -100.0D;
    private static final double MAX_TRUST = 100.0D;

    private double trust;

    public void increaseTrust() {
        increaseTrustBy(1.0D);
    }

    public void increaseTrustBy(double increase) {
        if (trust < MAX_TRUST) {
            trust = Math.min(MAX_TRUST, trust + increase);
        }
    }

    public void decreaseTrust() {
        decreaseTrustBy(1.0D);
    }

    public void decreaseTrustBy(double decrease) {
        if (trust > MIN_TRUST) {
            trust = Math.max(MIN_TRUST, trust - decrease);
        }
    }

    public void setTrust(double trust) {
        this.trust = Math.max(MIN_TRUST, Math.min(MAX_TRUST, trust));
    }

    public double getTrust() {
        return trust;
    }

    public TrustRank getRank() {
        double current = getTrust();

        if (current <= -50.0D) {
            return TrustRank.SUPER_UNTRUSTWORTHY;
        } else if (current <= -20.0D) {
            return TrustRank.UNTRUSTWORTHY;
        } else if (current < 20.0D) {
            return TrustRank.SUSPICIOUS;
        } else if (current < 60.0D) {
            return TrustRank.NORMAL;
        } else if (current < 85.0D) {
            return TrustRank.TRUSTED;
        } else {
            return TrustRank.LEGIT;
        }
    }

    public int getRequiredBuffer() {
        double current = getTrust();

        if (current < -20.0D) {
            return 0;
        } else if (current < 20.0D) {
            return 5;
        } else if (current < 80.0D) {
            return 15;
        } else {
            return 20;
        }
    }
}