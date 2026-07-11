package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Sprint A", stableKey = "windfall.packet.sprint", decay = 0.01, setbackVl = 15)
public class SprintCheck extends Check implements PacketCheck {

    private static final int MAX_SPRINT_TOGGLE_PER_SECOND = 4;
    private static final long TOGGLE_WINDOW_MS = 1000;
    private static final int MIN_TOGGLE_FLAGS = 3;

    private boolean lastSprinting;
    private long lastToggleTime;
    private int toggleCount;
    private int consecutiveFlags;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        boolean sprinting = player.isSprinting();
        long now = System.currentTimeMillis();

        if (now - lastToggleTime > TOGGLE_WINDOW_MS) {
            if (toggleCount > MAX_SPRINT_TOGGLE_PER_SECOND) {
                consecutiveFlags++;
                if (consecutiveFlags >= MIN_TOGGLE_FLAGS) {
                    flag(player);
                    consecutiveFlags = 0;
                }
            } else {
                consecutiveFlags = Math.max(0, consecutiveFlags - 1);
            }
            toggleCount = 0;
            lastToggleTime = now;
        }

        if (sprinting != lastSprinting) {
            toggleCount++;
        }
        lastSprinting = sprinting;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
