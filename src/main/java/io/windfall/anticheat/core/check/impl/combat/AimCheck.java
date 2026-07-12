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

@CheckData(name = "Aim A", stableKey = "windfall.combat.aim", decay = 0.01, setbackVl = 10)
public class AimCheck extends Check implements PacketCheck {

    private static final double INSTANT_SNAP_THRESHOLD = 180.0;
    private static final float ROTATION_MODULO = 360.0f;
    private static final int MIN_ROTATION_SAMPLES = 5;
    private static final double AIMBOT_YAW_VARIANCE_THRESHOLD = 0.5;
    private static final double SNAP_BUFFER_FLAG_THRESHOLD = 3.0;

    private static final class PlayerState {
        float lastYaw;
        float lastPitch;
        boolean hasRotation;
        double yawAccumulator;
        double pitchAccumulator;
        int rotationCount;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

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

        float deltaYaw = yaw - state.lastYaw;
        if (deltaYaw > 180) deltaYaw -= ROTATION_MODULO;
        if (deltaYaw < -180) deltaYaw += ROTATION_MODULO;

        float deltaPitch = pitch - state.lastPitch;

        double absDeltaYaw = Math.abs(deltaYaw);
        double absDeltaPitch = Math.abs(deltaPitch);

        if (absDeltaYaw > INSTANT_SNAP_THRESHOLD || absDeltaPitch > 90.0) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > SNAP_BUFFER_FLAG_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.yawAccumulator += absDeltaYaw;
        state.pitchAccumulator += absDeltaPitch;
        state.rotationCount++;

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

            state.yawAccumulator = 0;
            state.pitchAccumulator = 0;
            state.rotationCount = 0;
        }

        state.lastYaw = yaw;
        state.lastPitch = pitch;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
