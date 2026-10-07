package io.windfall.anticheat.compat.sprint;

public final class OmniSprintModel {

    public static final double GROUND_INVALID_ANGLE = 78.0D;
    public static final double GROUND_HARD_INVALID_ANGLE = 112.5D;
    public static final double GROUND_MIN_FORWARD_DOT = 0.20D;
    public static final double GROUND_HARD_BACKWARD_DOT = -0.15D;

    public static final double AIR_HORIZONTAL_FRICTION = 0.91D;
    public static final double AIR_INVALID_INPUT_ANGLE = 90.0D;
    public static final double AIR_HARD_INVALID_INPUT_ANGLE = 105.0D;
    public static final double AIR_MIN_INPUT_FORWARD_DOT = 0.30D;
    public static final double AIR_HARD_BACKWARD_INPUT_DOT = -0.10D;
    public static final double MIN_AIR_INPUT_XZ = 0.234D;

    public static final double MIN_GROUND_DELTA_XZ = 0.075D;
    public static final double MIN_AIR_DELTA_XZ = 0.055D;

    public static final double GROUND_FAIL_THRESHOLD = 8.0D;
    public static final double AIR_FAIL_THRESHOLD = 6.5D;

    private double groundBuffer;
    private double airBuffer;
    private int groundInvalidTicks;
    private int airInvalidTicks;
    private int sprintTicks;
    private int turnGraceTicks = 20;

    public static final class Context {
        public double deltaX;
        public double deltaZ;
        public double yaw;
        public double lastYaw;
        public double deltaYaw;
        public double yawAccel;
        public double baseSpeed;

        public boolean sprinting;
        public boolean lastSprinting;

        public boolean onGround;
        public boolean serverGround;
        public int clientGroundTicks;
        public int serverGroundTicksPlus;
        public int clientAirTicks;
        public int serverAirTicks;

        public boolean packetMoving;
        public boolean insideWater;
        public boolean nearWater;
        public boolean nearWebs;
        public boolean nearClimbable;
        public boolean onIce;
        public boolean onSlime;
        public boolean onSoulSand;
        public boolean onHoney;
        public boolean nearBoat;
        public boolean onBoat;
        public boolean nearWall;
        public boolean colliding;
        public boolean underBlock;
        public boolean predictDownwards;
        public boolean predictUpwards;
        public boolean recentCollision;
        public boolean recentGhostBlock;
        public boolean teleports;
        public boolean riptiding;
        public boolean recentRiptiding;
        public boolean gliding;
        public boolean takingVelocity;
        public boolean inVehicle;
        public boolean recentVehicle;
        public boolean slimeBounce;
        public boolean cancelPending;
    }

    public static final class Result {
        public final boolean groundFlag;
        public final boolean airFlag;
        public final double groundBuffer;
        public final double airBuffer;

        private Result(boolean groundFlag, boolean airFlag, double groundBuffer, double airBuffer) {
            this.groundFlag = groundFlag;
            this.airFlag = airFlag;
            this.groundBuffer = groundBuffer;
            this.airBuffer = airBuffer;
        }

        public static Result rewarded(double groundBuffer, double airBuffer) {
            return new Result(false, false, groundBuffer, airBuffer);
        }

        public static Result ground(double groundBuffer, double airBuffer) {
            return new Result(true, false, groundBuffer, airBuffer);
        }

        public static Result air(double groundBuffer, double airBuffer) {
            return new Result(false, true, groundBuffer, airBuffer);
        }
    }

