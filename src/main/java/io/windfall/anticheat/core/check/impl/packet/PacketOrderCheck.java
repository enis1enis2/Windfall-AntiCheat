package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Packet Order A", stableKey = "windfall.packet.order", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class PacketOrderCheck extends Check implements PacketCheck {

    private static final int MAX_MOVEMENT_BEFORE_LOGIN = 0;
    private static final int DUPLICATE_PACKET_THRESHOLD = 5;
    private static final long PACKET_BURST_WINDOW_MS = 100;
    private static final int MAX_PACKETS_IN_BURST = 15;

    private static final class PlayerState {
        boolean loginComplete;
        int movementCountBeforeLogin;
        int duplicatePacketCount;
        long lastPacketHash;
        final ArrayDeque<Long> packetBurst = new ArrayDeque<>();
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
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        state.packetBurst.addLast(now);
        while (!state.packetBurst.isEmpty() && now - state.packetBurst.peekFirst() > PACKET_BURST_WINDOW_MS) {
            state.packetBurst.removeFirst();
        }
        if (state.packetBurst.size() > MAX_PACKETS_IN_BURST) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        long currentHash = type.hashCode();
        if (currentHash == state.lastPacketHash && isMovementType(type)) {
            state.duplicatePacketCount++;
            if (state.duplicatePacketCount > DUPLICATE_PACKET_THRESHOLD) {
                flag(player);
                state.duplicatePacketCount = 0;
            }
        } else {
            state.duplicatePacketCount = 0;
        }
        state.lastPacketHash = currentHash;

        if (!state.loginComplete) {
            if (isMovementType(type)) {
                state.movementCountBeforeLogin++;
            }
            if (type == PacketType.Play.Client.PLAYER_POSITION
                    || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                state.loginComplete = true;
                if (state.movementCountBeforeLogin > MAX_MOVEMENT_BEFORE_LOGIN) {
                    flag(player);
                }
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    public void onLoginComplete(WindfallPlayer player) {
        getState(player).loginComplete = true;
    }

    public void onDisconnect(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.loginComplete = false;
        state.movementCountBeforeLogin = 0;
        state.duplicatePacketCount = 0;
        state.lastPacketHash = 0;
        state.packetBurst.clear();
    }

    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }
}
