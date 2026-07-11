package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;

@CheckData(name = "Packet Order A", stableKey = "windfall.packet.order", decay = 0.01, setbackVl = 15)
public class PacketOrderCheck extends Check implements PacketCheck {

    // Client must send at least one movement packet between login and any interaction
    private static final int MAX_MOVEMENT_BEFORE_LOGIN = 0;
    private static final int DUPLICATE_PACKET_THRESHOLD = 5;
    private static final long PACKET_BURST_WINDOW_MS = 100;
    private static final int MAX_PACKETS_IN_BURST = 15;

    private boolean loginComplete;
    private int movementCountBeforeLogin;
    private int duplicatePacketCount;
    private long lastPacketHash;
    private final ArrayDeque<Long> packetBurst = new ArrayDeque<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();

        // Track burst rate
        packetBurst.addLast(now);
        while (!packetBurst.isEmpty() && now - packetBurst.peekFirst() > PACKET_BURST_WINDOW_MS) {
            packetBurst.removeFirst();
        }
        if (packetBurst.size() > MAX_PACKETS_IN_BURST) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        // Detect duplicate packets (same type hash in sequence)
        long currentHash = type.hashCode();
        if (currentHash == lastPacketHash && isMovementType(type)) {
            duplicatePacketCount++;
            if (duplicatePacketCount > DUPLICATE_PACKET_THRESHOLD) {
                flag(player);
                duplicatePacketCount = 0;
            }
        } else {
            duplicatePacketCount = 0;
        }
        lastPacketHash = currentHash;

        if (!loginComplete) {
            if (isMovementType(type)) {
                movementCountBeforeLogin++;
            }
            if (type == PacketType.Play.Client.PLAYER_POSITION
                    || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                loginComplete = true;
                if (movementCountBeforeLogin > MAX_MOVEMENT_BEFORE_LOGIN) {
                    flag(player);
                }
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    public void onLoginComplete(WindfallPlayer player) {
        this.loginComplete = true;
    }

    public void onDisconnect(WindfallPlayer player) {
        this.loginComplete = false;
        this.movementCountBeforeLogin = 0;
        this.duplicatePacketCount = 0;
        this.lastPacketHash = 0;
        this.packetBurst.clear();
    }

    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }
}
