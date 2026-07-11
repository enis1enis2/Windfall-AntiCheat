package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects placing multiple blocks in the same tick.
 * Vanilla clients can only place one block per tick.
 * Hacked clients can place multiple blocks simultaneously.
 */
@CheckData(name = "Multi Place", stableKey = "windfall.movement.multiplace", decay = 0.02, setbackVl = 10)
public class MultiPlaceCheck extends Check implements PacketCheck {

    private static final int MAX_PLACES_PER_TICK = 1;
    private static final int BUFFER_THRESHOLD = 2;

    private int placesThisTick;
    private long lastTick;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        if (wrapper.getFace() == null) return;

        long currentTick = player.getTickCount();
        if (currentTick != lastTick) {
            placesThisTick = 0;
            lastTick = currentTick;
        }

        placesThisTick++;

        if (placesThisTick > MAX_PLACES_PER_TICK) {
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
