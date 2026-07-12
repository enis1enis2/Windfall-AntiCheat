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

@CheckData(name = "Kill Aura A", stableKey = "windfall.combat.killaura", decay = 0.005, setbackVl = 25,
    compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.2)
public class KillAuraCheck extends Check implements PacketCheck {

    // Pre-1.9: unlimited CPS, more targets is normal
    private static final int MAX_TARGETS_PER_SECOND_LEGACY = 6;
    // 1.9+: cooldown limits valid CPS
    private static final int MAX_TARGETS_PER_SECOND_MODERN = 4;
    private static final int SWING_WINDOW_MS = 1000;
    private static final double SNAP_TO_TARGET_THRESHOLD = 60.0;
    private static final int MIN_SNAP_COUNT = 3;
    private static final double BOT_ROTATION_SYMMETRY_THRESHOLD = 0.95;

    private final ArrayDeque<TargetEvent> recentTargets = new ArrayDeque<>();
    private final ArrayDeque<Float> recentYawDeltas = new ArrayDeque<>();

    private static final double LEGACY_STRAFE_THRESHOLD = 0.15;
    private static final int LEGACY_BUFFER_MULTIPLIER = 2;

    private float lastYaw;
    private boolean hasLastYaw;
    private int snapCount;
    private int totalAttacks;
    private boolean legacyClient;
    private boolean strafing;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleAttack(player, event);
        } else if (type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            handleRotation(player, event);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleAttack(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int protocol = player.getProtocolVersion();
        legacyClient = protocol < 107;

        int targetId = wrapper.getEntityId();
        long now = System.currentTimeMillis();

        recentTargets.addLast(new TargetEvent(targetId, now));
        while (!recentTargets.isEmpty() && now - recentTargets.peekFirst().timestamp > SWING_WINDOW_MS) {
            recentTargets.removeFirst();
        }

        totalAttacks++;

        if (totalAttacks > 0 && totalAttacks % 20 == 0) {
            checkMultiAura(player);
        }

        checkRotationSymmetry(player);
    }

    private void handleRotation(WindfallPlayer player, PacketReceiveEvent event) {
        com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation wrapper =
                new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();

        if (legacyClient) {
            double lateralSpeed = player.getHorizontalSpeed();
            strafing = lateralSpeed > LEGACY_STRAFE_THRESHOLD;
        }

        if (!hasLastYaw) {
            lastYaw = yaw;
            hasLastYaw = true;
            return;
        }

        float deltaYaw = yaw - lastYaw;
        if (deltaYaw > 180) deltaYaw -= 360;
        if (deltaYaw < -180) deltaYaw += 360;

        recentYawDeltas.addLast(deltaYaw);
        if (recentYawDeltas.size() > 20) {
            recentYawDeltas.removeFirst();
        }

        if (Math.abs(deltaYaw) > SNAP_TO_TARGET_THRESHOLD) {
            snapCount++;
        }

        lastYaw = yaw;
    }

    private void checkMultiAura(WindfallPlayer player) {
        int uniqueTargets = (int) recentTargets.stream()
                .map(te -> te.targetId)
                .distinct()
                .count();

        // Version-aware: 1.8 unlimited CPS = more targets normal
        int protocol = player.getProtocolVersion();
        int maxTargets = protocol < 107 ? MAX_TARGETS_PER_SECOND_LEGACY : MAX_TARGETS_PER_SECOND_MODERN;

        // Bedrock: wider tolerance for touch/controller input
        if (player.isBedrock()) {
            maxTargets += 2;
        }

        if (uniqueTargets > maxTargets) {
            double bufferIncrease = legacyClient ? 1.0 : 2.0;
            if (legacyClient && strafing) {
                bufferIncrease *= 0.5;
            }
            increaseBuffer(player, bufferIncrease);
            double flagThreshold = legacyClient ? 7.0 : 5.0;
            if (getBuffer(player) > flagThreshold) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    private void checkRotationSymmetry(WindfallPlayer player) {
        if (snapCount < MIN_SNAP_COUNT) return;
        if (recentYawDeltas.size() < 10) return;

        long positiveCount = recentYawDeltas.stream().filter(d -> d > 0.5).count();
        long negativeCount = recentYawDeltas.stream().filter(d -> d < -0.5).count();
        long total = positiveCount + negativeCount;

        if (total < 10) return;

        double symmetryRatio = Math.min(positiveCount, negativeCount) / (double) total;

        // Bedrock: rotation wrapping differences reduce symmetry detection accuracy
        double threshold = BOT_ROTATION_SYMMETRY_THRESHOLD;
        if (player.isBedrock()) {
            threshold = 0.98;
        }

        if (symmetryRatio > threshold) {
            double bufferIncrease = legacyClient ? 0.8 : 1.5;
            if (legacyClient && strafing) {
                bufferIncrease *= 0.5;
            }
            increaseBuffer(player, bufferIncrease);
            double flagThreshold = legacyClient ? 6.0 : 4.0;
            if (getBuffer(player) > flagThreshold) {
                flag(player);
                resetBuffer(player);
                snapCount = 0;
            }
        } else {
            decreaseBuffer(player, 0.2);
        }

        if (snapCount > MIN_SNAP_COUNT * 3) {
            snapCount = 0;
        }
    }

    private static final class TargetEvent {
        final int targetId;
        final long timestamp;

        TargetEvent(int targetId, long timestamp) {
            this.targetId = targetId;
            this.timestamp = timestamp;
        }
    }
}
