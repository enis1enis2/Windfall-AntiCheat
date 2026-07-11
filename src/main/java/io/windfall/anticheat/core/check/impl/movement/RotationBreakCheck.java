package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects rotation inconsistencies during block breaking.
 * If the player's rotation changes drastically between break start and finish,
 * it indicates aim manipulation or multi-task cheating.
 */
@CheckData(name = "Rotation Break A", stableKey = "windfall.movement.rotationbreak", decay = 0.02, setbackVl = 15)
public class RotationBreakCheck extends Check implements PacketCheck {

    private static final float MAX_ROTATION_DELTA = 45.0f;

    private float breakStartYaw;
    private float breakStartPitch;
    private boolean breaking;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action == DiggingAction.START_DIGGING) {
            breaking = true;
            breakStartYaw = player.getYaw();
            breakStartPitch = player.getPitch();
        } else if (action == DiggingAction.FINISHED_DIGGING) {
            if (!breaking) return;
            breaking = false;

            float deltaYaw = Math.abs(player.getYaw() - breakStartYaw);
            float deltaPitch = Math.abs(player.getPitch() - breakStartPitch);

            if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;

            if (deltaYaw > MAX_ROTATION_DELTA || deltaPitch > MAX_ROTATION_DELTA) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        } else if (action == DiggingAction.CANCELLED_DIGGING) {
            breaking = false;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
