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

/**
 * Detects players breaking blocks from distances exceeding vanilla reach limits.
 * Vanilla maximum reach is ~4.5 blocks; this check uses {@link #MAX_REACH} (5.0)
 * with an additional {@link #TOLERANCE} (0.3) for two severity tiers.
 *
 * <p><b>Algorithm:</b> On START_DIGGING and FINISHED_DIGGING packets, the Euclidean
 * distance from the player's eye-level position (X, Y+height/2, Z) to the block
 * center (X+0.5, Y+0.5, Z+0.5) is calculated. The check uses two tiers:</p>
 * <ul>
 *   <li><b>Far (distance &gt; 5.3):</b> Buffer increases by 1.0; flags at buffer &gt; 3.0.</li>
 *   <li><b>Moderate (5.0 &lt; distance &le; 5.3):</b> Buffer increases by 0.3;
 *       flags at buffer &gt; 5.0 (higher threshold for borderline cases).</li>
 * </ul>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Far Break A", stableKey = "windfall.movement.farbreak", decay = 0.01, setbackVl = 15)
public class FarBreakCheck extends Check implements PacketCheck {

    /** Maximum reach distance in blocks — slightly above vanilla's ~4.5 to allow latency. */
    private static final double MAX_REACH = 5.0;

    /** Additional tolerance beyond MAX_REACH before the high-severity tier activates. */
    private static final double TOLERANCE = 0.3;

    /**
     * Evaluates the distance to the targeted block on dig start/finish packets.
     * Uses a two-tier severity model based on how far beyond vanilla reach the
     * player reaches.
     *
     * @param player the player associated with this packet
     * @param event  the incoming digging packet
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();

        if (action != DiggingAction.START_DIGGING && action != DiggingAction.FINISHED_DIGGING) {
            return;
        }

        int blockX = wrapper.getBlockPosition().getX();
        int blockY = wrapper.getBlockPosition().getY();
        int blockZ = wrapper.getBlockPosition().getZ();

        /** Block center coordinates — targeting is evaluated to the center of each block face. */
        double centerX = blockX + 0.5;
        double centerY = blockY + 0.5;
        double centerZ = blockZ + 0.5;

        /**
         * Euclidean distance from the player's approximate eye position
         * (mid-body height) to the block center.
         */
        double distance = Math.sqrt(
            Math.pow(player.getX() - centerX, 2) +
            Math.pow((player.getY() + player.getHeight() / 2.0) - centerY, 2) +
            Math.pow(player.getZ() - centerZ, 2)
        );

        /** Tier 1: Severely out of range — high buffer increase, lower flag threshold. */
        if (distance > MAX_REACH + TOLERANCE) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (distance > MAX_REACH) {
            /** Tier 2: Moderately out of range — gradual buffer increase, higher flag threshold. */
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
