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
 * Detects breaking blocks from positions that are too far from the player's
 * current position, using squared-distance comparison for performance.
 * Complements {@link FarBreakCheck} by measuring from the player's actual
 * eye-level position (Y + height) rather than mid-body height, providing
 * better accuracy for tall or crouching players.
 *
 * <p><b>Algorithm:</b> On START_DIGGING and FINISHED_DIGGING packets, the
 * squared Euclidean distance from the player's eye position to the block
 * center is computed (avoiding the {@code sqrt} cost). Two tiers:</p>
 * <ul>
 *   <li><b>Severely out of range (distSq &gt; 25.5):</b> Buffer +1.0, flag at &gt; 3.0.</li>
 *   <li><b>Moderately out of range (25.0 &lt; distSq &le; 25.5):</b> Buffer +0.3, flag at &gt; 5.0.</li>
 * </ul>
 *
 * @see FarBreakCheck
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Position Break", stableKey = "windfall.movement.positionbreak", decay = 0.01, setbackVl = 15)
public class PositionBreakCheck extends Check implements PacketCheck {

    /** Maximum reach distance squared (5.0^2 = 25.0) to avoid sqrt in the hot path. */
    private static final double MAX_REACH_SQ = 25.0; // 5.0 blocks squared

    /** Squared-distance tolerance beyond MAX_REACH_SQ before the severe tier activates. */
    private static final double TOLERANCE = 0.5;

    /** Buffer level at which the player is flagged in the severe tier. */
    private static final int BUFFER_THRESHOLD = 3;

    /**
     * Validates block-break distance on dig start/finish packets using a
     * squared-distance comparison for efficiency.
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

        /** Block center coordinates. */
        double centerX = blockX + 0.5;
        double centerY = blockY + 0.5;
        double centerZ = blockZ + 0.5;

        /** Player eye-level Y coordinate (feet + full height). */
        double eyeY = player.getY() + player.getHeight();

        /** Component deltas from eye position to block center. */
        double dx = player.getX() - centerX;
        double dy = eyeY - centerY;
        double dz = player.getZ() - centerZ;
        /** Squared distance — avoids Math.sqrt for performance. */
        double distSq = dx * dx + dy * dy + dz * dz;

        /** Tier 1: Severely out of range (above MAX_REACH_SQ + TOLERANCE). */
        if (distSq > MAX_REACH_SQ + TOLERANCE) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else if (distSq > MAX_REACH_SQ) {
            /** Tier 2: Moderately out of range — needs more consecutive hits to flag. */
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
