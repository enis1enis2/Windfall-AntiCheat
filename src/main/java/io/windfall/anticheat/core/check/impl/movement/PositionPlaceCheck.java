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
 * Detects placing blocks from positions that are too far from the player's eye position.
 *
 * <p>Similar to {@link FarPlaceCheck} but measures from the player's <em>eye position</em>
 * (feet Y + height) instead of body centre, and uses squared-distance comparison for efficiency
 * (avoiding the {@link Math#sqrt} call on the hot path).
 *
 * <p><b>Key thresholds:</b>
 * <ul>
 *   <li>{@value #MAX_REACH_SQ} = 25.0 (squared) — equivalent to 5.0 blocks reach.</li>
 *   <li>{@value #TOLERANCE} = 0.5 — squared tolerance beyond reach before definite flag.</li>
 *   <li>{@value #BUFFER_THRESHOLD} = 3 — buffer level at which definite violations flag.</li>
 * </ul>
 *
 * <p><b>Tiered severity:</b> Same pattern as {@link FarPlaceCheck} — definite violations add 1.0
 * to the buffer, borderline violations add 0.3 with a higher flag threshold (5.0).
 *
 * <p>Placements with a {@code null} face are ignored (air-place packets).
 *
 * @see FarPlaceCheck — companion check using body-centre distance with sqrt
 * @see RotationPlaceCheck — companion check verifying player rotation matches placement target
 */
@CheckData(name = "Position Place", stableKey = "windfall.movement.positionplace", decay = 0.01, setbackVl = 15)
public class PositionPlaceCheck extends Check implements PacketCheck {

    /** Maximum reach distance squared (5.0² = 25.0). Avoids sqrt for performance. */
    private static final double MAX_REACH_SQ = 25.0; // 5.0 blocks squared

    /**
     * Squared tolerance beyond {@value #MAX_REACH_SQ} before definite flagging.
     * Accounts for latency and movement drift.
     */
    private static final double TOLERANCE = 0.5;

    /** Buffer must exceed this value before definite violations are flagged. */
    private static final int BUFFER_THRESHOLD = 3;

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT} packets with a
     * non-null face. Computes squared distance from eye position to block centre and applies
     * tiered severity thresholds.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        if (wrapper.getFace() == null) return;

        int blockX = wrapper.getBlockPosition().getX();
        int blockY = wrapper.getBlockPosition().getY();
        int blockZ = wrapper.getBlockPosition().getZ();

        /* Block centre = block position + 0.5 on each axis */
        double centerX = blockX + 0.5;
        double centerY = blockY + 0.5;
        double centerZ = blockZ + 0.5;

        /* Eye position = feet Y + player height (eye-level view) */
        double eyeY = player.getY() + player.getHeight();

        /* Squared Euclidean distance (avoids sqrt for performance) */
        double dx = player.getX() - centerX;
        double dy = eyeY - centerY;
        double dz = player.getZ() - centerZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > MAX_REACH_SQ + TOLERANCE) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else if (distSq > MAX_REACH_SQ) {
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
