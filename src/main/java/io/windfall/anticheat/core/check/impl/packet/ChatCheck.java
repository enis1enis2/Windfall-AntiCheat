package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;

@CheckData(name = "Chat A", stableKey = "windfall.packet.chat", decay = 0.01, setbackVl = 15)
public class ChatCheck extends Check implements PacketCheck {

    private static final int MAX_CHAT_PER_SECOND = 5;
    private static final int MAX_CHAT_PER_MINUTE = 60;
    private static final long CHAT_BURST_WINDOW_MS = 2000;
    private static final int MAX_CHAT_BURST = 4;

    private final ArrayDeque<Long> chatTimestamps = new ArrayDeque<>();
    private final ArrayDeque<Long> chatBurstWindow = new ArrayDeque<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CHAT_MESSAGE) return;

        long now = System.currentTimeMillis();
        chatTimestamps.addLast(now);
        chatBurstWindow.addLast(now);

        while (!chatTimestamps.isEmpty() && now - chatTimestamps.peekFirst() > 60000) {
            chatTimestamps.removeFirst();
        }
        while (!chatBurstWindow.isEmpty() && now - chatBurstWindow.peekFirst() > CHAT_BURST_WINDOW_MS) {
            chatBurstWindow.removeFirst();
        }

        int chatsPerMinute = chatTimestamps.size();
        int chatsInBurst = chatBurstWindow.size();

        if (chatsPerMinute > MAX_CHAT_PER_MINUTE) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (chatsInBurst > MAX_CHAT_BURST) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
