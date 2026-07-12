package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Simulation A", stableKey = "windfall.movement.simulation", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.4)
public class SimulationCheck extends Check implements PacketCheck {

    private static final double MAX_SIMULATION_DEVIATION = 0.15;
    private static final int MIN_SAMPLES = 10;

    private static final class PlayerState {
        double expectedDeltaY;
        int samples;
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
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();

        if (onGround) {
            state.expectedDeltaY = 0;
            state.samples = 0;
            return;
        }

        int protocol = player.getProtocolVersion();
        double gravity = PhysicsConstants.GRAVITY;
        double airDrag = PhysicsConstants.AIR_DRAG;

        if (player.isSwimming()) {
            double waterDrag = protocol >= 393 ? PhysicsConstants.WATER_DRAG : 0.8;
            double predictedDeltaY = (state.expectedDeltaY - gravity) * waterDrag;
            double verticalDeviation = Math.abs(deltaY - predictedDeltaY);
            checkDeviation(player, verticalDeviation, deltaX, deltaZ, state);
            state.expectedDeltaY = deltaY;
            return;
        }

        double predictedDeltaY = (state.expectedDeltaY - gravity) * airDrag;

        double verticalDeviation = Math.abs(deltaY - predictedDeltaY);
        checkDeviation(player, verticalDeviation, deltaX, deltaZ, state);

        state.expectedDeltaY = deltaY;
    }

    private void checkDeviation(WindfallPlayer player, double verticalDeviation, double deltaX, double deltaZ, PlayerState state) {
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double maxDeviation = MAX_SIMULATION_DEVIATION;
        if (player.isBedrock()) {
            maxDeviation *= 1.15;
        }
        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);
        if (bracket == VersionBracket.LEGACY || bracket == VersionBracket.COMBAT) {
            maxDeviation *= 1.2;
        }

        if (verticalDeviation > maxDeviation && horizontalSpeed > 0.1) {
            state.samples++;
            if (state.samples >= MIN_SAMPLES) {
                increaseBuffer(player, 0.5 * (verticalDeviation / maxDeviation));
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.samples = 0;
                }
            }
        } else {
            state.samples = Math.max(0, state.samples - 1);
            decreaseBuffer(player, 0.1);
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
