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

@CheckData(name = "Ground Spoof A", stableKey = "windfall.movement.groundspoof", decay = 0.01, setbackVl = 20)
public class GroundSpoofCheck extends Check implements PacketCheck {

    private static final double MIN_AIR_TIME_FOR_GROUND = 3.0;
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_FALSE_GROUND_FLAGS = 5;
    private static final double FALLING_VELOCITY_THRESHOLD = 0.3;
    private static final double MIN_FALL_DISTANCE = 2.0;

    private static final class PlayerState {
        int falseGroundCount;
        int airTicks;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        PlayerState state = getState(player);
        PredictionContext ctx = new PredictionContext(player);

        boolean claimsGround = ctx.onGround;
        double deltaY = ctx.deltaY;
        boolean isFalling = deltaY < -FALLING_VELOCITY_THRESHOLD;
        double fallDistance = ctx.lastY - ctx.y;

        if (!claimsGround) {
            state.airTicks++;
            return;
        }

        if (isFalling && fallDistance > MIN_FALL_DISTANCE) {
            state.falseGroundCount++;
            if (state.falseGroundCount >= MIN_FALSE_GROUND_FLAGS) {
                flag(player);
                resetBuffer(player);
                state.falseGroundCount = 0;
            }
            return;
        }

        if (state.airTicks > MIN_AIR_TIME_FOR_GROUND * TICKS_PER_SECOND) {
            if (Math.abs(deltaY) > 0.5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.airTicks = 0;
        state.falseGroundCount = Math.max(0, state.falseGroundCount - 1);
        decreaseBuffer(player, 0.1);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
