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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Chest Stealer A", stableKey = "windfall.packet.cheststealer", decay = 0.01, setbackVl = 15)
public class ChestStealerCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_WINDOW = 40;
    private static final long WINDOW_TIMEOUT_MS = 3000;
    private static final int FAST_CLICK_THRESHOLD = 6;
    private static final long FAST_CLICK_WINDOW_MS = 500;
    private static final int MAX_ITEMS_PER_SECOND = 15;

    private static final class PlayerState {
        int clicksThisWindow;
        long windowOpenTime;
        boolean windowOpen;
        final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            handleClick(player, now, state);
        } else if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            handleClose(player, now, state);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            PlayerState state = getState(player);
            state.windowOpen = true;
            state.windowOpenTime = System.currentTimeMillis();
            state.clicksThisWindow = 0;
        }
    }

    private void handleClick(WindfallPlayer player, long now, PlayerState state) {
        if (!state.windowOpen) return;

        state.clicksThisWindow++;
        state.clickTimestamps.addLast(now);
        while (!state.clickTimestamps.isEmpty() && now - state.clickTimestamps.peekFirst() > FAST_CLICK_WINDOW_MS) {
            state.clickTimestamps.removeFirst();
        }

        if (state.clicksThisWindow > MAX_CLICKS_PER_WINDOW) {
            flag(player);
            return;
        }

        if (state.clickTimestamps.size() > FAST_CLICK_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.05);
        }
    }

    private void handleClose(WindfallPlayer player, long now, PlayerState state) {
        if (!state.windowOpen) return;

        long windowDuration = now - state.windowOpenTime;
        if (windowDuration < WINDOW_TIMEOUT_MS && state.clicksThisWindow > MAX_ITEMS_PER_SECOND) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.windowOpen = false;
        state.clicksThisWindow = 0;
    }
}
