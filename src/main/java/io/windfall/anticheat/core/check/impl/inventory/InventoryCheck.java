package io.windfall.anticheat.core.check.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects inventory manipulation: clicking windows too fast, creative mode
 * item spawning in survival, and abnormal click patterns.
 */
@CheckData(name = "Inventory A", stableKey = "windfall.inventory.inventory", decay = 0.02, setbackVl = 15)
public class InventoryCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_SECOND = 20;
    private static final long CLICK_WINDOW_MS = 50;

    private long lastClickTime;
    private int clickCount;
    private int clicksThisSecond;
    private long secondStart;

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
        long now = System.currentTimeMillis();

        if (now - lastClickTime < CLICK_WINDOW_MS) {
            clickCount++;
            if (clickCount > 5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            clickCount = 0;
        }
        lastClickTime = now;

        if (now - secondStart > 1000) {
            clicksThisSecond = 0;
            secondStart = now;
        }
        clicksThisSecond++;
        if (clicksThisSecond > MAX_CLICKS_PER_SECOND) {
            flag(player);
        }
    }

    private void handleCreativeSlot(WindfallPlayer player, PacketReceiveEvent event) {
        if (!player.getPlayer().getGameMode().equals(org.bukkit.GameMode.CREATIVE)) {
            flag(player);
        }
    }
}
