package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;

// Immutable snapshot of player state and pre-calculated predictions for a single tick.
// Created once per movement packet and shared by all checks that need physics data.
public final class PredictionContext {

    // Raw player state
    public final double x, y, z;
    public final double lastX, lastY, lastZ;
    public final double lastLastX, lastLastY, lastLastZ;
    public final double deltaX, deltaY, deltaZ;
    public final boolean onGround, lastOnGround;
    public final boolean sprinting, sneaking, swimming, climbing;
    public final int protocolVersion;

    // Derived physics values
    public final double horizontalSpeed;
    public final double lastHorizontalSpeed;
    public final double baseSpeed;
    public final double predictedMaxHorizontalSpeed;
    public final boolean inWater, inLava;
    public final boolean hasSlowFalling, hasLevitation;

    public PredictionContext(WindfallPlayer player) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.lastX = player.getLastX();
        this.lastY = player.getLastY();
        this.lastZ = player.getLastZ();
        this.lastLastX = player.getLastLastX();
        this.lastLastY = player.getLastLastY();
        this.lastLastZ = player.getLastLastZ();
        this.deltaX = player.getDeltaX();
        this.deltaY = player.getDeltaY();
        this.deltaZ = player.getDeltaZ();
        this.onGround = player.isOnGround();
        this.lastOnGround = player.isLastOnGround();
        this.sprinting = player.isSprinting();
        this.sneaking = player.isSneaking();
        this.swimming = player.isSwimming();
        this.climbing = player.isClimbing();
        this.protocolVersion = player.getProtocolVersion();

        this.horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double ldx = lastX - lastLastX;
        double ldz = lastZ - lastLastZ;
        this.lastHorizontalSpeed = Math.sqrt(ldx * ldx + ldz * ldz);

        double speedMult = PredictionEngine.getSpeedPotionMultiplier(player);
        double slowMult = PredictionEngine.getSlownessPotionMultiplier(player);
        this.baseSpeed = PredictionEngine.calculateBaseSpeed(sprinting, sneaking, speedMult, slowMult);
        this.predictedMaxHorizontalSpeed = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, lastHorizontalSpeed, onGround, climbing, swimming, protocolVersion);

        this.inWater = PredictionEngine.checkInWater(player);
        this.inLava = PredictionEngine.checkInLava(player);
        this.hasSlowFalling = PredictionEngine.checkSlowFalling(player);
        this.hasLevitation = PredictionEngine.checkLevitation(player);
    }
}
