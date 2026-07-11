package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Speed A", stableKey = "windfall.movement.speed", decay = 0.01, setbackVl = 20)
public class SpeedCheck extends Check implements PacketCheck {

    private static final double SPEED_TOLERANCE = 1.05;
    private static final double PRE_1_18_2_THRESHOLD = 0.03;
    private static final double MIN_SPEED_FLAG_BUFFER = 3.0;

    private static final class PlayerState {
        double maxObservedSpeed;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        PredictionContext ctx = new PredictionContext(player);

        double actualSpeed = ctx.horizontalSpeed;

        PlayerState state = getState(player);
        if (actualSpeed > state.maxObservedSpeed) {
            state.maxObservedSpeed = actualSpeed;
        }

        if (actualSpeed < PRE_1_18_2_THRESHOLD && ctx.protocolVersion < 757) {
            decreaseBuffer(player, 0.1);
            return;
        }

        if (actualSpeed < 0.005) {
            decreaseBuffer(player, 0.05);
            return;
        }

        double maxSpeed = ctx.predictedMaxHorizontalSpeed;

        if (actualSpeed > maxSpeed * SPEED_TOLERANCE) {
            double exceedRatio = actualSpeed / Math.max(maxSpeed, 0.001);
            if (exceedRatio > 2.0) {
                flag(player);
                resetBuffer(player);
            } else {
                increaseBuffer(player, 0.5 * (exceedRatio - 1.0));
                if (getBuffer(player) > MIN_SPEED_FLAG_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    public double getMaxObservedSpeed(WindfallPlayer player) {
        return getState(player).maxObservedSpeed;
    }
}
