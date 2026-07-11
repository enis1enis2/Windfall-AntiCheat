package io.windfall.anticheat.core.check.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Inventory A", stableKey = "windfall.inventory.inventory", decay = 0.02, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class InventoryCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_SECOND = 20;
    private static final long CLICK_WINDOW_MS = 50;

    private static final class PlayerState {
        long lastClickTime;
        int clickCount;
        int clicksThisSecond;
        long secondStart;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            handleClickWindow(player, event);
        } else if (type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            handleCreativeSlot(player, event);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleClickWindow(WindfallPlayer player, PacketReceiveEvent event) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        if (now - state.lastClickTime < CLICK_WINDOW_MS) {
            state.clickCount++;
            if (state.clickCount > 5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.clickCount = 0;
        }
        state.lastClickTime = now;

        if (now - state.secondStart > 1000) {
            state.clicksThisSecond = 0;
            state.secondStart = now;
        }
        state.clicksThisSecond++;
        if (state.clicksThisSecond > MAX_CLICKS_PER_SECOND) {
            flag(player);
        }
    }

    private void handleCreativeSlot(WindfallPlayer player, PacketReceiveEvent event) {
        if (!player.getPlayer().getGameMode().equals(org.bukkit.GameMode.CREATIVE)) {
            flag(player);
        }
    }
}