    public Result handle(Context ctx) {
        DirectionalMovement direction = DirectionalMovement.predict(ctx.deltaX, ctx.deltaZ, ctx.yaw);

        boolean sprinting = ctx.sprinting;
        boolean lastSprinting = ctx.lastSprinting;

        if (sprinting) {
            sprintTicks = Math.min(20, sprintTicks + 1);
        } else {
            sprintTicks = 0;
        }

        double minimumGroundMovement = Math.max(MIN_GROUND_DELTA_XZ, Math.min(0.18D, ctx.baseSpeed * 0.45D));
        double minimumAirMovement = Math.max(MIN_AIR_DELTA_XZ, Math.min(0.16D, ctx.baseSpeed * 0.35D));

        boolean largeTurn = ctx.deltaYaw > 45.0D
                || ctx.yawAccel > 60.0D
                || DirectionalMovement.yawDifference((float) ctx.yaw, (float) ctx.lastYaw) > 45.0D;

        if (largeTurn) {
            turnGraceTicks = 0;
        } else {
            turnGraceTicks = Math.min(20, turnGraceTicks + 1);
        }

        boolean ground =
                ctx.onGround
                        || ctx.serverGround
                        || ctx.clientGroundTicks > 0
                        || ctx.serverGroundTicksPlus > 0;

        boolean air =
                !ground
                        && ctx.clientAirTicks > 1
                        && ctx.serverAirTicks > 1;

        if (exemptReward(ctx, direction)) {
            return Result.rewarded(groundBuffer, airBuffer);
        }

        if (!sprinting && !lastSprinting) {
            groundInvalidTicks = 0;
            airInvalidTicks = 0;
            rewardAll(0.75D);
            return Result.rewarded(groundBuffer, airBuffer);
        }

        boolean groundImpossible =
                direction.absoluteAngle >= GROUND_INVALID_ANGLE
                        || direction.dot < GROUND_MIN_FORWARD_DOT;

        boolean groundHardImpossible =
                direction.absoluteAngle >= GROUND_HARD_INVALID_ANGLE
                        || direction.dot < GROUND_HARD_BACKWARD_DOT;

        boolean groundInvalid =
                ground
                        && direction.deltaXZ > minimumGroundMovement
                        && sprinting
                        && lastSprinting
                        && sprintTicks > 1
                        && turnGraceTicks > 1
                        && groundImpossible;

        boolean usefulAirInput =
                direction.deltaXZ > MIN_AIR_INPUT_XZ
                        && direction.isMoving();

        boolean airInputImpossible =
                direction.absoluteAngle >= AIR_INVALID_INPUT_ANGLE
                        || direction.dot < AIR_MIN_INPUT_FORWARD_DOT;

        boolean airInputHardImpossible =
                direction.absoluteAngle >= AIR_HARD_INVALID_INPUT_ANGLE
                        || direction.dot < AIR_HARD_BACKWARD_INPUT_DOT;

        boolean airInvalid =
                air
                        && direction.deltaXZ > minimumAirMovement
                        && usefulAirInput
                        && sprinting
                        && lastSprinting
                        && sprintTicks > 2
                        && turnGraceTicks > 4
                        && airInputImpossible;

        handleGround(groundInvalid, groundHardImpossible);
        handleAir(airInvalid, airInputHardImpossible);

        if (groundBuffer > GROUND_FAIL_THRESHOLD) {
            groundBuffer = 4.0D;
            groundInvalidTicks = 0;
            return Result.ground(groundBuffer, airBuffer);
        }

        if (airBuffer > AIR_FAIL_THRESHOLD) {
            airBuffer = 3.0D;
            airInvalidTicks = 0;
            return Result.air(groundBuffer, airBuffer);
        }

        return Result.rewarded(groundBuffer, airBuffer);
    }

    private boolean exemptReward(Context ctx, DirectionalMovement direction) {
        if (!ctx.packetMoving) {
            rewardAll(0.75D);
            return true;
        }
        if (!direction.isMoving()) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.insideWater) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.nearWater) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.nearWebs) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.nearClimbable) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.onIce) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.onSlime) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.onSoulSand) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.onHoney) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.nearBoat) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.onBoat) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.nearWall) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.colliding) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.underBlock) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.predictDownwards) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.predictUpwards) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.recentCollision) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.recentGhostBlock) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.teleports) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.riptiding) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.recentRiptiding) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.gliding) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.takingVelocity) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.inVehicle) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.cancelPending) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.slimeBounce) {
            rewardAll(0.75D);
            return true;
        }
        if (ctx.recentVehicle) {
            rewardAll(0.75D);
            return true;
        }
        return false;
    }

    private void handleGround(boolean invalid, boolean hard) {
        if (invalid) {
            groundInvalidTicks++;

            if (groundInvalidTicks > 1) {
                groundBuffer += hard ? 1.45D : 1.0D;
            } else {
                groundBuffer += 0.35D;
            }
        } else {
            groundInvalidTicks = Math.max(0, groundInvalidTicks - 1);
            groundBuffer = Math.max(0.0D, groundBuffer - 0.035D);
        }
    }

    private void handleAir(boolean invalid, boolean hard) {
        if (invalid) {
            airInvalidTicks++;

            if (airInvalidTicks > 2) {
                airBuffer += hard ? 1.15D : 0.90D;
            } else {
                airBuffer += 0.25D;
            }
        } else {
            airInvalidTicks = Math.max(0, airInvalidTicks - 1);
            airBuffer = Math.max(0.0D, airBuffer - 0.030D);
        }
    }

    private void rewardAll(double amount) {
        groundBuffer = Math.max(0.0D, groundBuffer - amount);
        airBuffer = Math.max(0.0D, airBuffer - amount);

        groundInvalidTicks = Math.max(0, groundInvalidTicks - 1);
        airInvalidTicks = Math.max(0, airInvalidTicks - 1);
    }

    public double getGroundBuffer() {
        return groundBuffer;
    }

    public double getAirBuffer() {
        return airBuffer;
    }

    public int getSprintTicks() {
        return sprintTicks;
    }

    public int getGroundInvalidTicks() {
        return groundInvalidTicks;
    }

    public int getAirInvalidTicks() {
        return airInvalidTicks;
    }

    public int getTurnGraceTicks() {
        return turnGraceTicks;
    }
}