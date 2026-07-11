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

@CheckData(name = "Phase A", stableKey = "windfall.movement.phase", decay = 0.01, setbackVl = 20)
public class PhaseCheck extends Check implements PacketCheck {

    private static final double MAX_BLOCK_CLIP = 0.1;
    private static final double MAX_VELOCITY_INSIDE_BLOCK = 0.01;
    private static final int MIN_CLIPPING_TICKS = 3;

    private static final class PlayerState {
        int clippingTicks;
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

        try {
            org.bukkit.Location loc = new org.bukkit.Location(player.getPlayer().getWorld(), ctx.x, ctx.y, ctx.z);
            org.bukkit.block.Block feetBlock = loc.getBlock();
            org.bukkit.block.Block headBlock = loc.clone().add(0, 1.6, 0).getBlock();

            boolean feetInside = feetBlock.getType().isSolid();
            boolean headInside = headBlock.getType().isSolid();

            if (feetInside || headInside) {
                state.clippingTicks++;
                if (state.clippingTicks >= MIN_CLIPPING_TICKS) {
                    if (ctx.horizontalSpeed > MAX_VELOCITY_INSIDE_BLOCK
                            || Math.abs(ctx.deltaY) > MAX_VELOCITY_INSIDE_BLOCK) {
                        increaseBuffer(player, 1.5);
                        if (getBuffer(player) > 3.0) {
                            flag(player);
                            resetBuffer(player);
                            state.clippingTicks = 0;
                        }
                    }
                }
            } else {
                state.clippingTicks = Math.max(0, state.clippingTicks - 1);
                decreaseBuffer(player, 0.2);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
