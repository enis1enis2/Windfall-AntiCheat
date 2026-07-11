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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Detects players breaking blocks while standing in air or liquid.
 * Legitimate players must be on solid ground to break blocks.
 * Hacked clients can break blocks from any position.
 */
@CheckData(name = "Air Liquid Break", stableKey = "windfall.movement.airliquidbreak", decay = 0.02, setbackVl = 10)
public class AirLiquidBreakCheck extends Check implements PacketCheck {

    private static final int BUFFER_THRESHOLD = 3;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        if (!player.getPlayer().isOnline()) return;

        Location feetLoc = player.getPlayer().getLocation();
        Block feetBlock = feetLoc.getBlock();
        Material feetType = feetBlock.getType();

        // Player is in liquid (use isLiquid for cross-version compat)
        boolean inLiquid = feetBlock.isLiquid();

        // Player is in air (feet block is not solid and not liquid)
        boolean inAir = !feetType.isSolid() && !inLiquid;

        // Also check if player is falling (Y delta is large and negative)
        boolean falling = player.getDeltaY() < -0.5;

        if (inLiquid || (inAir && falling)) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
