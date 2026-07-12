package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects kill aura cheats by monitoring target-switching speed and rotation symmetry patterns.
 *
 * <p><b>Multi-Aura detection:</b> Tracks the number of unique entity targets attacked within
 * a {@value SWING_WINDOW_MS}ms sliding window. If the count exceeds the version-appropriate
 * threshold (modern: {@value MAX_TARGETS_PER_SECOND_MODERN}, legacy: {@value MAX_TARGETS_PER_SECOND_LEGACY}),
 * buffer increases and flags when above the threshold.</p>
 *
 * <p><b>Rotation symmetry detection:</b> Bots often snap to targets with near-perfect
 * rotational symmetry (equal clockwise and counter-clockwise deltas). When yaw delta
 * snaps exceed {@value SNAP_TO_TARGET_THRESHOLD} degrees and the symmetry ratio exceeds
 * {@value BOT_ROTATION_SYMMETRY_THRESHOLD}, the buffer increases. Legacy clients receive
 * relaxed thresholds due to less precise rotation data.</p>
 *
 * @see <a href="https://wiki.vg/Protocol">Protocol version numbers</a>
 */
@CheckData(name = "Kill Aura A", stableKey = "windfall.combat.killaura", decay = 0.005, setbackVl = 25,
    compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.2)
public class KillAuraCheck extends Check implements PacketCheck {

    /** Maximum unique targets allowed per window on legacy clients (protocol < 1.0.7). */
    private static final int MAX_TARGETS_PER_SECOND_LEGACY = 6;
    /** Maximum unique targets allowed per window on modern clients. */
    private static final int MAX_TARGETS_PER_SECOND_MODERN = 4;
    /** Sliding window duration in milliseconds for target counting. */
    private static final int SWING_WINDOW_MS = 1000;
    /** Yaw delta (degrees) above which a rotation is considered a "snap" to a new target. */
    private static final double SNAP_TO_TARGET_THRESHOLD = 60.0;
    /** Minimum number of snaps required before the symmetry check activates. */
    private static final int MIN_SNAP_COUNT = 3;
    /**
     * Minimum ratio of |positive|/|negative| yaw deltas to flag rotation symmetry.
     * A value close to 1.0 means the bot rotates equally in both directions.
     */
    private static final double BOT_ROTATION_SYMMETRY_THRESHOLD = 0.95;
    /** Minimum horizontal movement speed (blocks/tick) to consider a legacy client as strafing. */
    private static final double LEGACY_STRAFE_THRESHOLD = 0.15;
    /** Multiplier applied to buffer gains for legacy clients to compensate for less precise data. */
    private static final int LEGACY_BUFFER_MULTIPLIER = 2;

