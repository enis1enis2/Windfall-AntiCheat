package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@CheckData(
    name = "Illegal Move",
    stableKey = "windfall.movement.illegalmove",
    decay = 0.4,
    setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.2,
    minVersion = 4,
    maxVersion = 99999
)
public class IllegalMoveCheck extends Check implements PacketCheck {

    private static final double MAX_HORIZONTAL_TELEPORT = 5.0;
    private static final double MAX_VERTICAL_TELEPORT = 5.0;
    private static final double MAX_BLOCK_CLIP = 0.1;
    private static final int MIN_CLIP_TICKS = 3;
    private static final double MAX_BUFFER = 13.0;
    private static final double FLYING_TELEPORT_THRESHOLD = 50.0;

    private static final class PlayerState {
        int clipTicks;
        int teleportBuffer;
        boolean wasTeleport;
        final ArrayDeque<Double> recentSpeeds = new ArrayDeque<>();
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
        if (!enabled) return;

        PlayerState state = getState(player);
        PredictionContext ctx = new PredictionContext(player);

        double horizontalSpeed = ctx.horizontalSpeed;
        double verticalDelta = Math.abs(ctx.deltaY);
        boolean flying = player.isFlying();
        boolean onGround = ctx.onGround;
        boolean climbing = ctx.climbing;

        detectTeleport(player, state, ctx, horizontalSpeed, verticalDelta, flying);
        detectVerticalClip(player, state, ctx, onGround, climbing);

        reward(player);
    }

    private void detectTeleport(WindfallPlayer player, PlayerState state, PredictionContext ctx,
                                 double horizontalSpeed, double verticalDelta, boolean flying) {
        double teleportThreshold = flying ? FLYING_TELEPORT_THRESHOLD : MAX_HORIZONTAL_TELEPORT;
        double verticalThreshold = flying ? FLYING_TELEPORT_THRESHOLD : MAX_VERTICAL_TELEPORT;

        if (horizontalSpeed > teleportThreshold || verticalDelta > verticalThreshold) {
            state.teleportBuffer++;
            if (state.teleportBuffer >= MIN_CLIP_TICKS) {
                increaseBuffer(player, 2.0);
                double buf = getBuffer(player);
                if (buf > MAX_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.teleportBuffer = Math.max(0, state.teleportBuffer - 1);
        }
    }

    private void detectVerticalClip(WindfallPlayer player, PlayerState state, PredictionContext ctx,
                                     boolean onGround, boolean climbing) {
        if (climbing) return;
        if (ctx.swimming || ctx.inWater || ctx.inLava) return;
        if (ctx.deltaY <= 0) return;
        if (onGround) return;

        try {
            org.bukkit.entity.Player bukkitPlayer = player.getPlayer();
            if (bukkitPlayer == null) return;

            Location loc = bukkitPlayer.getLocation();
            Block below = loc.getBlock();
            Block inside = loc.clone().add(0, 0.3, 0).getBlock();
            Block head = loc.clone().add(0, 1.6, 0).getBlock();

            boolean belowSolid = below.getType().isSolid();
            boolean insideSolid = inside.getType().isSolid();
            boolean headSolid = head.getType().isSolid();

            if (insideSolid || headSolid) {
                state.clipTicks++;
                if (state.clipTicks >= MIN_CLIP_TICKS) {
                    increaseBuffer(player, 1.5);
                    double buf = getBuffer(player);
                    if (buf > MAX_BUFFER) {
                        flagWithSetback(player);
                        resetBuffer(player);
                    }
                }
            } else {
                state.clipTicks = Math.max(0, state.clipTicks - 1);
                decreaseBuffer(player, 0.2);
            }
        } catch (Exception e) {
            state.clipTicks = 0;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
            || type == PacketType.Play.Client.PLAYER_POSITION
            || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
            || type == PacketType.Play.Client.PLAYER_ROTATION;
    }
}
