package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Detects block placements performed while the player is in air or liquid.
 *
 * <p>In vanilla Minecraft, players must be standing on solid ground to place blocks normally. A
 * client placing blocks while airborne or submerged indicates a hacked client that bypasses
 * server-side placement validation (e.g. scaffold, fast-place, or phase exploits).
 *
 * <p><b>Detection conditions (either triggers):</b>
 * <ol>
 *   <li><b>In liquid</b> — The block at the player's feet position is liquid (water/lava).
 *       Uses {@link Block#isLiquid()} for cross-version compatibility.</li>
 *   <li><b>In air while falling</b> — The feet block is non-solid, non-liquid (air), AND the
 *       player's Y-velocity ({@link WindfallPlayer#getDeltaY()}) is below -0.5 (falling fast).
 *       This avoids flagging players on ladder edges or half-slabs where brief air contact is
 *       normal.</li>
 * </ol>
 *
 * <p><b>Buffer logic:</b> Each violation adds 1.0. Flag at buffer &gt; {@value #BUFFER_THRESHOLD}.
 * Valid placements decay the buffer by 0.5.
 *
 * @see AirLiquidBreakCheck — companion check for breaking blocks while in air/liquid
 * @see ScaffoldCheck — companion check for block-placing speed
 */
@CheckData(name = "Air Liquid Place", stableKey = "windfall.movement.airliquidplace", decay = 0.02, setbackVl = 10)
public class AirLiquidPlaceCheck extends Check implements PacketCheck {

    /** Buffer must exceed this value before a flag is raised. */
    private static final int BUFFER_THRESHOLD = 3;

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT} packets with a
     * non-null face. Checks the player's feet block for liquid or air-while-falling conditions.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        if (wrapper.getFace() == null) return;

        if (!player.getPlayer().isOnline()) return;

        Location feetLoc = player.getPlayer().getLocation();
        Block feetBlock = feetLoc.getBlock();
        Material feetType = feetBlock.getType();

        /* Player is in liquid (water, lava, etc.) */
        boolean inLiquid = feetBlock.isLiquid();

        /* Player is in air: feet block is neither solid nor liquid */
        boolean inAir = !feetType.isSolid() && !inLiquid;

        /*
         * Falling threshold: deltaY < -0.5 indicates the player is falling at more than
         * 10 blocks/s (vanilla gravity ≈ blocks/tick²). Brief air contact on edges/ladders
         * has deltaY ≈ 0 and is excluded.
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
