package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects breaking or placing blocks without the corresponding arm swing animation.
 * Hacked clients often skip the swing animation for speed or stealth.
 */
@CheckData(name = "No Swing A", stableKey = "windfall.movement.noswing", decay = 0.02, setbackVl = 10)
public class NoSwingCheck extends Check implements PacketCheck {

    private static final long SWING_TIMEOUT_MS = 300;
    private static final int BUFFER_THRESHOLD = 3;

    private long lastSwingTime;
    private int missingSwingCount;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.ANIMATION) {
            lastSwingTime = System.currentTimeMillis();
            return;
        }

        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = wrapper.getAction();

            if (action == DiggingAction.START_DIGGING) {
                checkSwing(player);
            }
        } else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            if (wrapper.getFace() != null) {
                checkSwing(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void checkSwing(WindfallPlayer player) {
        long now = System.currentTimeMillis();
        if (now - lastSwingTime > SWING_TIMEOUT_MS) {
            missingSwingCount++;
            if (missingSwingCount >= BUFFER_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            missingSwingCount = Math.max(0, missingSwingCount - 1);
        }
    }
}
