package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;

@CheckData(name = "Chest Stealer A", stableKey = "windfall.packet.cheststealer", decay = 0.01, setbackVl = 15)
public class ChestStealerCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_WINDOW = 40;
    private static final long WINDOW_TIMEOUT_MS = 3000;
    private static final int FAST_CLICK_THRESHOLD = 6;
    private static final long FAST_CLICK_WINDOW_MS = 500;
    private static final int MAX_ITEMS_PER_SECOND = 15;

    private int clicksThisWindow;
    private long windowOpenTime;
    private boolean windowOpen;

    private final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();

        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            handleClick(player, now);
        } else if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            handleClose(player, now);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            windowOpen = true;
            windowOpenTime = System.currentTimeMillis();
            clicksThisWindow = 0;
        }
    }

    private void handleClick(WindfallPlayer player, long now) {
        if (!windowOpen) return;

        clicksThisWindow++;
        clickTimestamps.addLast(now);
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.peekFirst() > FAST_CLICK_WINDOW_MS) {
            clickTimestamps.removeFirst();
        }

        if (clicksThisWindow > MAX_CLICKS_PER_WINDOW) {
            flag(player);
            return;
        }

        if (clickTimestamps.size() > FAST_CLICK_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.05);
        }
    }

    private void handleClose(WindfallPlayer player, long now) {
        if (!windowOpen) return;

        long windowDuration = now - windowOpenTime;
        if (windowDuration < WINDOW_TIMEOUT_MS && clicksThisWindow > MAX_ITEMS_PER_SECOND) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        windowOpen = false;
        clicksThisWindow = 0;
    }
}
