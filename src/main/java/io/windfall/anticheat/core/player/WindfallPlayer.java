package io.windfall.anticheat.core.player;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.player.data.ActionData;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Tracks all anti-cheat state for a single online player.
 *
 * <p>One instance per connected player, created at LOGIN_SUCCESS and destroyed on quit.
 * Stores position history, velocity, rotation, ground state, pose, and per-check
 * violation levels and buffers.
 *
 * <p>Thread safety: compound state groups (position, ground, rotation) are stored as
 * immutable snapshots published via {@code volatile} references. This guarantees that
 * readers (checks on Netty threads, ticks on the main thread) always see a consistent
 * state — no torn reads are possible. Simple flags use {@code volatile} directly.
 *
 * <p>Position tracking uses a three-deep roll (current → last → lastLast)
 * to enable acceleration and jerk calculations in movement checks.
 * Deltas are computed automatically in {@link #setPosition}.
 *
 * @see PlayerManager for the player registry
 * @see io.windfall.anticheat.core.check.Check for per-check VL/buffer storage
 */
public class WindfallPlayer {

    /** Player pose states matching vanilla Minecraft — affects bounding box height and eye height */
    public enum Pose {
        STANDING,
        FALL_FLYING,
        SWIMMING,
        SLEEPING,
        SPIN_ATTACK,
        SNEAKING,
        DYING,
        LONG_JUMPING
    }

    // === IMMUTABLE STATE SNAPSHOTS ===
    // Each snapshot is a frozen, consistent view of a compound state group.
    // Published via volatile reference — readers always see a complete, non-torn state.

    /** Immutable position + delta + tick state — published atomically from Netty, read from checks and main thread */
    static final class PositionState {
        final double x, y, z;
        final double lastX, lastY, lastZ;
        final double lastLastX, lastLastY, lastLastZ;
        final double deltaX, deltaY, deltaZ;
        final int tickCount;

        PositionState(double x, double y, double z,
                      double lastX, double lastY, double lastZ,
                      double lastLastX, double lastLastY, double lastLastZ,
                      double deltaX, double deltaY, double deltaZ,
                      int tickCount) {
            this.x = x; this.y = y; this.z = z;
            this.lastX = lastX; this.lastY = lastY; this.lastZ = lastZ;
            this.lastLastX = lastLastX; this.lastLastY = lastLastY; this.lastLastZ = lastLastZ;
            this.deltaX = deltaX; this.deltaY = deltaY; this.deltaZ = deltaZ;
            this.tickCount = tickCount;
        }
    }

    /** Immutable ground state — published atomically, prevents torn onGround/lastOnGround reads */
    static final class GroundState {
        final boolean onGround;
        final boolean lastOnGround;
        final double groundX, groundY, groundZ;

        GroundState(boolean onGround, boolean lastOnGround,
                    double groundX, double groundY, double groundZ) {
            this.onGround = onGround;
            this.lastOnGround = lastOnGround;
            this.groundX = groundX;
            this.groundY = groundY;
            this.groundZ = groundZ;
        }
    }

    /** Immutable rotation state — published atomically, prevents torn yaw/lastYaw reads */
    static final class RotationState {
        final float yaw, pitch;
        final float lastYaw, lastPitch;

        RotationState(float yaw, float pitch, float lastYaw, float lastPitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.lastYaw = lastYaw;
            this.lastPitch = lastPitch;
        }
    }

    // === CORE IDENTITY (immutable, thread-safe by construction) ===

    private final UUID uuid;
    private final String name;
    private final Player player;
    private final User user;

    private volatile ClientVersion clientVersion;
    private volatile int protocolVersion;

    // === COMPOUND STATE (immutable snapshots, published atomically) ===

    /** Position + deltas + tick count — updated from Netty, read from checks and main thread */
    private volatile PositionState pos = new PositionState(0,0,0, 0,0,0, 0,0,0, 0,0,0, 0);

    /** Ground state + fallback setback position — updated from Netty and main thread */
    private volatile GroundState ground = new GroundState(false, false, 0,0,0);

    /** Rotation state — updated from Netty, read from checks and main thread */
    private volatile RotationState rotation = new RotationState(0, 0, 0, 0);

    // === SIMPLE FLAGS (volatile for cross-thread visibility) ===

    // Standard player bounding box: 0.6 wide × 1.8 tall (MC default since 1.0)
    private volatile double width = 0.6;
    private volatile double height = 1.8;

    /** Server-reported ground state — may lag behind client state by 1 tick */
    private volatile boolean serverOnGround;

    private volatile boolean sprinting;
    private volatile boolean sneaking;
    private volatile boolean flying;
    private volatile boolean swimming;
    private volatile boolean climbing;
    private volatile boolean gliding;
    private volatile double elytraMomentum;
    private volatile int glideStartTick;

    private volatile Pose pose = Pose.STANDING;

    // Ping via our own transaction system, not Bukkit API — gives sub-tick accuracy
    private volatile int transactionPing;
    private volatile int transactionId;

    /** Client-claimed velocity (from movement packets) */
    private volatile double velocityX, velocityY, velocityZ;
    /** Server-sent velocity (from ENTITY_VELOCITY packet) */
    private volatile double serverVelocityX, serverVelocityY, serverVelocityZ;
    private volatile boolean velocityReceived;

    private volatile boolean allowFlight;

    private volatile double teleportX, teleportY, teleportZ;

    // ConcurrentHashMap required: checks read from Netty, ticks run on main thread
    private final ConcurrentHashMap<String, Integer> violationLevels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> buffers = new ConcurrentHashMap<>();

    private volatile int attackCooldown;
    private volatile long lastAttackTime;

    private volatile long joinTime;

    private volatile boolean movedSinceTick;
    private volatile boolean valid = true;

    private volatile BedrockInfo bedrockInfo;
    private volatile boolean alertsEnabled = true;

    /** Tracks block-level actions (placement, breaking, piston pushes) for movement check exemptions */
    private final ActionData actionData = new ActionData(this);

    // === BYPASS RESISTANCE STATE ===
    // Latency-compensated world state for this player
    private volatile io.windfall.anticheat.core.compensation.CompensatedWorld compensatedWorld;

    // Set after RESPAWN packet to suppress false-positive flags from ViaVersion respawn desync
    private volatile boolean respawned;

    // Cached Bukkit API state — updated on main thread, read from Netty packet threads.
    // Prevents thread-unsafe cross-thread Bukkit API access in PredictionEngine.
    private volatile boolean cachedInWater;
    private volatile boolean cachedInLava;
    private volatile boolean cachedOnHoney;
    private volatile double cachedSpeedMultiplier = 1.0;
    private volatile double cachedSlownessMultiplier = 1.0;
    private volatile boolean cachedHasSlowFalling;
    private volatile boolean cachedHasLevitation;
    private volatile double cachedLevitationAmplifier = 1.0;
    private volatile boolean cachedIsFallFlying;
    private volatile boolean cachedHasRiptide;

    /**
     * Creates a WindfallPlayer from a Bukkit Player and PacketEvents User.
     * Called once at LOGIN_SUCCESS from {@link io.windfall.anticheat.core.network.PacketListener}.
     */
    public WindfallPlayer(Player player, User user) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.player = player;
        this.user = user;
        this.clientVersion = user.getClientVersion();
        this.protocolVersion = clientVersion.getProtocolVersion();
        this.joinTime = System.currentTimeMillis();
    }

    // === POSITION: immutable snapshot published atomically ===

    /**
     * Returns the bounding box height for the current pose.
     *
     * <p>Heights vary by pose and protocol version:
     * <ul>
     *   <li>STANDING: 1.8 (all versions)</li>
     *   <li>SNEAKING: 1.5 (1.14+, protocol ≥477) or 1.62 (older)</li>
     *   <li>FALL_FLYING/SWIMMING/SPIN_ATTACK: 0.6</li>
     *   <li>SLEEPING: 0.2</li>
     *   <li>DYING: 0.0</li>
     * </ul>
     */
    public double getHeight() {
        switch (pose) {
            case FALL_FLYING: return 0.6;
            case SWIMMING: return 0.6;
            case SPIN_ATTACK: return 0.6;
            case SLEEPING: return 0.2;
            case DYING: return 0.0;
            case SNEAKING: return protocolVersion >= 477 ? 1.5 : 1.62;
            case LONG_JUMPING: return protocolVersion >= 477 ? 1.5 : 1.8;
            case STANDING:
            default: return 1.8;
        }
    }

    /**
     * Returns the eye height for the current pose (offset from feet to eye level).
     *
     * <p>Used by reach and hitbox checks to compute the player's look vector origin.
     * Eye height follows the same pose/version scaling as {@link #getHeight()}.
     */
    public double getEyeHeight() {
        switch (pose) {
            case FALL_FLYING: return 0.4;
            case SWIMMING: return 0.4;
            case SPIN_ATTACK: return 0.4;
            case SLEEPING: return 0.2;
            case DYING: return 0.0;
            case SNEAKING: return protocolVersion >= 477 ? 1.27 : 1.54;
            case LONG_JUMPING: return protocolVersion >= 477 ? 1.27 : 1.62;
            case STANDING:
            default: return 1.62;
        }
    }

    public double getDeltaX() { return pos.deltaX; }
    public double getDeltaZ() { return pos.deltaZ; }

    /** Returns horizontal speed: sqrt(deltaX² + deltaZ²) */
    public double getHorizontalSpeed() {
        return Math.sqrt(pos.deltaX * pos.deltaX + pos.deltaZ * pos.deltaZ);
    }

    /** Returns absolute vertical speed: |deltaY| */
    public double getVerticalSpeed() {
        return Math.abs(pos.deltaY);
    }

    /** Returns squared distance from this player to the given point — avoids sqrt for comparisons */
    public double getDistanceSq(double x, double y, double z) {
        double dx = this.pos.x - x;
        double dy = this.pos.y - y;
        double dz = this.pos.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Updates position and rolls the three-deep history.
     *
     * <p>Called from {@link io.windfall.anticheat.core.network.PacketListener} on each
     * position packet. Creates a new immutable snapshot and publishes it atomically —
     * readers always see a consistent position/delta/tick state.
     */
    public void setPosition(double x, double y, double z) {
        PositionState old = this.pos;
        this.pos = new PositionState(
            x, y, z,
            old.x, old.y, old.z,
            old.lastX, old.lastY, old.lastZ,
            x - old.x, y - old.y, z - old.z,
            old.tickCount + 1
        );
    }

    /**
     * Resets per-tick state: clears movement flag and rolls ground/rotation history.
     *
     * <p>Must run BEFORE checks each tick so that {@link #isLastOnGround()} reflects
     * the previous tick's ground state. Called by {@link io.windfall.anticheat.core.check.CheckManager#onTick()}.
     *
     * <p>Creates new immutable snapshots for ground and rotation with rolled history —
     * guarantees atomic transition from current → last state.
     */
    public void resetTickState() {
        this.movedSinceTick = false;
        // Roll ground history atomically
        GroundState g = this.ground;
        this.ground = new GroundState(g.onGround, g.onGround, g.groundX, g.groundY, g.groundZ);
        // Roll rotation history atomically
        RotationState r = this.rotation;
        this.rotation = new RotationState(r.yaw, r.pitch, r.yaw, r.pitch);
    }

    /**
     * Updates ground state and records ground position if now grounded.
     * Creates a new immutable snapshot — prevents torn onGround/lastOnGround reads.
     */
    public void setOnGround(boolean onGround) {
        GroundState old = this.ground;
        double gx = onGround ? pos.x : old.groundX;
        double gy = onGround ? pos.y : old.groundY;
        double gz = onGround ? pos.z : old.groundZ;
        this.ground = new GroundState(onGround, old.onGround, gx, gy, gz);
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public Player getPlayer() { return player; }
    public User getUser() { return user; }
    public ClientVersion getClientVersion() { return clientVersion; }
    public void setClientVersion(ClientVersion clientVersion) { this.clientVersion = clientVersion; this.protocolVersion = clientVersion.getProtocolVersion(); }
    public int getProtocolVersion() { return protocolVersion; }

    // Position getters — read from immutable snapshot
    public double getX() { return pos.x; }
    public double getY() { return pos.y; }
    public double getZ() { return pos.z; }
    public double getLastX() { return pos.lastX; }
    public double getLastY() { return pos.lastY; }
    public double getLastZ() { return pos.lastZ; }
    public double getLastLastX() { return pos.lastLastX; }
    public double getLastLastY() { return pos.lastLastY; }
    public double getLastLastZ() { return pos.lastLastZ; }
    public double getDeltaY() { return pos.deltaY; }
    public int getTickCount() { return pos.tickCount; }

    // Ground getters — read from immutable snapshot
    public double getWidth() { return width; }
    public void setHeight(double height) { this.height = height; }

    public boolean isOnGround() { return ground.onGround; }
    public boolean isLastOnGround() { return ground.lastOnGround; }
    public boolean isServerOnGround() { return serverOnGround; }
    public void setServerOnGround(boolean serverOnGround) { this.serverOnGround = serverOnGround; }

    // Ground position getters — read from immutable snapshot
    public double getGroundX() { return ground.groundX; }
    public double getGroundY() { return ground.groundY; }
    public double getGroundZ() { return ground.groundZ; }

    // State flag getters/setters — volatile for cross-thread visibility
    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    public boolean isSneaking() { return sneaking; }
    /** Updates sneak state and automatically transitions pose between SNEAKING ↔ STANDING */
    public synchronized void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        if (sneaking && pose == Pose.STANDING) pose = Pose.SNEAKING;
        else if (!sneaking && pose == Pose.SNEAKING) pose = Pose.STANDING;
    }
    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    public boolean isSwimming() { return swimming; }
    /** Updates swim state and automatically transitions pose between SWIMMING ↔ STANDING */
    public synchronized void setSwimming(boolean swimming) {
        this.swimming = swimming;
        if (swimming) pose = Pose.SWIMMING;
        else if (pose == Pose.SWIMMING) pose = Pose.STANDING;
    }
    public boolean isClimbing() { return climbing; }
    public void setClimbing(boolean climbing) { this.climbing = climbing; }
    public boolean isGliding() { return gliding; }
    /** Updates glide state and automatically transitions pose between FALL_FLYING ↔ STANDING */
    public synchronized void setGliding(boolean gliding) {
        this.gliding = gliding;
        if (gliding) pose = Pose.FALL_FLYING;
        else if (pose == Pose.FALL_FLYING) pose = Pose.STANDING;
    }
    public double getElytraMomentum() { return elytraMomentum; }
    public void setElytraMomentum(double elytraMomentum) { this.elytraMomentum = elytraMomentum; }
    public int getGlideStartTick() { return glideStartTick; }
    public void setGlideStartTick(int glideStartTick) { this.glideStartTick = glideStartTick; }

    public Pose getPose() { return pose; }
    /** Sets pose directly and synchronizes boolean flags (sneaking, swimming, gliding) */
    public void setPose(Pose pose) {
        this.pose = pose;
        this.sneaking = (pose == Pose.SNEAKING);
        this.swimming = (pose == Pose.SWIMMING);
        this.gliding = (pose == Pose.FALL_FLYING);
    }
    public boolean isLongJumping() { return pose == Pose.LONG_JUMPING; }
    /** Updates long-jumping state and transitions pose between LONG_JUMPING ↔ STANDING */
    public void setLongJumping(boolean longJumping) {
        if (longJumping) pose = Pose.LONG_JUMPING;
        else if (pose == Pose.LONG_JUMPING) pose = Pose.STANDING;
    }
    public boolean isDying() { return pose == Pose.DYING; }

    /** Returns the player's ping measured via our transaction system (sub-tick accuracy) */
    public int getTransactionPing() { return transactionPing; }
    public void setTransactionPing(int transactionPing) { this.transactionPing = transactionPing; }
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    // Velocity getters/setters — volatile for cross-thread visibility
    public double getVelocityX() { return velocityX; }
    public void setVelocityX(double velocityX) { this.velocityX = velocityX; }
    public double getVelocityY() { return velocityY; }
    public void setVelocityY(double velocityY) { this.velocityY = velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public void setVelocityZ(double velocityZ) { this.velocityZ = velocityZ; }
    public void setVelocity(double x, double y, double z) { this.velocityX = x; this.velocityY = y; this.velocityZ = z; }

    public double getServerVelocityX() { return serverVelocityX; }
    public void setServerVelocityX(double v) { this.serverVelocityX = v; }
    public double getServerVelocityY() { return serverVelocityY; }
    public void setServerVelocityY(double v) { this.serverVelocityY = v; }
    public double getServerVelocityZ() { return serverVelocityZ; }
    public void setServerVelocityZ(double v) { this.serverVelocityZ = v; }
    public boolean isVelocityReceived() { return velocityReceived; }
    public void setVelocityReceived(boolean v) { this.velocityReceived = v; }

    public boolean isAllowFlight() { return allowFlight; }
    public void setAllowFlight(boolean allowFlight) { this.allowFlight = allowFlight; }

    public void setProtocolVersion(int protocolVersion) { this.protocolVersion = protocolVersion; }

    // Teleport position — volatile for cross-thread visibility
    /** Returns the X coordinate of the last server-initiated teleport (used for setbacks) */
    public double getTeleportX() { return teleportX; }
    public double getTeleportY() { return teleportY; }
    public double getTeleportZ() { return teleportZ; }
    public void setTeleportPosition(double x, double y, double z) { this.teleportX = x; this.teleportY = y; this.teleportZ = z; }

    /** Returns the per-check violation levels map (keyed by stableKey) */
    public ConcurrentHashMap<String, Integer> getViolationLevels() { return violationLevels; }
    /** Returns the per-check buffer map (keyed by stableKey) — higher values = stronger detection confidence */
    public ConcurrentHashMap<String, Double> getBuffers() { return buffers; }

    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int attackCooldown) { this.attackCooldown = attackCooldown; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }

    public long getJoinTime() { return joinTime; }

    // Rotation getters — read from immutable snapshot
    public float getYaw() { return rotation.yaw; }
    public void setYaw(float yaw) {
        RotationState old = this.rotation;
        this.rotation = new RotationState(yaw, old.pitch, old.yaw, old.lastPitch);
    }
    public float getPitch() { return rotation.pitch; }
    public void setPitch(float pitch) {
        RotationState old = this.rotation;
        this.rotation = new RotationState(old.yaw, pitch, old.lastYaw, old.lastPitch);
    }
    public float getLastYaw() { return rotation.lastYaw; }
    public float getLastPitch() { return rotation.lastPitch; }

    public boolean isMovedSinceTick() { return movedSinceTick; }
    public void setMovedSinceTick(boolean movedSinceTick) { this.movedSinceTick = movedSinceTick; }

    /** Returns false after the player disconnects — causes packet callbacks to skip processing */
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public BedrockInfo getBedrockInfo() { return bedrockInfo; }
    public void setBedrockInfo(BedrockInfo bedrockInfo) { this.bedrockInfo = bedrockInfo; }
    /** Returns true if this player is a Bedrock client connected via Geyser */
    public boolean isBedrock() { return bedrockInfo != null; }

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }

    /** Returns the action data tracker for this player — provides block update/piston exemptions */
    public ActionData getActionData() { return actionData; }

    /** Returns the latency-compensated world for this player — used by bypass resistance engine */
    public io.windfall.anticheat.core.compensation.CompensatedWorld getCompensatedWorld() { return compensatedWorld; }
    /** Sets the latency-compensated world for this player */
    public void setCompensatedWorld(io.windfall.anticheat.core.compensation.CompensatedWorld world) { this.compensatedWorld = world; }

    /** Returns true if the player just respawned — used to suppress ViaVersion false positives */
    public boolean isRespawned() { return respawned; }
    public void setRespawned(boolean respawned) { this.respawned = respawned; }

    /**
     * Sums all check violation levels for this player.
     * Called frequently by PunishmentEngine and SeverityManager for tier evaluation.
     */
    public int getTotalViolationLevel() {
        int total = 0;
        for (int v : violationLevels.values()) {
            total += v;
        }
        return total;
    }

    // === Cached Bukkit state — safe to read from Netty threads ===

    /** Updates all cached Bukkit API state on the main thread. Called once per tick from CheckManager. */
    public void updateCachedState() {
        try {
            org.bukkit.Location loc = player.getLocation();
            org.bukkit.block.Block block = loc.getBlock();
            this.cachedInWater = io.windfall.anticheat.core.util.MaterialUtils.isWater(block.getType());
            this.cachedInLava = io.windfall.anticheat.core.util.MaterialUtils.isLava(block.getType());
            this.cachedOnHoney = io.windfall.anticheat.core.util.MaterialUtils.isHoney(
                loc.clone().subtract(0, 0.1, 0).getBlock().getType());
        } catch (Exception e) {
            this.cachedInWater = false;
            this.cachedInLava = false;
            this.cachedOnHoney = false;
        }
        try {
            this.cachedSpeedMultiplier = computePotionMultiplier("SPEED", 0.20, 5);
            this.cachedSlownessMultiplier = computePotionMultiplier("SLOW", -0.15, 4);
            this.cachedHasSlowFalling = hasPotionEffect("SLOW_FALLING");
            this.cachedHasLevitation = hasPotionEffect("LEVITATION");
            this.cachedLevitationAmplifier = getPotionAmplifier("LEVITATION");
        } catch (Exception e) {
            this.cachedSpeedMultiplier = 1.0;
            this.cachedSlownessMultiplier = 1.0;
            this.cachedHasSlowFalling = false;
            this.cachedHasLevitation = false;
            this.cachedLevitationAmplifier = 1.0;
        }
        try {
            java.lang.reflect.Method riptideMethod = player.getClass().getMethod("isRiptiding");
            this.cachedHasRiptide = (Boolean) riptideMethod.invoke(player);
            java.lang.reflect.Method glideMethod = player.getClass().getMethod("isGliding");
            this.cachedIsFallFlying = (Boolean) glideMethod.invoke(player);
        } catch (Exception e) {
            this.cachedHasRiptide = false;
            this.cachedIsFallFlying = false;
        }
    }

    private double computePotionMultiplier(String nameContains, double perLevel, int maxLevel) {
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            String effectName = effect.getType().getName().toUpperCase();
            if (effectName.contains(nameContains)) {
                int level = Math.min(effect.getAmplifier() + 1, maxLevel);
                return 1.0 + (perLevel * level);
            }
        }
        return 1.0;
    }

    private boolean hasPotionEffect(String nameContains) {
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getName().toUpperCase().contains(nameContains)) return true;
        }
        return false;
    }

    private double getPotionAmplifier(String nameContains) {
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getName().toUpperCase().contains(nameContains)) {
                return effect.getAmplifier() + 1;
            }
        }
        return 1.0;
    }

    public boolean isCachedInWater() { return cachedInWater; }
    public boolean isCachedInLava() { return cachedInLava; }
    public boolean isCachedOnHoney() { return cachedOnHoney; }
    public double getCachedSpeedMultiplier() { return cachedSpeedMultiplier; }
    public double getCachedSlownessMultiplier() { return cachedSlownessMultiplier; }
    public boolean isCachedHasSlowFalling() { return cachedHasSlowFalling; }
    public boolean isCachedHasLevitation() { return cachedHasLevitation; }
    public double getCachedLevitationAmplifier() { return cachedLevitationAmplifier; }
    public boolean isCachedIsFallFlying() { return cachedIsFallFlying; }
    public boolean isCachedHasRiptide() { return cachedHasRiptide; }
}
