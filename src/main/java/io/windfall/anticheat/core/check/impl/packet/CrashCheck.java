package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Crash A", stableKey = "windfall.packet.crash", decay = 0.0, setbackVl = 5)
public class CrashCheck extends Check implements PacketCheck {

    private static final int MAX_STRING_LENGTH = 32767;
    private static final int MAX_PACKET_SIZE = 32767;
    private static final int KICK_THRESHOLD = 3;

    private int violations;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        // Oversized payload packets can crash the server
        if (type == PacketType.Play.Client.CHAT_MESSAGE) {
            String message = getChatMessage(event);
            if (message != null && message.length() > MAX_STRING_LENGTH) {
                violations++;
                if (violations >= KICK_THRESHOLD) {
                    flag(player);
                    player.getPlayer().kickPlayer("[Windfall] Oversized chat packet");
                    violations = 0;
                }
            }
        }

        // Creative inventory with oversized NBT
        if (type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 10.0) {
                flag(player);
                player.getPlayer().kickPlayer("[Windfall] Suspicious creative packet");
                resetBuffer(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private String getChatMessage(PacketReceiveEvent event) {
        try {
            com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage wrapper =
                    new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage(event);
            return wrapper.getMessage();
        } catch (Exception e) {
            return null;
        }
    }
}
