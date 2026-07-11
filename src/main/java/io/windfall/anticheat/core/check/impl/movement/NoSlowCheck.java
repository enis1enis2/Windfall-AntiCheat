package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "NoSlow A", stableKey = "windfall.movement.noslow", decay = 0.01, setbackVl = 15)
public class NoSlowCheck extends Check implements PacketCheck {

    private static final double BASE_WALK_SPEED = 0.102;
    private static final double SPRINT_MULTIPLIER = 1.3;
    private static final double SNEAK_MULTIPLIER = 0.3;
    private static final double USING_ITEM_SLOWDOWN = 0.2;
    private static final double MIN_SPEED_FOR_CHECK = 0.05;
    private static final int MIN_USING_ITEM_TICKS = 3;

    private boolean usingItem;
    private int usingItemTicks;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed < MIN_SPEED_FOR_CHECK) {
            decreaseBuffer(player, 0.1);
            return;
        }

        // Detect item use via player state — not directly available from packets, use speed heuristic
        double maxExpectedSpeed = BASE_WALK_SPEED * SPRINT_MULTIPLIER;

        // If player is moving faster than expected while using an item, it's noslow
        if (usingItem && horizontalSpeed > maxExpectedSpeed * 0.9) {
            increaseBuffer(player, 0.8);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
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
