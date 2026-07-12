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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players whose view rotation changes by more than a reasonable angle
 * between the start and finish of a block break. In vanilla, a player cannot
 * physically rotate their camera more than {@link #MAX_ROTATION_DELTA} degrees
 * during the few-tick break animation without assistance.
 *
 * <p><b>Algorithm:</b> On START_DIGGING, the player's yaw and pitch are
 * snapshot. On FINISHED_DIGGING the angular deltas are computed. Yaw is
 * normalised across the 360-degree boundary. If either delta exceeds 45
 * degrees, the buffer increments; three consecutive violations trigger a flag.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Rotation Break A", stableKey = "windfall.movement.rotationbreak", decay = 0.02, setbackVl = 15)
public class RotationBreakCheck extends Check implements PacketCheck {

    /** Maximum allowed rotation change (degrees) between break start and finish. */
    private static final float MAX_ROTATION_DELTA = 45.0f;

    /** Per-player state storing the rotation snapshot at break start. */
    private static final class PlayerState {
        /** Yaw (horizontal angle) when START_DIGGING was received. */
        float breakStartYaw;
        /** Pitch (vertical angle) when START_DIGGING was received. */
        float breakStartPitch;
        /** Whether a break is currently in progress. */
        boolean breaking;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Tracks rotation deltas across the break lifecycle: snapshots yaw/pitch on
     * start, compares on finish, and flags if the delta exceeds the threshold.
     *
     * @param player the player associated with this packet
     * @param event  the incoming digging packet
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        DiggingAction action = wrapper.getAction();
        PlayerState state = getState(player);

        if (action == DiggingAction.START_DIGGING) {
            state.breaking = true;
            state.breakStartYaw = player.getYaw();
            state.breakStartPitch = player.getPitch();
        } else if (action == DiggingAction.FINISHED_DIGGING) {
            if (!state.breaking) return;
            state.breaking = false;

            float deltaYaw = Math.abs(player.getYaw() - state.breakStartYaw);
            float deltaPitch = Math.abs(player.getPitch() - state.breakStartPitch);

            /** Normalise yaw delta across the 360/0 degree wrap-around. */
            if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;

            if (deltaYaw > MAX_ROTATION_DELTA || deltaPitch > MAX_ROTATION_DELTA) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        } else if (action == DiggingAction.CANCELLED_DIGGING) {
            state.breaking = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
