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
 * Detects block-breaking actions performed while the player is in air or liquid.
 *
 * <p>Mirrors {@link AirLiquidPlaceCheck} but targets {@link DiggingAction#START_DIGGING} events
 * instead of block placements. Legitimate players must be on solid ground to break blocks;
 * a client breaking blocks while airborne or submerged indicates a hacked client.
 *
 * <p><b>Detection conditions (either triggers):</b>
 * <ol>
 *   <li><b>In liquid</b> — The block at the player's feet is liquid (water/lava).</li>
 *   <li><b>In air while falling</b> — The feet block is non-solid, non-liquid, and the player's
 *       Y-velocity is below -0.5 (falling fast). This avoids false positives on edges.</li>
 * </ol>
 *
 * <p><b>Buffer logic:</b> Each violation adds 1.0. Flag at buffer &gt; {@value #BUFFER_THRESHOLD}.
 * Valid breaks decay the buffer by 0.5.
 *
 * @see AirLiquidPlaceCheck — companion check for placing blocks while in air/liquid
 * @see WrongBreakCheck — companion check for spatial consistency of break actions
 */
@CheckData(name = "Air Liquid Break", stableKey = "windfall.movement.airliquidbreak", decay = 0.02, setbackVl = 10)
public class AirLiquidBreakCheck extends Check implements PacketCheck {

    /** Buffer must exceed this value before a flag is raised. */
    private static final int BUFFER_THRESHOLD = 3;

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_DIGGING} packets with action
     * {@link DiggingAction#START_DIGGING}. Checks the player's feet block for liquid or
     * air-while-falling conditions.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        if (!player.getPlayer().isOnline()) return;

        Location feetLoc = player.getPlayer().getLocation();
        Block feetBlock = feetLoc.getBlock();
        Material feetType = feetBlock.getType();

        /* Player is in liquid (water, lava, etc.) — uses isLiquid() for cross-version compat */
        boolean inLiquid = feetBlock.isLiquid();

        /* Player is in air: feet block is neither solid nor liquid */
        boolean inAir = !feetType.isSolid() && !inLiquid;

        /*
         * Falling threshold: deltaY < -0.5 indicates falling at more than ~10 blocks/s.
         * Brief air contact on edges/ladders has deltaY ≈ 0 and is excluded.
         */
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
