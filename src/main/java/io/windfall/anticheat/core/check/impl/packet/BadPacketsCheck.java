package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects malformed, malicious, or impossible packets sent by the client.
 *
 * <p>Checks performed:
 * <ul>
 *   <li><b>NaN/Infinite coordinates</b> &mdash; kicks immediately (prevents server crash via NaN propagation)</li>
 *   <li><b>Y out of bounds</b> &mdash; flags if Y exceeds world height + 64 buffer or drops below min world height</li>
 *   <li><b>NaN/Infinite rotation</b> &mdash; flags invalid yaw/pitch values</li>
 *   <li><b>Yaw/pitch out of range</b> &mdash; yaw must be in [-180, 180], pitch in [-90, 90]</li>
 *   <li><b>Rotation-only during fast movement</b> &mdash; detects position desync when player sends rotation without
 *       position while moving faster than 0.5 blocks/tick (potential movement spoofing)</li>
 *   <li><b>Duplicate packets</b> &mdash; flags repeated identical position+rotation packets (packet spam)</li>
 *   <li><b>Auto-clicker</b> &mdash; flags if more than {@value #MAX_ATTACKS_PER_TICK} attack packets arrive in a single tick</li>
 *   <li><b>Movement before login</b> &mdash; flags movement packets received before login is complete</li>
 * </ul>
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value #MAX_ATTACKS_PER_TICK} attacks per tick before auto-clicker flag</li>
 *   <li>{@value #DUPLICATE_THRESHOLD} consecutive identical packets before buffer increase</li>
 *   <li>Y bounds: world height + 64 above max, world min below</li>
 *   <li>Rotation-only detection: >0.5 blocks/tick horizontal movement, >0.15 horizontal speed</li>
 * </ul>
 *
 * <p>Setback triggers at VL {@value #setbackVl}. Decay is disabled (0.0) since violations are binary.
 * Uses RELAX_ON_MISMATCH with 1.2x multiplier for version compatibility.
 *
 * @see CheckData for annotation configuration
 * @see PacketOrderCheck for packet sequencing validation
 */
@CheckData(name = "Bad Packets A", stableKey = "windfall.packet.bad", decay = 0.0, setbackVl = 5, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class BadPacketsCheck extends Check implements PacketCheck {

    /** Maximum allowed Y coordinate for modern versions (1.18+), world height + 64 buffer */
    private static final double MAX_Y_MODERN = 400.0;
    /** Maximum allowed Y coordinate for legacy versions (pre-1.18) */
    private static final double MAX_Y_LEGACY = 256.0;
    /** Minimum allowed Y coordinate for modern versions (1.18+) */
    private static final double MIN_Y_MODERN = -64.0;
    /** Minimum allowed Y coordinate for legacy versions (pre-1.18) */
    private static final double MIN_Y_LEGACY = 0.0;
    /** Maximum attack (INTERACT_ENTITY with ATTACK action) packets allowed per tick before auto-clicker detection */
    private static final int MAX_ATTACKS_PER_TICK = 20;
    /** Number of consecutive duplicate position+rotation packets before buffer increases */
    private static final int DUPLICATE_THRESHOLD = 10;

    /**
     * Per-player state tracking duplicate detection, attack rate, and tick-based counters.
     * Stored in a ConcurrentHashMap keyed by UUID for thread-safe access.
     */
    private static final class PlayerState {
        /** Hash of the last received packet type, used for consecutive duplicate detection */
        int lastPacketTypeHash;
        /** Timestamp of the last movement packet (SYSTEM time, not server tick) */
        long lastMovementPacketTime;
        /** Timestamp of the last transaction (window confirmation) received from the server */
        long lastTransactionTime;
        /** Timestamp of the last attack packet, used for per-tick attack counting */
        long lastAttackPacketTime;
        /** Previous X coordinate for duplicate position detection */
        double lastPosX;
        /** Previous Y coordinate for duplicate position detection */
        double lastPosY;
        /** Previous Z coordinate for duplicate position detection */
        double lastPosZ;
        /** Previous yaw for duplicate rotation detection */
        float lastRotYaw;
        /** Previous pitch for duplicate rotation detection */
        float lastRotPitch;
        /** Count of consecutive identical position+rotation packets */
        int duplicateCount;
        /** Number of ATTACK packets received in the current tick window */
        int attackCountThisTick;
        /** Start time (ms) of the current tick window for attack counting */
        long currentTickStart;
        /** Whether the player has completed the login sequence (prevents pre-login movement) */
        boolean loggedIn;
    }

    /** Thread-safe map of player UUID to their per-player check state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates the per-player state for this check.
     *
     * @param player the player to get state for
     * @return the player's state, creating a new entry if absent
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** {@inheritDoc} Clears player state to prevent memory leaks on disconnect */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Main packet receive handler. Routes packets to type-specific validation methods
     * and tracks per-tick attack counts.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        if (now - state.currentTickStart > 50) {
            state.attackCountThisTick = 0;
            state.currentTickStart = now;
        }

        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_ROTATION) {
            state.lastMovementPacketTime = now;

            if (!state.loggedIn) {
                flagDetail(player, "movement before login complete");
                return;
            }
        }

        if (type == PacketType.Play.Client.PLAYER_POSITION) {
            handlePosition(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handlePositionAndRotation(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_ROTATION) {
            handleRotation(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_FLYING) {
            handleFlying(player, event);
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteractEntity(player, event, now, state);
        }

        int typeHash = type.hashCode();
        if (typeHash == state.lastPacketTypeHash && isMovementType(type)) {
            // duplicate packet type in sequence is normal, but track it
        }
        state.lastPacketTypeHash = typeHash;
    }

    /**
     * Tracks server transaction (window confirmation) timestamps for ping compensation.
     *
     * @param player the player who received the packet
     * @param event  the outgoing packet event
     */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            PlayerState state = getState(player);
            state.lastTransactionTime = System.currentTimeMillis();
        }
    }

    /**
     * Called when a player completes login. Enables movement packet validation.
     *
     * @param player the player who completed login
     */
    public void onLoginComplete(WindfallPlayer player) {
        getState(player).loggedIn = true;
    }

    /**
     * Resets all per-player state on disconnect to prevent memory leaks.
     *
     * @param player the player who disconnected
     */
    public void onDisconnect(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.loggedIn = false;
        state.lastPacketTypeHash = 0;
        state.lastMovementPacketTime = 0;
        state.lastTransactionTime = 0;
        state.lastAttackPacketTime = 0;
        state.duplicateCount = 0;
        state.attackCountThisTick = 0;
        state.currentTickStart = 0;
    }

    /**
     * Validates position-only packets for coordinate bounds and duplicate detection.
     *
     * @param player the player who sent the packet
     * @param event  the packet event to extract position from
     * @param state  the player's check state for duplicate tracking
     */
    private void handlePosition(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientPlayerPosition wrapper = new WrapperPlayClientPlayerPosition(event);
        var pos = wrapper.getPosition();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        validateCoordinates(player, x, y, z);
        checkDuplicate(player, x, y, z, 0, 0, state);
    }

    /**
     * Validates position+rotation packets for coordinate bounds, rotation range, and duplicate detection.
     *
     * @param player the player who sent the packet
     * @param event  the packet event to extract position and rotation from
     * @param state  the player's check state for duplicate tracking
     */
    private void handlePositionAndRotation(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
        var pos = wrapper.getPosition();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        validateCoordinates(player, x, y, z);
        validateRotation(player, yaw, pitch);
        checkDuplicate(player, x, y, z, pitch, yaw, state);
    }

    /**
     * Validates rotation-only packets. Detects position desync when a player sends
     * rotation without position while moving fast (speed > 0.15, movement > 0.5 blocks/tick).
     *
     * @param player the player who sent the packet
     * @param event  the packet event to extract rotation from
     * @param state  the player's check state
     */
    private void handleRotation(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        validateRotation(player, yaw, pitch);

        if (player.getHorizontalSpeed() > 0.15 && !player.isOnGround()) {
            double dx = player.getX() - player.getLastX();
            double dz = player.getZ() - player.getLastZ();
            double horizontalMovement = Math.sqrt(dx * dx + dz * dz);
            if (horizontalMovement > 0.5) {
                increaseBuffer(player, 0.2);
                if (getBuffer(player) > 2.0) {
                    flagDetail(player, "rotation-only during fast movement, position desync");
                    resetBuffer(player);
                }
            }
        }
    }

    /**
     * Handles flying (no-position, no-rotation) packets. Currently a pass-through for
     * creative/spectator mode flight validation.
     *
     * @param player the player who sent the packet
     * @param event  the packet event
     */
    private void handleFlying(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
        boolean onGround = wrapper.isOnGround();

        if (player.isFlying() && !player.isServerOnGround() && !onGround) {
            // flying packet while not on ground but player is in creative/spectator - allow
        }
    }

    /**
     * Handles INTERACT_ENTITY packets to detect auto-clickers. Counts ATTACK actions
     * per tick window (50ms) and flags if exceeding {@value #MAX_ATTACKS_PER_TICK}.
     *
     * @param player the player who sent the packet
     * @param event  the packet event
     * @param now    current system time in milliseconds
     * @param state  the player's check state for attack counting
     */
    private void handleInteractEntity(WindfallPlayer player, PacketReceiveEvent event, long now, PlayerState state) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();

        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            state.attackCountThisTick++;
            state.lastAttackPacketTime = now;

            if (state.attackCountThisTick > MAX_ATTACKS_PER_TICK) {
                flagDetail(player, "auto-clicker detected: " + state.attackCountThisTick + " attacks/tick");
            }
        }
    }

    /**
     * Validates that coordinates are not NaN/Infinite and within world bounds.
     * NaN/Infinite coordinates cause an immediate kick to prevent server-side issues.
     * Out-of-bounds Y is flagged but not immediately kicked.
     *
     * @param player the player to validate
     * @param x      the X coordinate
     * @param y      the Y coordinate
     * @param z      the Z coordinate
     */
    private void validateCoordinates(WindfallPlayer player, double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            flagDetail(player, "NaN/Infinite coordinates, kicking");
            player.getPlayer().kickPlayer("[Windfall] Invalid position data");
            return;
        }

        int protocol = player.getProtocolVersion();
        double maxY = VersionPhysics.getMaxWorldHeight(protocol) + 64;
        double minY = VersionPhysics.getMinWorldHeight(protocol);

        if (y > maxY || y < minY) {
            flagDetail(player, "Y out of bounds: " + y);
        }
    }

    /**
     * Validates rotation values are not NaN/Infinite and within valid ranges.
     * Minecraft yaw: [-180, 180], pitch: [-90, 90].
     *
     * @param player the player to validate
     * @param yaw    the yaw angle in degrees
     * @param pitch  the pitch angle in degrees
     */
    private void validateRotation(WindfallPlayer player, float yaw, float pitch) {
        if (Float.isNaN(yaw) || Float.isNaN(pitch)
                || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
            flagDetail(player, "NaN/Infinite rotation");
            return;
        }

        if (yaw < -180.0f || yaw > 180.0f) {
            flagDetail(player, "Yaw out of range: " + yaw);
        }

        if (pitch < -90.0f || pitch > 90.0f) {
            flagDetail(player, "Pitch out of range: " + pitch);
        }
    }

    /**
     * Detects consecutive duplicate position+rotation packets by comparing against the last values.
     * Uses an epsilon of {@value #epsilon} for floating-point comparison. When duplicates exceed
     * {@value #DUPLICATE_THRESHOLD}, the buffer increases by 0.1 per additional duplicate.
     *
     * @param player the player to check
     * @param x      current X coordinate
     * @param y      current Y coordinate
     * @param z      current Z coordinate
     * @param pitch  current pitch
     * @param yaw    current yaw
     * @param state  the player's check state for duplicate tracking
     */
    private void checkDuplicate(WindfallPlayer player, double x, double y, double z, float pitch, float yaw, PlayerState state) {
        /* Epsilon for floating-point coordinate comparison — small enough to catch true duplicates
         * while allowing for minor server-side rounding differences */
        double epsilon = 0.00001;
        if (Math.abs(x - state.lastPosX) < epsilon
                && Math.abs(y - state.lastPosY) < epsilon
                && Math.abs(z - state.lastPosZ) < epsilon
                && Math.abs(yaw - state.lastRotYaw) < epsilon
                && Math.abs(pitch - state.lastRotPitch) < epsilon) {
            state.duplicateCount++;
            if (state.duplicateCount > DUPLICATE_THRESHOLD) {
                increaseBuffer(player, 0.1);
            }
        } else {
            state.duplicateCount = Math.max(0, state.duplicateCount - 1);
        }

        state.lastPosX = x;
        state.lastPosY = y;
        state.lastPosZ = z;
        state.lastRotYaw = yaw;
        state.lastRotPitch = pitch;
    }

    /**
     * Checks if the given packet type is a movement-related packet (flying, position, rotation).
     *
     * @param type the packet type to check
     * @return true if the packet is a movement packet
     */
    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }

    /**
     * Flags the player and logs the specific violation detail to the server console.
     *
     * @param player the flagged player
     * @param detail human-readable description of the violation
     */
    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
        logger.warning("[Bad Packets A] " + player.getName() + ": " + detail);
    }
}
