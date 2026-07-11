package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Creative A", stableKey = "windfall.packet.creative", decay = 0.0, setbackVl = 5)
public class CreativeCheck extends Check implements PacketCheck {

    private static final int MAX_CREATIVE_ACTIONS_PER_TICK = 5;
    private static final int KICK_THRESHOLD = 10;

    private int actionsThisTick;
    private long lastTickStart;
    private int totalActions;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) return;

        if (player.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            flag(player);
            player.getPlayer().kickPlayer("[Windfall] Creative packet in non-creative mode");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTickStart > 50) {
            actionsThisTick = 0;
            lastTickStart = now;
        }

        actionsThisTick++;
        totalActions++;

        if (actionsThisTick > MAX_CREATIVE_ACTIONS_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > KICK_THRESHOLD) {
                flagWithSetback(player);
                resetBuffer(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
