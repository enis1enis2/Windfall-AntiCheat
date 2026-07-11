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

@CheckData(name = "Aim A", stableKey = "windfall.combat.aim", decay = 0.01, setbackVl = 10)
public class AimCheck extends Check implements PacketCheck {

    private float lastYaw;
    private float lastPitch;
    private boolean hasRotation;

    private double yawAccumulator;
    private double pitchAccumulator;
    private int rotationCount;

    // 180 degrees in one tick is physically impossible for any human input
    private static final double INSTANT_SNAP_THRESHOLD = 180.0;
    private static final double ROTATION_MODULO = 360.0;
    private static final int MIN_ROTATION_SAMPLES = 5;
    private static final double AIMBOT_YAW_VARIANCE_THRESHOLD = 0.5;
    private static final double SNAP_BUFFER_FLAG_THRESHOLD = 3.0;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.PLAYER_ROTATION
                && type != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return;
        }

        WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
        float yaw = wrapper.getYaw();
        float pitch = wrapper.getPitch();

        if (!hasRotation) {
            lastYaw = yaw;
            lastPitch = pitch;
            hasRotation = true;
            return;
        }

        float deltaYaw = yaw - lastYaw;
        // Normalize to [-180, 180] to handle 359→1 wraparound correctly
        if (deltaYaw > 180) deltaYaw -= ROTATION_MODULO;
        if (deltaYaw < -180) deltaYaw += ROTATION_MODULO;

        float deltaPitch = pitch - lastPitch;

        double absDeltaYaw = Math.abs(deltaYaw);
        double absDeltaPitch = Math.abs(deltaPitch);

        if (absDeltaYaw > INSTANT_SNAP_THRESHOLD || absDeltaPitch > 90.0) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > SNAP_BUFFER_FLAG_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }

        yawAccumulator += absDeltaYaw;
        pitchAccumulator += absDeltaPitch;
        rotationCount++;

        if (rotationCount >= MIN_ROTATION_SAMPLES) {
            double avgYawDelta = yawAccumulator / rotationCount;
            double avgPitchDelta = pitchAccumulator / rotationCount;

            // Low pitch + high yaw variance = horizontal aim assistance signature
            if (avgYawDelta > 0.1 && avgPitchDelta < AIMBOT_YAW_VARIANCE_THRESHOLD) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }

            yawAccumulator = 0;
            pitchAccumulator = 0;
            rotationCount = 0;
        }

        lastYaw = yaw;
        lastPitch = pitch;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
