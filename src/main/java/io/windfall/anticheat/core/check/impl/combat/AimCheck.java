package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects inhuman aim patterns consistent with aim-assist or aimbot cheats.
 *
 * <p>This check monitors player rotation packets and applies two complementary
 * detection strategies:</p>
 *
 * <h3>1. Instant-Snap Detection</h3>
 * <p>Flags rotation deltas exceeding {@value #INSTANT_SNAP_THRESHOLD} degrees per
 * packet. A single snap that extreme is nearly impossible for a human and is a
 * strong signal of a teleport-to-target or hard-lock aimbot. The buffer
 * increments by 1.0 per snap and flags once it exceeds
 * {@value #SNAP_BUFFER_FLAG_THRESHOLD}.</p>
 *
 * <h3>2. Yaw-Variance / Pitch-Variance Ratio Analysis</h3>
 * <p>Over a rolling window of {@value #MIN_ROTATION_SAMPLES} rotation packets,
 * this check computes the average absolute yaw and pitch deltas. Legitimate
 * players exhibit correlated yaw and pitch movement when tracking a target.
 * A bot that locks horizontal aim while barely adjusting vertical aim produces
 * a high yaw average coupled with a low pitch variance. When
 * {@code avgYawDelta > 0.1} and {@code avgPitchDelta < 0.5} the buffer
 * increments; otherwise it decays.</p>
 *
 * <h3>Buffer &amp; Thresholds</h3>
 * <ul>
 *   <li>Snap buffer: flags at &gt; {@value #SNAP_BUFFER_FLAG_THRESHOLD},
 *       resets after flag.</li>
 *   <li>Variance buffer: flags at &gt; 5.0, increments by 0.3 per offending
 *       window, decays by 0.1 per clean window.</li>
 * </ul>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Aim A", stableKey = "windfall.combat.aim", decay = 0.01, setbackVl = 10)
public class AimCheck extends Check implements PacketCheck {

    /** Maximum yaw or pitch delta (in degrees) before a single-packet snap is flagged. */
    private static final double INSTANT_SNAP_THRESHOLD = 180.0;

    /** Full circle rotation used for yaw delta normalization to [-180, 180). */
    private static final float ROTATION_MODULO = 360.0f;

    /** Minimum rotation samples collected before the variance-ratio window is evaluated. */
    private static final int MIN_ROTATION_SAMPLES = 5;

    /**
     * Upper bound on average pitch delta that, combined with a high yaw delta,
     * suggests the player is locking horizontal aim without natural vertical
     * correction.
     */
    private static final double AIMBOT_YAW_VARIANCE_THRESHOLD = 0.5;

    /** Buffer level at which the snap-detection heuristic triggers a flag. */
    private static final double SNAP_BUFFER_FLAG_THRESHOLD = 3.0;

    /** Per-player mutable state for tracking rotation history and accumulators. */
    private static final class PlayerState {
        float lastYaw;
        float lastPitch;
        boolean hasRotation;
        double yawAccumulator;
        double pitchAccumulator;
        int rotationCount;
    }

    /** Player state lookup keyed by UUID. */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily initialises the per-player state for the given player.
     *
     * @param player the player whose state is requested
     * @return the current {@link PlayerState} snapshot
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Evicts cached state when a player leaves the server.
     *
     * @param uuid UUID of the player being removed
     */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming rotation packets and runs both detection heuristics.
     *
     * <p>Only {@code PLAYER_ROTATION} and {@code PLAYER_POSITION_AND_ROTATION}
     * packets are examined. For the first packet the previous-rotation baseline
     * is initialised and no further processing occurs.</p>
     *
     * @param player the player associated with this packet
     * @param event  the raw packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.PLAYER_ROTATION
                && type != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return;
        }

        PlayerState state = getState(player);
        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        if (!state.hasRotation) {
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            state.hasRotation = true;
            return;
        }

        /* Normalize yaw delta to the [-180, 180) range to handle the 359 -> 1 degree wrap-around. */
        float deltaYaw = yaw - state.lastYaw;
        if (deltaYaw > 180) deltaYaw -= ROTATION_MODULO;
        if (deltaYaw < -180) deltaYaw += ROTATION_MODULO;

        float deltaPitch = pitch - state.lastPitch;

        double absDeltaYaw = Math.abs(deltaYaw);
        double absDeltaPitch = Math.abs(deltaPitch);

        /* Strategy 1: Instant-snap — a single rotation delta exceeding the threshold. */
        if (absDeltaYaw > INSTANT_SNAP_THRESHOLD || absDeltaPitch > 90.0) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > SNAP_BUFFER_FLAG_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }

        /* Accumulate deltas for the rolling variance-ratio window. */
        state.yawAccumulator += absDeltaYaw;
        state.pitchAccumulator += absDeltaPitch;
        state.rotationCount++;

        /* Strategy 2: Yaw/pitch variance ratio — evaluate once enough samples are collected. */
        if (state.rotationCount >= MIN_ROTATION_SAMPLES) {
            double avgYawDelta = state.yawAccumulator / state.rotationCount;
            double avgPitchDelta = state.pitchAccumulator / state.rotationCount;

            if (avgYawDelta > 0.1 && avgPitchDelta < AIMBOT_YAW_VARIANCE_THRESHOLD) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }

            /* Reset accumulators for the next evaluation window. */
            state.yawAccumulator = 0;
            state.pitchAccumulator = 0;
            state.rotationCount = 0;
        }

        state.lastYaw = yaw;
        state.lastPitch = pitch;
    }

    /** No outbound packets are relevant to this check. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
