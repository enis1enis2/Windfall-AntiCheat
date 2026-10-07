package io.windfall.anticheat.compat.sprint;

public final class DirectionalMovement {

    public enum MovementSector {
        FORWARD,
        FORWARD_LEFT,
        FORWARD_RIGHT,
        LEFT,
        RIGHT,
        BACKWARD_LEFT,
        BACKWARD_RIGHT,
        BACKWARD,
        STATIONARY
    }

    public enum RelativeMove {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        STATIONARY
    }

    private static final double MOVE_EPS = 1.0E-4D;

    public final double deltaX;
    public final double deltaZ;
    public final double deltaXZ;
    public final double yaw;
    public final double moveX;
    public final double moveZ;
    public final double forwardX;
    public final double forwardZ;
    public final double dot;
    public final double cross;
    public final double signedAngle;
    public final double absoluteAngle;
    public final MovementSector sector;

    private DirectionalMovement(double deltaX,
                                double deltaZ,
                                double deltaXZ,
                                double yaw,
                                double moveX,
                                double moveZ,
                                double forwardX,
                                double forwardZ,
                                double dot,
                                double cross,
                                double signedAngle,
                                double absoluteAngle,
                                MovementSector sector) {
        this.deltaX = deltaX;
        this.deltaZ = deltaZ;
        this.deltaXZ = deltaXZ;
        this.yaw = yaw;
        this.moveX = moveX;
        this.moveZ = moveZ;
        this.forwardX = forwardX;
        this.forwardZ = forwardZ;
        this.dot = dot;
        this.cross = cross;
        this.signedAngle = signedAngle;
        this.absoluteAngle = absoluteAngle;
        this.sector = sector;
    }

    public static DirectionalMovement predict(double deltaX, double deltaZ, double yaw) {
        double deltaXZ = Math.hypot(deltaX, deltaZ);

        if (deltaXZ < MOVE_EPS) {
            return new DirectionalMovement(
                    deltaX,
                    deltaZ,
                    deltaXZ,
                    yaw,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    MovementSector.STATIONARY
            );
        }

        double moveX = deltaX / deltaXZ;
        double moveZ = deltaZ / deltaXZ;

        double rad = Math.toRadians(yaw);

        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);

        double dot = clamp((forwardX * moveX) + (forwardZ * moveZ), -1.0D, 1.0D);
        double cross = clamp((forwardX * moveZ) - (forwardZ * moveX), -1.0D, 1.0D);

        double signedAngle = Math.toDegrees(Math.atan2(cross, dot));
        double absoluteAngle = Math.abs(signedAngle);

        MovementSector sector = classifySector(signedAngle, absoluteAngle);

        return new DirectionalMovement(
                deltaX,
                deltaZ,
                deltaXZ,
                yaw,
                moveX,
                moveZ,
                forwardX,
                forwardZ,
                dot,
                cross,
                signedAngle,
                absoluteAngle,
                sector
        );
    }

    public boolean isMoving() {
        return deltaXZ > MOVE_EPS;
    }

    public RelativeMove toSimpleRelativeMove() {
        if (!isMoving()) {
            return RelativeMove.STATIONARY;
        }

        if (absoluteAngle <= 67.5D) {
            return RelativeMove.FORWARD;
        }

        if (absoluteAngle >= 112.5D) {
            return RelativeMove.BACKWARD;
        }

        return cross > 0.0D ? RelativeMove.RIGHT : RelativeMove.LEFT;
    }

    public static float wrapDegrees(float value) {
        value %= 360.0F;

        if (value >= 180.0F) {
            value -= 360.0F;
        }

        if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    public static float yawDifference(float first, float second) {
        return Math.abs(wrapDegrees(first - second));
    }

    private static MovementSector classifySector(double signedAngle, double absoluteAngle) {
        if (absoluteAngle < 22.5D) {
            return MovementSector.FORWARD;
        }

        if (absoluteAngle < 67.5D) {
            return signedAngle > 0.0D ? MovementSector.FORWARD_RIGHT : MovementSector.FORWARD_LEFT;
        }

        if (absoluteAngle < 112.5D) {
            return signedAngle > 0.0D ? MovementSector.RIGHT : MovementSector.LEFT;
        }

        if (absoluteAngle < 157.5D) {
            return signedAngle > 0.0D ? MovementSector.BACKWARD_RIGHT : MovementSector.BACKWARD_LEFT;
        }

        return MovementSector.BACKWARD;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}