package io.windfall.anticheat.compat.elytra;

public final class FireworkBoostModel {

    public static final double MIN_BOOST_MULTIPLIER = 1.15D;
    public static final double MAX_BOOST_MULTIPLIER = 1.25D;

    public static final int DURATION_LEVEL_1 = 10;
    public static final int DURATION_LEVEL_2 = 20;
    public static final int DURATION_LEVEL_3 = 30;

    public static final int POST_BOOST_GRACE = 10;
    public static final double MIN_SPEED_FOR_DETECTION = 0.5D;

    private boolean boostActive;
    private int boostStartTick;
    private int boostDuration;
    private int ticksSinceBoostEnd;

    private double lastSpeed;
    private double speedAtBoostStart;
    private int consecutiveAccelerationTicks;
    private double totalAcceleration;

    private boolean lastBoostWasLegitimate;
    private String lastBoostFailReason;

    private int legitimateBoostCount;
    private int suspiciousBoostCount;

    public FireworkBoostModel() {
        reset();
    }

    public void reset() {
        this.boostActive = false;
        this.boostStartTick = 0;
        this.boostDuration = 0;
        this.ticksSinceBoostEnd = 100;
        this.lastSpeed = 0;
        this.speedAtBoostStart = 0;
        this.consecutiveAccelerationTicks = 0;
        this.totalAcceleration = 0;
        this.lastBoostWasLegitimate = false;
        this.lastBoostFailReason = null;
        this.legitimateBoostCount = 0;
        this.suspiciousBoostCount = 0;
    }

    public void onFireworkInteraction(int currentTick, double currentSpeed, boolean isGliding) {
        if (!isGliding) {
            this.lastBoostFailReason = "Not gliding";
            return;
        }

        this.boostActive = true;
        this.boostStartTick = currentTick;
        this.boostDuration = 0;
        this.speedAtBoostStart = currentSpeed;
        this.consecutiveAccelerationTicks = 0;
        this.totalAcceleration = 0;
        this.lastBoostFailReason = null;
    }

    public boolean update(int currentTick, double currentSpeed, double deltaSpeed) {
        if (!this.boostActive && this.ticksSinceBoostEnd < POST_BOOST_GRACE) {
            this.ticksSinceBoostEnd++;
        }

        double prevSpeed = this.lastSpeed;
        this.lastSpeed = currentSpeed;

        if (!this.boostActive) {
            return isInGracePeriod();
        }

        this.boostDuration = currentTick - this.boostStartTick;

        if (deltaSpeed > 0.01D) {
            this.consecutiveAccelerationTicks++;
            this.totalAcceleration += deltaSpeed;

            if (prevSpeed > 0.1D) {
                double multiplier = currentSpeed / prevSpeed;
                if (multiplier > MAX_BOOST_MULTIPLIER) {
                    this.suspiciousBoostCount++;
                }
            }
        } else {
            if (this.boostDuration > 5) {
                endBoost(currentTick, true);
            }
        }

        if (this.boostDuration > DURATION_LEVEL_3 + 10) {
            endBoost(currentTick, false);
            this.lastBoostFailReason = "Duration exceeded maximum";
            return false;
        }

        return this.boostActive || isInGracePeriod();
    }

    private void endBoost(int currentTick, boolean wasLegitimate) {
        if (!this.boostActive) {
            return;
        }

        this.boostActive = false;
        this.ticksSinceBoostEnd = 0;

        boolean legitimate = wasLegitimate;

        if (this.consecutiveAccelerationTicks < 3) {
            legitimate = false;
            this.lastBoostFailReason = "Insufficient acceleration";
        }

        double expectedMinAccel = this.speedAtBoostStart * 0.1D;
        if (this.totalAcceleration < expectedMinAccel) {
            legitimate = false;
            this.lastBoostFailReason = "Acceleration too low";
        }

        if (this.boostDuration < DURATION_LEVEL_1 - 3) {
            legitimate = false;
            this.lastBoostFailReason = "Duration too short";
        }

        this.lastBoostWasLegitimate = legitimate;

        if (legitimate) {
            this.legitimateBoostCount++;
        } else {
            this.suspiciousBoostCount++;
        }
    }

    public boolean isInBoostOrGrace() {
        return this.boostActive || isInGracePeriod();
    }

    public boolean isInGracePeriod() {
        return !this.boostActive
                && this.ticksSinceBoostEnd < POST_BOOST_GRACE
                && this.lastBoostWasLegitimate;
    }

    public double getSuspicionScore() {
        if (this.legitimateBoostCount + this.suspiciousBoostCount == 0) {
            return 0.0D;
        }

        double ratio = (double) this.suspiciousBoostCount
                / (this.legitimateBoostCount + this.suspiciousBoostCount);

        return ratio * 10.0D;
    }

    public boolean isFakeFirework() {
        if (this.boostActive) {
            return false;
        }

        return this.ticksSinceBoostEnd < 5 && !this.lastBoostWasLegitimate;
    }

    public boolean isBoostActive() {
        return boostActive;
    }

    public int getBoostDuration() {
        return boostDuration;
    }

    public int getTicksSinceBoostEnd() {
        return ticksSinceBoostEnd;
    }

    public boolean wasLastBoostLegitimate() {
        return lastBoostWasLegitimate;
    }

    public String getLastBoostFailReason() {
        return lastBoostFailReason;
    }

    public int getLegitimateBoostCount() {
        return legitimateBoostCount;
    }

    public int getSuspiciousBoostCount() {
        return suspiciousBoostCount;
    }

    public double getTotalAcceleration() {
        return totalAcceleration;
    }

    public int getConsecutiveAccelerationTicks() {
        return consecutiveAccelerationTicks;
    }
}