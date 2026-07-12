package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects block placements where the player's rotation does not face the target block.
 *
 * <p>In vanilla Minecraft, the server validates that the player's look direction is consistent with
 * the block face they are placing on. Hacked clients can bypass this by sending rotation spoofed
 * only on the client side, then sending placement packets with arbitrary server-side rotations.
 *
 * <p><b>Detection algorithm:</b>
 * <ol>
 *   <li>Compute the vector from the player's eye position to the block centre.</li>
 *   <li>Derive the expected yaw and pitch angles using {@link Math#atan2} trigonometry.</li>
 *   <li>Compare the expected angles with the player's actual rotation (from the player data).</li>
 *   <li>If either yaw or pitch deviation exceeds {@value #MAX_YAW_DEVIATION}° /
 *       {@value #MAX_PITCH_DEVIATION}°, increase the buffer.</li>
 * </ol>
 *
 * <p><b>Yaw wrapping:</b> Yaw angles wrap at 360°, so the deviation is normalised to the
 * [0, 180] range.
 *
 * <p><b>Buffer logic:</b> Each violation adds 1.0. Flag at buffer &gt; {@value #BUFFER_THRESHOLD}.
 * Valid placements decay by 0.5. The high threshold (5) reduces false positives from latency.
 *
 * @see PositionPlaceCheck — companion check for placement distance
 * @see FarPlaceCheck — companion check for placement reach
 */
@CheckData(name = "Rotation Place", stableKey = "windfall.movement.rotationplace", decay = 0.02, setbackVl = 10)
public class RotationPlaceCheck extends Check implements PacketCheck {

    /** Maximum allowed yaw deviation in degrees before the placement is considered suspicious. */
    private static final float MAX_YAW_DEVIATION = 90.0f;

    /** Maximum allowed pitch deviation in degrees before the placement is considered suspicious. */
    private static final float MAX_PITCH_DEVIATION = 90.0f;

    /**
     * Buffer must exceed this value before a flag is raised.
     * Set to 5 to reduce false positives from latency-induced rotation jitter.
     */
    private static final int BUFFER_THRESHOLD = 5;

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT} packets with a
     * non-null face. Computes expected rotation angles and compares with the player's actual
     * rotation, flagging significant deviations.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        if (wrapper.getFace() == null) return;

        int blockX = wrapper.getBlockPosition().getX();
        int blockY = wrapper.getBlockPosition().getY();
        int blockZ = wrapper.getBlockPosition().getZ();

        /* Eye position = feet position + player height (e.g. 1.62 for Java players) */
        double eyeX = player.getX();
        double eyeY = player.getY() + player.getHeight();
        double eyeZ = player.getZ();

        /* Vector from player eye to block centre */
        double dx = (blockX + 0.5) - eyeX;
        double dy = (blockY + 0.5) - eyeY;
        double dz = (blockZ + 0.5) - eyeZ;

        /*
         * Expected rotation angles:
         *   yaw   = atan2(-dx, dz)   (horizontal angle, Minecraft convention)
         *   pitch = atan2(-dy, distXZ) (vertical angle, negative = looking up)
         */
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float expectedYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float expectedPitch = (float) Math.toDegrees(Math.atan2(-dy, distXZ));

        /* Yaw difference normalised to [0, 180] range (handles 360° wrap-around) */
        float deltaYaw = Math.abs(player.getYaw() - expectedYaw);
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;

        float deltaPitch = Math.abs(player.getPitch() - expectedPitch);

        if (deltaYaw > MAX_YAW_DEVIATION || deltaPitch > MAX_PITCH_DEVIATION) {
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
