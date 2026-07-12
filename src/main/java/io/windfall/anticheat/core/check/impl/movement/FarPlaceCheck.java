package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects players placing blocks from distances exceeding vanilla reach.
 *
 * <p>Measures the 3D Euclidean distance between the player's body centre (midpoint of the bounding
 * box) and the centre of the target block. Vanilla survival reach is 4.5 blocks; this check uses
 * a maximum reach of {@value #MAX_REACH} blocks with a tolerance of {@value #TOLERANCE} blocks.
 *
 * <p><b>Tiered severity:</b>
 * <ul>
 *   <li><b>Distance &gt; {@value #MAX_REACH} + {@value #TOLERANCE}</b> — definite violation,
 *       buffer += 1.0, flag at buffer &gt; 3.0.</li>
 *   <li><b>Distance &gt; {@value #MAX_REACH}</b> — borderline violation (could be lag/latency),
 *       buffer += 0.3, flag at buffer &gt; 5.0 (higher threshold).</li>
 *   <li><b>Distance within reach</b> — buffer decays by 0.1 per valid placement.</li>
 * </ul>
 *
 * <p>The distance is computed from the player's body centre (Y midpoint) rather than eye level
 * because vanilla checks both eye-level and feet-level reach for placement.
 *
 * @see PositionPlaceCheck — similar check using eye-level position and squared-distance optimisation
 * @see RotationPlaceCheck — companion check verifying player rotation matches placement target
 */
@CheckData(name = "Far Place A", stableKey = "windfall.movement.farplace", decay = 0.01, setbackVl = 15)
public class FarPlaceCheck extends Check implements PacketCheck {

    /** Maximum vanilla reach distance in blocks for block placement. */
    private static final double MAX_REACH = 5.0;

    /**
     * Extra tolerance in blocks beyond {@value #MAX_REACH} before definite flagging.
     * Accounts for latency, movement, and server-side position drift.
     */
    private static final double TOLERANCE = 0.3;

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT} packets.
     * Computes 3D distance from player body centre to block centre and applies tiered
     * severity thresholds.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        var position = wrapper.getBlockPosition();

        int blockX = position.getX();
        int blockY = position.getY();
        int blockZ = position.getZ();

        /* Block centre = block position + 0.5 on each axis */
        double centerX = blockX + 0.5;
        double centerY = blockY + 0.5;
        double centerZ = blockZ + 0.5;

        /* Player body centre = midpoint of bounding box on Y axis */
        double distance = Math.sqrt(
            Math.pow(player.getX() - centerX, 2) +
            Math.pow((player.getY() + player.getHeight() / 2.0) - centerY, 2) +
            Math.pow(player.getZ() - centerZ, 2)
        );

        if (distance > MAX_REACH + TOLERANCE) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (distance > MAX_REACH) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
