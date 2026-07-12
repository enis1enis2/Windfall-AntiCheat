package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Baritone A", stableKey = "windfall.movement.baritone", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class BaritoneCheck extends Check implements PacketCheck {

    private static final double STRAIGHT_LINE_TOLERANCE = 0.02;
    private static final int MIN_STRAIGHT_TICKS = 20;
    private static final double PERFECT_PATH_THRESHOLD = 0.95;
    private static final int PATH_SAMPLE_SIZE = 40;

    private static final class PlayerState {
        int straightTicks;
        double lastDeltaX;
        double lastDeltaZ;
        int perfectPathSegments;
        int totalSegments;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed < 0.05) {
            state.straightTicks = 0;
            decreaseBuffer(player, 0.1);
            return;
        }

        double lastSpeed = Math.sqrt(state.lastDeltaX * state.lastDeltaX + state.lastDeltaZ * state.lastDeltaZ);

        if (lastSpeed > 0.05 && horizontalSpeed > 0.05) {
            double angle = Math.abs(Math.atan2(deltaZ, deltaX) - Math.atan2(state.lastDeltaZ, state.lastDeltaX));
            if (angle > Math.PI) angle = 2 * Math.PI - angle;

            if (angle < STRAIGHT_LINE_TOLERANCE) {
                state.straightTicks++;
            } else {
                state.straightTicks = 0;
            }

            state.totalSegments++;
            double speedDiff = Math.abs(horizontalSpeed - lastSpeed);
            if (speedDiff < 0.005) {
                state.perfectPathSegments++;
            }
        }

        if (state.straightTicks > MIN_STRAIGHT_TICKS) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
                state.straightTicks = 0;
            }
        }

        if (state.totalSegments >= PATH_SAMPLE_SIZE) {
            double perfectRatio = (double) state.perfectPathSegments / state.totalSegments;
            if (perfectRatio > PERFECT_PATH_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.2);
            }
            state.totalSegments = 0;
            state.perfectPathSegments = 0;
        }

        state.lastDeltaX = deltaX;
        state.lastDeltaZ = deltaZ;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
