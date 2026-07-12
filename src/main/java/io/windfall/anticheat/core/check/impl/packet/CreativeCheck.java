package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects creative inventory exploits in non-creative game modes and rapid creative slot manipulation.
 *
 * <p>Detection strategy:
 * <ul>
 *   <li><b>Mode validation</b> &mdash; immediately flags and kicks if a {@code CREATIVE_INVENTORY_ACTION}
 *       packet is received while the player is not in Creative mode (impossible without hacks)</li>
 *   <li><b>Rate limiting</b> &mdash; tracks creative inventory actions per tick window (50ms). If more than
 *       {@value #MAX_CREATIVE_ACTIONS_PER_TICK} actions occur in a single tick, buffer increases by 1.0.
 *       Flags with setback when buffer exceeds {@value #KICK_THRESHOLD}</li>
 * </ul>
 *
 * <p>This check is critical for preventing item duplication exploits that rely on creative inventory
 * packets in survival mode.
 *
 * <p>Setback at VL 5, decay disabled (0.0) — creative exploits are binary threats.
 *
 * @see CrashCheck for suspicious creative packet detection (buffer-based)
 * @see ExploitCheck for creative slot number validation
 */
@CheckData(name = "Creative A", stableKey = "windfall.packet.creative", decay = 0.0, setbackVl = 5)
public class CreativeCheck extends Check implements PacketCheck {

    /** Maximum creative inventory actions allowed per tick (50ms) before rate-limit flag */
    private static final int MAX_CREATIVE_ACTIONS_PER_TICK = 5;
    /** Buffer threshold that triggers a setback for repeated creative exploits */
    private static final int KICK_THRESHOLD = 10;

    /**
     * Per-player state tracking creative action rate.
     */
    private static final class PlayerState {
        /** Number of creative actions in the current tick window */
        int actionsThisTick;
        /** Start time of the current tick window in milliseconds */
        long lastTickStart;
        /** Total cumulative creative actions (informational/debugging) */
        int totalActions;
    }

    /** Thread-safe map of player UUID to their creative inventory state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player creative inventory state.
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
     * Processes creative inventory action packets. First validates game mode (must be Creative),
     * then enforces per-tick rate limiting.
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) return;

        if (player.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            flag(player);
            player.getPlayer().kickPlayer("[Windfall] Creative packet in non-creative mode");
            return;
        }

        PlayerState state = getState(player);
        /* Reset tick counter every 50ms (one server tick) to enforce per-tick rate limit */
        long now = System.currentTimeMillis();
        if (now - state.lastTickStart > 50) {
            state.actionsThisTick = 0;
            state.lastTickStart = now;
        }

        state.actionsThisTick++;
        state.totalActions++;

        if (state.actionsThisTick > MAX_CREATIVE_ACTIONS_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > KICK_THRESHOLD) {
                flagWithSetback(player);
                resetBuffer(player);
            }
        }
    }

    /** {@inheritDoc} No outgoing packet processing needed for creative checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
