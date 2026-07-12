package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Immutable snapshot of player state and pre-calculated predictions for a single tick.
 *
 * <p>Created once per movement packet and shared by all checks that need physics data.
 * This avoids redundant calculations — each check reads from the same snapshot rather
 * than independently computing deltas, speeds, and potion multipliers.
 *
 * <p>Design rationale: checks run in parallel (via CheckManager), so sharing mutable state
 * is unsafe. An immutable snapshot eliminates race conditions while keeping the API simple.
 *
 * <p>The constructor eagerly computes all derived values (horizontal speed, base speed,
 * predicted max speed) so checks only need to call getters.
 *
 * @see PredictionEngine for the underlying physics calculations
 * @see VersionPhysics for version-dependent constants
 * @see io.windfall.anticheat.core.check.impl.movement.SpeedCheck for a typical consumer
 */
public final class PredictionContext {

    // === RAW PLAYER STATE ===

    /** Current tick position (feet) */
    public final double x, y, z;

    /** Previous tick position (feet) — used for delta calculation */
    public final double lastX, lastY, lastZ;

    /** Two-ticks-ago position — enables second-order delta (acceleration) detection */
    public final double lastLastX, lastLastY, lastLastZ;

    /** Position deltas this tick (current - last) — the primary movement vector */
    public final double deltaX, deltaY, deltaZ;

    /** Ground state flags — server-side and previous-tick for ground spoof checks */
    public final boolean onGround, lastOnGround;

    /** Movement state flags — affect acceleration, drag, and friction calculations */
    public final boolean sprinting, sneaking, swimming, climbing;

    /** Client protocol version — used for version-dependent physics branching */
    public final int protocolVersion;

    // === DERIVED PHYSICS VALUES ===

    /** Horizontal speed this tick: sqrt(deltaX² + deltaZ²) */
    public final double horizontalSpeed;

    /** Horizontal speed last tick — used for acceleration limits */
    public final double lastHorizontalSpeed;

    /** Base walk/sprint/crouch speed after potion multipliers (before friction) */
    public final double baseSpeed;

    /** Predicted maximum horizontal speed for this tick — checks compare against this */
    public final double predictedMaxHorizontalSpeed;

    /** Fluid detection flags — affect drag, gravity, and vertical motion */
    public final boolean inWater, inLava;

    /** Potion effect flags — affect gravity and vertical motion */
    public final boolean hasSlowFalling, hasLevitation;

    /**
     * Creates a snapshot from the current state of a WindfallPlayer.
     * All values are eagerly copied and computed — the snapshot is fully independent
     * of the player's mutable state after construction.
     */
    public PredictionContext(WindfallPlayer player) {
        // Copy raw position state
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.lastX = player.getLastX();
        this.lastY = player.getLastY();
        this.lastZ = player.getLastZ();
        this.lastLastX = player.getLastLastX();
        this.lastLastY = player.getLastLastY();
        this.lastLastZ = player.getLastLastZ();

        // Compute position deltas (current - last)
        this.deltaX = player.getDeltaX();
        this.deltaY = player.getDeltaY();
        this.deltaZ = player.getDeltaZ();

        // Copy state flags
        this.onGround = player.isOnGround();
        this.lastOnGround = player.isLastOnGround();
        this.sprinting = player.isSprinting();
        this.sneaking = player.isSneaking();
        this.swimming = player.isSwimming();
        this.climbing = player.isClimbing();
        this.protocolVersion = player.getProtocolVersion();

        // Compute horizontal speed: sqrt(deltaX² + deltaZ²)
        this.horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Compute last tick's horizontal speed for acceleration comparison
        double ldx = lastX - lastLastX;
        double ldz = lastZ - lastLastZ;
        this.lastHorizontalSpeed = Math.sqrt(ldx * ldx + ldz * ldz);

        // Compute base speed and predicted max speed (the core prediction)
        double speedMult = PredictionEngine.getSpeedPotionMultiplier(player);
        double slowMult = PredictionEngine.getSlownessPotionMultiplier(player);
        this.baseSpeed = PredictionEngine.calculateBaseSpeed(sprinting, sneaking, speedMult, slowMult);
        this.predictedMaxHorizontalSpeed = PredictionEngine.calculateMaxHorizontalSpeed(
                baseSpeed, lastHorizontalSpeed, onGround, climbing, swimming, protocolVersion);

        // Detect fluid and potion states
        this.inWater = PredictionEngine.checkInWater(player);
        this.inLava = PredictionEngine.checkInLava(player);
        this.hasSlowFalling = PredictionEngine.checkSlowFalling(player);
        this.hasLevitation = PredictionEngine.checkLevitation(player);
    }
}