    /** Per-player tracking state for kill aura detection. */
    private static final class PlayerState {
        final ArrayDeque<TargetEvent> recentTargets = new ArrayDeque<>();
        final ArrayDeque<Float> recentYawDeltas = new ArrayDeque<>();
        float lastYaw;
        boolean hasLastYaw;
        int snapCount;
        int totalAttacks;
        boolean legacyClient;
        boolean strafing;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or initializes the tracking state for the given player.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming packets to track attacks and rotation data.
     * Routes attack packets to {@link #handleAttack} and rotation packets to {@link #handleRotation}.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        PlayerState state = getState(player);

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleAttack(player, event, state);
        } else if (type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handleRotation(player, event, state);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Handles an attack packet by recording the target, pruning stale entries from the
     * sliding window, and triggering multi-aura checks every 20 attacks.
     *
     * @param player the attacking player
     * @param event  the interact-entity packet event
     * @param state  the player's tracking state
     */
    private void handleAttack(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int protocol = player.getProtocolVersion();
        state.legacyClient = protocol < 107;

        int targetId = wrapper.getEntityId();
        long now = System.currentTimeMillis();

        state.recentTargets.addLast(new TargetEvent(targetId, now));
        while (!state.recentTargets.isEmpty() && now - state.recentTargets.peekFirst().timestamp > SWING_WINDOW_MS) {
            state.recentTargets.removeFirst();
        }

        state.totalAttacks++;

        if (state.totalAttacks > 0 && state.totalAttacks % 20 == 0) {
            checkMultiAura(player, state);
        }

        checkRotationSymmetry(player, state);
    }

    /**
     * Tracks yaw changes between rotation packets. Computes delta yaw (wrapped to [-180, 180]),
     * maintains a sliding window of recent deltas, and counts snap-to-target movements.
     *
     * @param player the player whose rotation is being tracked
     * @param event  the rotation packet event
     * @param state  the player's tracking state
     */
    private void handleRotation(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation wrapper =
                new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();

        if (state.legacyClient) {
            double lateralSpeed = player.getHorizontalSpeed();
            state.strafing = lateralSpeed > LEGACY_STRAFE_THRESHOLD;
        }

        if (!state.hasLastYaw) {
            state.lastYaw = yaw;
            state.hasLastYaw = true;
            return;
        }

        float deltaYaw = yaw - state.lastYaw;
        if (deltaYaw > 180) deltaYaw -= 360;
        if (deltaYaw < -180) deltaYaw += 360;

        state.recentYawDeltas.addLast(deltaYaw);
        if (state.recentYawDeltas.size() > 20) {
            state.recentYawDeltas.removeFirst();
        }

        if (Math.abs(deltaYaw) > SNAP_TO_TARGET_THRESHOLD) {
            state.snapCount++;
        }

        state.lastYaw = yaw;
    }

    /**
     * Checks for multi-aura by counting distinct targets within the sliding window.
     * If the unique target count exceeds the version-appropriate limit, buffer increases.
     * Legacy strafing reduces the buffer gain to avoid false positives.
     *
     * @param player the player being checked
     * @param state  the player's tracking state
     */
    private void checkMultiAura(WindfallPlayer player, PlayerState state) {
        int uniqueTargets = (int) state.recentTargets.stream()
                .map(te -> te.targetId)
                .distinct()
                .count();

        int protocol = player.getProtocolVersion();
        int maxTargets = protocol < 107 ? MAX_TARGETS_PER_SECOND_LEGACY : MAX_TARGETS_PER_SECOND_MODERN;

        if (player.isBedrock()) {
            maxTargets += 2;
        }

        if (uniqueTargets > maxTargets) {
            double bufferIncrease = state.legacyClient ? 1.0 : 2.0;
            if (state.legacyClient && state.strafing) {
                bufferIncrease *= 0.5;
            }
            increaseBuffer(player, bufferIncrease);
            double flagThreshold = state.legacyClient ? 7.0 : 5.0;
            if (getBuffer(player) > flagThreshold) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    /**
     * Detects inhuman rotation patterns by measuring the ratio of positive to negative yaw deltas.
     * A ratio close to 1.0 indicates the bot is snapping equally in both directions — a hallmark
     * of automated targeting. Bedrock players get a slightly higher threshold (0.98) due to
     * controller input characteristics.
     *
     * @param player the player being checked
     * @param state  the player's tracking state
     */
    private void checkRotationSymmetry(WindfallPlayer player, PlayerState state) {
        if (state.snapCount < MIN_SNAP_COUNT) return;
        if (state.recentYawDeltas.size() < 10) return;

        long positiveCount = state.recentYawDeltas.stream().filter(d -> d > 0.5).count();
        long negativeCount = state.recentYawDeltas.stream().filter(d -> d < -0.5).count();
        long total = positiveCount + negativeCount;

        if (total < 10) return;

        double symmetryRatio = Math.min(positiveCount, negativeCount) / (double) total;

        double threshold = BOT_ROTATION_SYMMETRY_THRESHOLD;
        if (player.isBedrock()) {
            threshold = 0.98;
        }

        if (symmetryRatio > threshold) {
            double bufferIncrease = state.legacyClient ? 0.8 : 1.5;
            if (state.legacyClient && state.strafing) {
                bufferIncrease *= 0.5;
            }
            increaseBuffer(player, bufferIncrease);
            double flagThreshold = state.legacyClient ? 6.0 : 4.0;
            if (getBuffer(player) > flagThreshold) {
                flag(player);
                resetBuffer(player);
                state.snapCount = 0;
            }
        } else {
            decreaseBuffer(player, 0.2);
        }

        if (state.snapCount > MIN_SNAP_COUNT * 3) {
            state.snapCount = 0;
        }
    }

    /** Lightweight record of an attack event: entity ID and timestamp. */
    private static final class TargetEvent {
        final int targetId;
        final long timestamp;

        TargetEvent(int targetId, long timestamp) {
            this.targetId = targetId;
            this.timestamp = timestamp;
        }
    }
}
