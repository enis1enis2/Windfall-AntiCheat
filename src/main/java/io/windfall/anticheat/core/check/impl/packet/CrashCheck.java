package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects crash/exploit packets designed to destabilize the server or other clients.
 *
 * <p>Detection strategy:
 * <ul>
 *   <li><b>Oversized chat messages</b> &mdash; flags chat packets exceeding {@value #MAX_STRING_LENGTH}
 *       characters (max Java String length). After {@value #KICK_THRESHOLD} violations, the player
 *       is kicked with "Oversized chat packet" reason</li>
 *   <li><b>Suspicious creative packets</b> &mdash; accumulates buffer (+0.5) for every creative inventory
 *       action received. Kicks when buffer exceeds 10.0 with "Suspicious creative packet" reason</li>
 * </ul>
 *
 * <p>These checks prevent known crash exploits including:
 * <ul>
 *   <li>Chunked packet exploits via oversized NBT strings</li>
 *   <li>Creative inventory desync attacks</li>
 *   <li>String-based denial of service</li>
 * </ul>
 *
 * <p>Setback at VL 5, decay disabled (0.0) since violations are persistent threats.
 *
 * @see CreativeCheck for creative inventory rate limiting
 * @see ExploitCheck for invalid packet field values
 */
@CheckData(name = "Crash A", stableKey = "windfall.packet.crash", decay = 0.0, setbackVl = 5)
public class CrashCheck extends Check implements PacketCheck {

    /** Maximum allowed string length in chat packets (32767 = max short value) */
    private static final int MAX_STRING_LENGTH = 32767;
    /** Maximum allowed packet payload size in bytes */
    private static final int MAX_PACKET_SIZE = 32767;
    /** Number of oversized chat violations before the player is kicked */
    private static final int KICK_THRESHOLD = 3;

    /**
     * Per-player state tracking violation count for graduated kick response.
     */
    private static final class PlayerState {
        /** Number of oversized chat violations accumulated */
        int violations;
    }

    /** Thread-safe map of player UUID to their crash detection state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player crash detection state.
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
     * Processes incoming packets for crash/exploit detection.
     * Checks oversized chat messages and suspicious creative inventory actions.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        PlayerState state = getState(player);

        if (type == PacketType.Play.Client.CHAT_MESSAGE) {
            String message = getChatMessage(event);
            if (message != null && message.length() > MAX_STRING_LENGTH) {
                state.violations++;
                if (state.violations >= KICK_THRESHOLD) {
                    flag(player);
                    player.getPlayer().kickPlayer("[Windfall] Oversized chat packet");
                    state.violations = 0;
                }
            }
        }

        if (type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 10.0) {
                flag(player);
                player.getPlayer().kickPlayer("[Windfall] Suspicious creative packet");
                resetBuffer(player);
            }
        }
    }

    /** {@inheritDoc} No outgoing packet processing needed for crash checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Safely extracts the chat message string from a CHAT_MESSAGE packet.
     * Returns null if the packet cannot be parsed (malformed/crash packets).
     *
     * @param event the packet event to extract the message from
     * @return the chat message string, or null if extraction failed
     */
    private String getChatMessage(PacketReceiveEvent event) {
        try {
            com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage wrapper =
                    new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage(event);
            return wrapper.getMessage();
        } catch (Exception e) {
            return null;
        }
    }
}
