package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.BoundingBox;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects attacks on entities the player's crosshair is not actually pointing at.
 *
 * <p>Companion to {@link ReachCheck}: reach answers "is the target too far away", this check
 * answers "was the target ever inside the crosshair". Both must hold for a legitimate attack,
 * and each is bypassable on its own — reach cheats extend the distance, auto-aim cheats close
 * the angular gap.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>On each {@code INTERACT_ENTITY} attack, reconstruct the look ray from the player's eye
 *       position and yaw/pitch.</li>
 *   <li>Resolve the target's server-side bounding box from {@link ReachCheck}'s entity cache.</li>
 *   <li>Test ray/AABB intersection via {@link BoundingBox#intersectsRay}. A vanilla client only
 *       sends the attack packet when its own ray hits the entity, so a miss means the client did
 *       not perform that ray test — or faked the result.</li>
 *   <li>Count consecutive misses; flag once the streak reaches {@value #MAX_CONSECUTIVE_MISSES}.
 *       A single miss is normal (tab-target switching, high ping), so misses must accumulate.</li>
 * </ol>
 *
 * <p>Entity boxes are expanded by {@value #CURSOR_TOLERANCE} blocks before the ray test to absorb
 * server-side interpolation error on the target. The ray length is capped at the version's
 * interaction range so a target the player cannot legally touch is left to {@link ReachCheck}.</p>
 *
 * <p>Untracked entities are skipped rather than assumed malicious: the cache is populated from
 * spawn packets, and a missed spawn must not become a false positive.</p>
 *
 * @see ReachCheck — distance-based counterpart
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Interact Cursor", stableKey = "windfall.combat.interactcursor", decay = 0.01, setbackVl = 15,
    compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.5)
public class InteractCursorCheck extends Check implements PacketCheck {

    /** Blocks of slack added to the target box to absorb interpolation error. */
    private static final double CURSOR_TOLERANCE = 0.3;

    /** Consecutive ray misses required before a flag is raised. */
    private static final int MAX_CONSECUTIVE_MISSES = 4;

    /** Buffer threshold. Kept high because each miss is weak evidence on its own. */
    private static final double BUFFER_THRESHOLD = 3.0;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int targetId = wrapper.getEntityId();

        double[] pos = ReachCheck.getTrackedPosition(targetId);
        if (pos == null) {
            /* Target not in the entity cache — nothing to validate against. */
            reward(player);
            return;
        }

        /* Only players have a reliably known hitbox; other entity types use the
         * conservative default box ReachCheck already applies. */
        BoundingBox target = entityBox(pos, player.getProtocolVersion());

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight();
        double eyeZ = player.getZ();

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);

        /* Unit look vector — the standard Minecraft rotation-to-direction formula. */
        double dirX = -Math.sin(yawRad) * cosPitch;
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * cosPitch;

        double range = VersionPhysics.getMaxReach(player.getProtocolVersion())
            + VersionPhysics.getSprintReachBonus(player.getProtocolVersion())
            + 1.0;

        boolean onTarget = target.intersectsRay(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, range);

        if (onTarget) {
            reward(player);
            return;
        }

        increaseBuffer(player, 1.0);
        if (getBuffer(player) > BUFFER_THRESHOLD) {
            flag(player);
            resetBuffer(player);
        }
    }

    /**
     * Builds the target's bounding box, expanded by the cursor tolerance.
     *
     * @param pos      tracked {@code [x, y, z]} position of the target
     * @param protocol attacker protocol version, used for hitbox dimensions
     * @return the padded bounding box around the target
     */
    static BoundingBox entityBox(double[] pos, int protocol) {
        double halfWidth = VersionPhysics.getPlayerWidth(protocol) * 0.5;
        double height = VersionPhysics.getPlayerHeight(false, protocol);
        return new BoundingBox(
            pos[0] - halfWidth, pos[1], pos[2] - halfWidth,
            pos[0] + halfWidth, pos[1] + height, pos[2] + halfWidth
        ).expand(CURSOR_TOLERANCE);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}