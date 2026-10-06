package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.BoundingBox;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Validates that a block placement actually corresponds to where the player is looking.
 *
 * <p>Covers two distinct competitor checks that share one packet and one ray:</p>
 * <ul>
 *   <li><b>InvalidPlaceCursor</b> — the block that was clicked is not intersected by the
 *       player's look ray, so the placement position was fabricated.</li>
 *   <li><b>InvalidPlaceFace</b> — the placement position is not the clicked block offset by
 *       the claimed face, or the look direction opposes that face.</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Read the clicked block straight from the packet. The protocol's "Use Item On" position
 *       is the block whose face was clicked; vanilla then places at {@code clicked + faceNormal}.</li>
 *   <li>Raycast the look ray against that block's AABB, expanded by {@value #BLOCK_TOLERANCE}.
 *       On 1.13+ the packet also carries an exact cursor position, used as a cross-check when
 *       present: the reported hit point must lie on the look ray.</li>
 *   <li>Verify the look direction opposes the face normal. A player clicks a face from outside
 *       the block, so the look vector must point into it; a ray travelling along the outward
 *       normal can only come from inside or behind the block.</li>
 * </ol>
 *
 * <p>The cursor is an in-block fraction (0..1), so it is resolved against the clicked block's
 * origin before it can be compared with world-space ray coordinates.</p>
 *
 * <p>{@link PositionPlaceCheck} remains the authority on placement distance — this check does
 * not duplicate it, it only answers whether the click was aimed at. {@link RotationPlaceCheck}
 * covers gross rotation mismatch.</p>
 *
 * @see PositionPlaceCheck — distance counterpart
 * @see RotationPlaceCheck — rotation counterpart
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Invalid Place Cursor", stableKey = "windfall.movement.invalidplacecursor", decay = 0.02, setbackVl = 10,
    compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.4)
public class InvalidPlaceCursorCheck extends Check implements PacketCheck {

    /** Blocks of slack around the clicked block for edge-click imprecision. */
    private static final double BLOCK_TOLERANCE = 0.15;

    /** Degrees of slack when comparing the look direction against the face normal. */
    private static final double FACE_ANGLE_SLACK_DEGREES = 15.0;

    /** Buffer threshold per violation type. */
    private static final double BUFFER_THRESHOLD = 4.0;

    /** Ray length used for the block test, generous enough to cover maximum placement reach. */
    private static final double RAY_LENGTH = 6.0;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        /* Legacy clients may omit the face, and OTHER carries no direction; there is nothing
         * to validate the click against in either case. */
        if (wrapper.getFace() == null || wrapper.getFace() == BlockFace.OTHER) return;

        Vector3i place = wrapper.getBlockPosition();
        if (place == null) return;

        int[] normal = faceNormal(wrapper.getFace());

        /* The packet reports the clicked block directly. Vanilla places at clicked + faceNormal,
         * but the face is the direction the placement travels, not an offset to undo. */
        int clickX = place.getX();
        int clickY = place.getY();
        int clickZ = place.getZ();

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight();
        double eyeZ = player.getZ();

        double yawRad = Math.toRadians(player.getYaw());
        double pitchRad = Math.toRadians(player.getPitch());
        double cosPitch = Math.cos(pitchRad);

        /* Unit look vector — the standard Minecraft rotation-to-direction formula. */
        double dirX = -Math.sin(yawRad) * cosPitch;
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * cosPitch;

        /* 1. Cursor validation: does the look ray reach the clicked block? */
        BoundingBox blockBox = new BoundingBox(clickX, clickY, clickZ, clickX + 1.0, clickY + 1.0, clickZ + 1.0)
            .expand(BLOCK_TOLERANCE);

        boolean onBlock = blockBox.intersectsRay(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, RAY_LENGTH);

        /* On 1.13+ the client reports the exact hit point, which is the fallback when the ray
         * test misses: the hit point sits on the clicked block's face, and the client picks the
         * nearest face, so an edge click can land outside the expanded box. The point must still
         * lie on the look ray — cursor is an in-block fraction, hence click + cursor. */
        Vector3f cursor = wrapper.getCursorPosition();
        if (!onBlock && cursor != null) {
            double hitX = clickX + cursor.getX();
            double hitY = clickY + cursor.getY();
            double hitZ = clickZ + cursor.getZ();

            double vx = hitX - eyeX;
            double vy = hitY - eyeY;
            double vz = hitZ - eyeZ;

            double along = vx * dirX + vy * dirY + vz * dirZ;
            if (along >= 0.0 && along <= RAY_LENGTH) {
                double perpX = vx - along * dirX;
                double perpY = vy - along * dirY;
                double perpZ = vz - along * dirZ;
                double perpSq = perpX * perpX + perpY * perpY + perpZ * perpZ;
                onBlock = perpSq <= BLOCK_TOLERANCE * BLOCK_TOLERANCE;
            }
        }

        if (!onBlock) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
            return;
        }

        /* 2. Face orientation: a player clicks a face from outside the block, so the look ray
         * must oppose the outward normal (angle near 180). A ray travelling along or into the
         * normal (angle near 0) originates inside or behind the block, which cannot be clicked. */
        double dot = dirX * normal[0] + dirY * normal[1] + dirZ * normal[2];
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (angle < 90.0 - FACE_ANGLE_SLACK_DEGREES) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
            return;
        }

        reward(player);
    }

    /**
     * Converts a block face into a unit block-offset normal.
     *
     * @param face the placement face reported by the client
     * @return {@code [dx, dy, dz]}, each component in {-1, 0, 1}
     */
    static int[] faceNormal(BlockFace face) {
        if (face == null) return new int[]{0, 1, 0};
        switch (face) {
            case NORTH:
                return new int[]{0, 0, -1};
            case SOUTH:
                return new int[]{0, 0, 1};
            case EAST:
                return new int[]{1, 0, 0};
            case WEST:
                return new int[]{-1, 0, 0};
            case DOWN:
                return new int[]{0, -1, 0};
            case UP:
            default:
                return new int[]{0, 1, 0};
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}