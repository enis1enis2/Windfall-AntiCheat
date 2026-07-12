package io.windfall.anticheat.core.player;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class WindfallPlayer {

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

    private final UUID uuid;
    private final String name;
    private final Player player;
    private final User user;

    private ClientVersion clientVersion;
    private int protocolVersion;

    private double x, y, z;
    private double lastX, lastY, lastZ;
    // Three-deep history needed for acceleration and jerk calculations in movement checks
    private double lastLastX, lastLastY, lastLastZ;

    private double deltaX, deltaY, deltaZ;

    // Standard player bounding box: 0.6 wide × 1.8 tall (MC default since 1.0)
    private double width = 0.6;
    private double height = 1.8;

    private boolean onGround;
    private boolean lastOnGround;
    private boolean serverOnGround;

    private boolean sprinting;
    private boolean sneaking;
    private boolean flying;
    private boolean swimming;
    private boolean climbing;
    private boolean gliding;
    private double elytraMomentum;
    private int glideStartTick;

    private Pose pose = Pose.STANDING;

    // Ping via our own transaction system, not Bukkit API — gives sub-tick accuracy
    private int transactionPing;
    private int transactionId;

    private double velocityX, velocityY, velocityZ;
    private double serverVelocityX, serverVelocityY, serverVelocityZ;
    private boolean velocityReceived;

    private boolean allowFlight;

    private double teleportX, teleportY, teleportZ;

    private double groundX, groundY, groundZ;

    // ConcurrentHashMap required: checks read from Netty, ticks run on main thread
    private final ConcurrentHashMap<String, Integer> violationLevels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> buffers = new ConcurrentHashMap<>();

    private int attackCooldown;
    private long lastAttackTime;

    private int tickCount;
    private long joinTime;

    private float yaw, pitch;
    private float lastYaw, lastPitch;

    private boolean movedSinceTick;
    private boolean valid = true;

    private BedrockInfo bedrockInfo;
    private boolean alertsEnabled = true;

    // Set after RESPAWN packet to suppress false-positive flags from ViaVersion respawn desync
    private boolean respawned;

    public WindfallPlayer(Player player, User user) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.player = player;
        this.user = user;
        this.clientVersion = user.getClientVersion();
        this.protocolVersion = clientVersion.getProtocolVersion();
        this.joinTime = System.currentTimeMillis();
    }

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

    public double getDeltaX() { return deltaX; }
    public double getDeltaZ() { return deltaZ; }

    public double getHorizontalSpeed() {
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    public double getVerticalSpeed() {
        return Math.abs(deltaY);
    }

    public double getDistanceSq(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    // Position roll: lastLast ← last ← current each tick
    public void setPosition(double x, double y, double z) {
        this.lastLastX = this.lastX;
        this.lastLastY = this.lastY;
        this.lastLastZ = this.lastZ;
        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;
        this.x = x;
        this.y = y;
        this.z = z;
        this.deltaX = x - this.lastX;
        this.deltaY = y - this.lastY;
        this.deltaZ = z - this.lastZ;
        this.tickCount++;
    }

    // Must run BEFORE checks each tick so lastOnGround reflects previous tick state
    public void resetTickState() {
        this.movedSinceTick = false;
        this.lastOnGround = this.onGround;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
    }

    public void updateGroundPosition() {
        if (onGround) {
            this.groundX = x;
            this.groundY = y;
            this.groundZ = z;
        }
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public Player getPlayer() { return player; }
    public User getUser() { return user; }
    public ClientVersion getClientVersion() { return clientVersion; }
    public void setClientVersion(ClientVersion clientVersion) { this.clientVersion = clientVersion; this.protocolVersion = clientVersion.getProtocolVersion(); }
    public int getProtocolVersion() { return protocolVersion; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }
    public double getLastLastX() { return lastLastX; }
    public double getLastLastY() { return lastLastY; }
    public double getLastLastZ() { return lastLastZ; }
    public double getDeltaY() { return deltaY; }

    public double getWidth() { return width; }
    public void setHeight(double height) { this.height = height; }

    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean onGround) {
        this.lastOnGround = this.onGround;
        this.onGround = onGround;
        if (onGround) updateGroundPosition();
    }
    public boolean isLastOnGround() { return lastOnGround; }
    public boolean isServerOnGround() { return serverOnGround; }
    public void setServerOnGround(boolean serverOnGround) { this.serverOnGround = serverOnGround; }

    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    public boolean isSneaking() { return sneaking; }
    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        if (sneaking && pose == Pose.STANDING) pose = Pose.SNEAKING;
        else if (!sneaking && pose == Pose.SNEAKING) pose = Pose.STANDING;
    }
    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    public boolean isSwimming() { return swimming; }
    public void setSwimming(boolean swimming) {
        this.swimming = swimming;
        if (swimming) pose = Pose.SWIMMING;
        else if (pose == Pose.SWIMMING) pose = Pose.STANDING;
    }
    public boolean isClimbing() { return climbing; }
    public void setClimbing(boolean climbing) { this.climbing = climbing; }
    public boolean isGliding() { return gliding; }
    public void setGliding(boolean gliding) {
        this.gliding = gliding;
        if (gliding) pose = Pose.FALL_FLYING;
        else if (pose == Pose.FALL_FLYING) pose = Pose.STANDING;
    }
    public double getElytraMomentum() { return elytraMomentum; }
    public void setElytraMomentum(double elytraMomentum) { this.elytraMomentum = elytraMomentum; }
    public int getGlideStartTick() { return glideStartTick; }
    public void setGlideStartTick(int glideStartTick) { this.glideStartTick = glideStartTick; }

    public Pose getPose() { return pose; }
    public void setPose(Pose pose) {
        this.pose = pose;
        this.sneaking = (pose == Pose.SNEAKING);
        this.swimming = (pose == Pose.SWIMMING);
        this.gliding = (pose == Pose.FALL_FLYING);
    }
    public boolean isLongJumping() { return pose == Pose.LONG_JUMPING; }
    public void setLongJumping(boolean longJumping) {
        if (longJumping) pose = Pose.LONG_JUMPING;
        else if (pose == Pose.LONG_JUMPING) pose = Pose.STANDING;
    }
    public boolean isDying() { return pose == Pose.DYING; }

    public int getTransactionPing() { return transactionPing; }
    public void setTransactionPing(int transactionPing) { this.transactionPing = transactionPing; }
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

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

    public double getTeleportX() { return teleportX; }
    public double getTeleportY() { return teleportY; }
    public double getTeleportZ() { return teleportZ; }
    public void setTeleportPosition(double x, double y, double z) { this.teleportX = x; this.teleportY = y; this.teleportZ = z; }

    public double getGroundX() { return groundX; }
    public double getGroundY() { return groundY; }
    public double getGroundZ() { return groundZ; }

    public ConcurrentHashMap<String, Integer> getViolationLevels() { return violationLevels; }
    public ConcurrentHashMap<String, Double> getBuffers() { return buffers; }

    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int attackCooldown) { this.attackCooldown = attackCooldown; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }

    public int getTickCount() { return tickCount; }
    public long getJoinTime() { return joinTime; }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }

    public boolean isMovedSinceTick() { return movedSinceTick; }
    public void setMovedSinceTick(boolean movedSinceTick) { this.movedSinceTick = movedSinceTick; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public BedrockInfo getBedrockInfo() { return bedrockInfo; }
    public void setBedrockInfo(BedrockInfo bedrockInfo) { this.bedrockInfo = bedrockInfo; }
    public boolean isBedrock() { return bedrockInfo != null; }

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }

    public boolean isRespawned() { return respawned; }
    public void setRespawned(boolean respawned) { this.respawned = respawned; }

    // Iterates all check VLs — called frequently by PunishmentEngine and SeverityManager
    public int getTotalViolationLevel() {
        int total = 0;
        for (int v : violationLevels.values()) {
            total += v;
        }
        return total;
    }
}
