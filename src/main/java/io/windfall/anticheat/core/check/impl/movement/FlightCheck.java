package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Fly A", stableKey = "windfall.movement.fly", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class FlightCheck extends Check implements PacketCheck {

    private static final double JUMP_MOMENTUM = 0.42;
    private static final double VERTICAL_TOLERANCE = 0.05;
    private static final int HOVER_TICK_THRESHOLD = 20;
    private static final double HOVER_DELTA_THRESHOLD = 0.005;
    private static final double NO_FALL_VELOCITY_THRESHOLD = 0.5;
    private static final double NO_FALL_DISTANCE = 3.0;

    private static final class PlayerState {
        double expectedDeltaY;
        int hoverTicks;
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

        boolean currentOnGround = ctx.onGround;
        double deltaY = ctx.deltaY;

        if (currentOnGround) {
            state.expectedDeltaY = 0;
            state.hoverTicks = 0;
            return;
        }

        if (ctx.lastOnGround && !currentOnGround) {
            if (deltaY >= JUMP_MOMENTUM - 0.01 && deltaY <= JUMP_MOMENTUM + 0.15) {
                state.expectedDeltaY = JUMP_MOMENTUM;
            } else if (Math.abs(deltaY) < 0.01) {
                state.expectedDeltaY = 0;
            }
        }

        boolean hasRiptide = PredictionEngine.checkRiptiding(player);
        boolean isFallFlying = PredictionEngine.checkFallFlying(player);

        double predictedDeltaY = PredictionEngine.predictDeltaY(
                state.expectedDeltaY,
                ctx.inWater,
                ctx.inLava,
                ctx.climbing,
                PredictionEngine.checkOnHoney(player),
                ctx.hasSlowFalling,
                ctx.hasLevitation,
                ctx.hasLevitation ? PredictionEngine.getLevitationAmplifier(player) : 1.0,
                isFallFlying,
                hasRiptide
        );

        double verticalDelta = deltaY - predictedDeltaY;
        boolean verticalDeviation = Math.abs(verticalDelta) > VERTICAL_TOLERANCE
                && Math.abs(deltaY) > 0.01;

        if (verticalDeviation && !isFallFlying && !hasRiptide && !ctx.hasLevitation) {
            handleHoverDetection(player, state, ctx);

            if (deltaY > 0 && state.expectedDeltaY <= 0 && !ctx.hasLevitation && !hasRiptide && !isFallFlying) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                double deviationRatio = Math.abs(verticalDelta) / Math.max(Math.abs(predictedDeltaY), 0.001);
                if (deviationRatio > 2.0) {
                    flag(player);
                    resetBuffer(player);
                } else {
                    increaseBuffer(player, 0.3 * Math.min(deviationRatio, 2.0));
                    if (getBuffer(player) > 5.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
            state.hoverTicks = Math.max(0, state.hoverTicks - 1);
        }

        handleNoFall(player, currentOnGround, deltaY, ctx.lastY, ctx.y);
        state.expectedDeltaY = deltaY;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleHoverDetection(WindfallPlayer player, PlayerState state, PredictionContext ctx) {
        double yMoved = Math.abs(ctx.deltaY);

        if (yMoved < HOVER_DELTA_THRESHOLD && !ctx.inWater && !ctx.inLava && !ctx.climbing) {
            state.hoverTicks++;
            if (state.hoverTicks > HOVER_TICK_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                    state.hoverTicks = 0;
                }
            }
        } else {
            state.hoverTicks = Math.max(0, state.hoverTicks - 1);
        }
    }

    private void handleNoFall(WindfallPlayer player, boolean currentOnGround, double deltaY,
                              double lastY, double currentY) {
        if (!currentOnGround && deltaY < -NO_FALL_VELOCITY_THRESHOLD) {
            double fallDistance = lastY - currentY;
            if (fallDistance > NO_FALL_DISTANCE) {
                if (currentOnGround) {
                    flagWithSetback(player);
                } else {
                    flag(player);
                }
            }
        }
    }
}
