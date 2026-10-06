package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.util.MaterialUtils;
import org.bukkit.World;

/**
 * Detects block placements that end up floating with no supporting block on any face.
 *
 * <p>Vanilla can only place a block by clicking the face of an existing block, so the clicked
 * block is always a neighbour of the placement target. A target that has no non-air neighbour
 * on the server therefore means the client placed against a block the server does not have —
 * the ghost-block / blink pattern that scaffold clients produce when they bridge ahead of
 * server-authoritative world state.</p>
 *
 * <h3>Why not "support below"</h3>
 * <p>An earlier revision required a solid block beneath the placement, mirroring a
 * {@code requiresSupportBelow} material table. That rule is unsound: horizontal bridging over a
 * void legitimately places blocks with air underneath, so five bridging placements tripped the
 * buffer. Surveying established anti-cheats confirms none of them run it as a standalone rule:</p>
 * <ul>
 *   <li>Grim's {@code GhostBlockMitigation} checks adjacency instead of support-below, and its
 *       {@code AirLiquidPlace} checks that the block being clicked is not air or liquid.</li>
 *   <li>Reflex only consults air-below as one condition inside a compound heuristic that also
 *       requires active bridging state, a neighbour count and build-speed timing — signals this
 *       check does not have.</li>
 * </ul>
 * <p>Adjacency is the subset of those rules that is sound without the missing state.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Read the clicked block from the packet; the packet position is the clicked block, and
 *       vanilla places at {@code clicked + faceNormal}.</li>
 *   <li>Inspect the six faces of that placement target in the server-side world. One non-air
 *       neighbour means the placement is supported.</li>
 *   <li>No non-air neighbour is a violation. Relief and chunk load lags make this accumulate in
 *       a buffer rather than flag on the first occurrence.</li>
 * </ol>
 *
 * @see ScaffoldCheck — movement-side scaffold detection
 * @see InvalidPlaceCheck — companion check for invalid placement state
 * @see InvalidPlaceCursorCheck — companion check that the click was aimed at
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Scaffold Support", stableKey = "windfall.movement.scaffoldsupport", decay = 0.02, setbackVl = 10,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class ScaffoldSupportCheck extends Check implements PacketCheck {

    /** Buffer threshold — relief application lags by a tick, so accumulate before flagging. */
    private static final double BUFFER_THRESHOLD = 4.0;

    /** Six face offsets of the placement target. */
    private static final int[][] NEIGHBOUR_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        if (wrapper.getFace() == null || wrapper.getFace() == BlockFace.OTHER) return;

        Vector3i clicked = wrapper.getBlockPosition();
        if (clicked == null) return;

        /* Vanilla places at clicked + faceNormal; the packet itself carries only the click. */
        int[] normal = InvalidPlaceCursorCheck.faceNormal(wrapper.getFace());
        int tx = clicked.getX() + normal[0];
        int ty = clicked.getY() + normal[1];
        int tz = clicked.getZ() + normal[2];

        try {
            World world = player.getPlayer().getWorld();

            /* Reading an unloaded chunk yields air for every face, which would look like a
             * placement floating in nothing. Skip instead of inventing a violation. */
            if (!world.isChunkLoaded(tx >> 4, tz >> 4)) {
                reward(player);
                return;
            }

            boolean supported = false;
            for (int[] offset : NEIGHBOUR_OFFSETS) {
                if (!MaterialUtils.isAirLike(
                        world.getBlockAt(tx + offset[0], ty + offset[1], tz + offset[2]).getType())) {
                    supported = true;
                    break;
                }
            }

            if (supported) {
                reward(player);
                return;
            }

            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } catch (Exception e) {
            /* World access can fail transiently on Folia region boundaries; do not flag. */
            reward(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
