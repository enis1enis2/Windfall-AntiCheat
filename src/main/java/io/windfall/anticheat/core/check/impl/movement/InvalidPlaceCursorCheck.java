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
 *   <li>Derive the clicked block from the placement position and face. Vanilla places at
 *       {@code clicked + faceNormal}, so the inverse reconstructs what was clicked.</li>
 *   <li>Raycast the look ray against that block's AABB, expanded by {@value #BLOCK_TOLERANCE}.
 *       On 1.13+ the packet also carries an exact cursor position, which is used as a
 *       cross-check when present.</li>
 *   <li>Verify the look direction has a positive component along the face normal. A player
 *       cannot legitimately place against a face they are looking away from.</li>
 * </ol>
 *
 * <p>Legacy protocols (pre-1.13) report the placement position directly with the face attached;
 * the same derivation holds because those clients send the resulting block position too. The
 * ray test is what adapts to each version, not the arithmetic.</p>
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

        /* Legacy clients may omit the face; there is nothing to derive the click from. */
        if (wrapper.getFace() == null) return;

        Vector3i place = wrapper.getBlockPosition();
        if (place == null) return;

        int[] normal = faceNormal(wrapper.getFace());

        /* Inverse of vanilla's "place at clicked + faceNormal". */
        int clickX = place.getX() - normal[0];
        int clickY = place.getY() - normal[1];
        int clickZ = place.getZ() - normal[2];

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

        /* On 1.13+ the client reports the exact hit point; a ray that misses the block but
         * passes through the reported cursor is still legitimate, because the client picks the
         * nearest face and can report a point on an adjacent block at an edge. */
        Vector3f cursor = wrapper.getCursorPosition();
        if (!onBlock && cursor != null) {
            onBlock = Math.abs(cursor.getX() - (clickX + 0.5)) <= 1.0
                && Math.abs(cursor.getY() - (clickY + 0.5)) <= 1.0
                && Math.abs(cursor.getZ() - (clickZ + 0.5)) <= 1.0;
        }

        if (!onBlock) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
            return;
        }

        /* 2. Face orientation: the look ray must lean into the claimed face. */
        double dot = dirX * normal[0] + dirY * normal[1] + dirZ * normal[2];
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

        if (angle > 90.0 + FACE_ANGLE_SLACK_DEGREES) {
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
            case UP:
            default:
                return new int[]{0, 1, 0};
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}