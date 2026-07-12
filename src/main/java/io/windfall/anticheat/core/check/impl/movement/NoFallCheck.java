package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "NoFall A", stableKey = "windfall.movement.nofall", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class NoFallCheck extends Check implements PacketCheck {

    private static final double MIN_FALL_VELOCITY = 0.3;
    private static final double MIN_FALL_DISTANCE = 2.0;
    private static final int MAX_CONSECUTIVE = 5;

    private static final class PlayerState {
        int consecutiveNoFall;
        double maxFallDistance;
        double maxFallVelocity;
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

        boolean onGround = player.isOnGround();
        double deltaY = player.getDeltaY();
        double fallDistance = player.getLastY() - player.getY();

        if (deltaY < -MIN_FALL_VELOCITY && fallDistance > MIN_FALL_DISTANCE && onGround) {
            state.consecutiveNoFall++;

            if (fallDistance > state.maxFallDistance) state.maxFallDistance = fallDistance;
            if (Math.abs(deltaY) > state.maxFallVelocity) state.maxFallVelocity = Math.abs(deltaY);

            if (state.consecutiveNoFall >= MAX_CONSECUTIVE) {
                flag(player);
                state.consecutiveNoFall = 0;
                state.maxFallDistance = 0;
                state.maxFallVelocity = 0;
            }
        } else {
            state.consecutiveNoFall = Math.max(0, state.consecutiveNoFall - 1);
        }
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
