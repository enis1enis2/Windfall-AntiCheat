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
 * Detects breaking multiple blocks in the same tick.
 * Vanilla clients can only break one block per tick.
 * Hacked clients can break multiple blocks simultaneously.
 */
@CheckData(name = "Multi Break", stableKey = "windfall.movement.multibreak", decay = 0.02, setbackVl = 10)
public class MultiBreakCheck extends Check implements PacketCheck {

    private static final int MAX_BREAKS_PER_TICK = 1;
    private static final int BUFFER_THRESHOLD = 2;

    private int breaksThisTick;
    private long lastTick;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        long currentTick = player.getTickCount();
        if (currentTick != lastTick) {
            breaksThisTick = 0;
            lastTick = currentTick;
        }

        breaksThisTick++;

        if (breaksThisTick > MAX_BREAKS_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
