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

/**
 * Detects out-of-order, duplicate, and burst packet sequences that indicate hacked clients.
 *
 * <p>Detection strategy (three independent checks):
 * <ul>
 *   <li><b>Packet burst detection</b> &mdash; tracks all incoming packets in a {@value #PACKET_BURST_WINDOW_MS}ms
 *       sliding window. Flags if more than {@value #MAX_PACKETS_IN_BURST} packets arrive in that window
 *       (buffer +1.0, flags at buffer >3.0). Indicates packet spam or automation.</li>
 *   <li><b>Consecutive duplicate movement packets</b> &mdash; tracks the hash of the last movement packet type.
 *       If the same movement type arrives more than {@value #DUPLICATE_PACKET_THRESHOLD} times in a row,
 *       flags immediately. Detects stuck-position or spoofed movement exploits.</li>
 *   <li><b>Pre-login movement</b> &mdash; counts movement packets received before the first position/position+rotation
 *       packet (which signals login completion). Flags if any movement packets arrive before login,
 *       which is impossible for legitimate clients.</li>
 * </ul>
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value #MAX_MOVEMENT_BEFORE_LOGIN} &mdash; zero tolerance for pre-login movement</li>
 *   <li>{@value #DUPLICATE_PACKET_THRESHOLD} consecutive identical movement types before flag</li>
 *   <li>{@value #MAX_PACKETS_IN_BURST} packets in {@value #PACKET_BURST_WINDOW_MS}ms</li>
 * </ul>
 *
 * <p>Setback at VL 15, decay 0.01/tick. Uses RELAX_ON_MISMATCH with 1.2x multiplier.
 *
 * @see BadPacketsCheck for per-packet field validation
 * @see SprintCheck for sprint state anomaly detection
 */
@CheckData(name = "Packet Order A", stableKey = "windfall.packet.order", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class PacketOrderCheck extends Check implements PacketCheck {

    /** Maximum movement packets allowed before the first position packet (login signal) — zero tolerance */
    private static final int MAX_MOVEMENT_BEFORE_LOGIN = 0;
    /** Number of consecutive identical movement packet types before flagging */
    private static final int DUPLICATE_PACKET_THRESHOLD = 5;
    /** Duration of the packet burst detection window in milliseconds */
    private static final long PACKET_BURST_WINDOW_MS = 100;
    /** Maximum packets allowed in the burst window before flagging */
    private static final int MAX_PACKETS_IN_BURST = 15;

    /**
     * Per-player state tracking packet ordering, duplicates, and burst detection.
     */
    private static final class PlayerState {
        /** Whether the player has sent a position packet (login completion signal) */
        boolean loginComplete;
        /** Count of movement packets received before the first position packet */
        int movementCountBeforeLogin;
        /** Count of consecutive movement packets with identical type hash */
        int duplicatePacketCount;
        /** Hash of the last movement packet type for duplicate detection */
        long lastPacketHash;
        /** Timestamps of incoming packets for burst detection (sliding window) */
        final ArrayDeque<Long> packetBurst = new ArrayDeque<>();
    }

    /** Thread-safe map of player UUID to their packet ordering state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player packet ordering state.
     *
     * @param player the player to get state for
     * @return the player's state
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** {@inheritDoc} Clears player state to prevent memory leaks on disconnect */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Main packet receive handler. Applies all three detection strategies:
     * burst detection, consecutive duplicate detection, and pre-login movement detection.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        /* Add current timestamp and evict entries outside the burst detection window */
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

        /* Check consecutive duplicate movement packet types via hash comparison */
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

        /* Detect pre-login movement: count movement packets before first position packet */
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

    /** {@inheritDoc} No outgoing packet processing needed for packet order checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Called externally when login completes. Marks the player as logged in,
     * preventing false positive pre-login movement flags.
     *
     * @param player the player who completed login
     */
    public void onLoginComplete(WindfallPlayer player) {
        getState(player).loginComplete = true;
    }

    /**
     * Resets all per-player state on disconnect to prevent memory leaks.
     *
     * @param player the player who disconnected
     */
    public void onDisconnect(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.loginComplete = false;
        state.movementCountBeforeLogin = 0;
        state.duplicatePacketCount = 0;
        state.lastPacketHash = 0;
        state.packetBurst.clear();
    }

    /**
     * Checks if the given packet type is a movement-related packet.
     *
     * @param type the packet type to check
     * @return true if the packet is a movement packet (flying, position, rotation)
     */
    private boolean isMovementType(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }
}
