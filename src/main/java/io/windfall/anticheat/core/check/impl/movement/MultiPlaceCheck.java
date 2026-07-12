package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects multiple block placements within a single server tick.
 *
 * <p>On vanilla clients, a maximum of one block placement can be sent per server tick because the
 * client processes physics and input in lock-step with the server tick. A client sending more than
 * {@value #MAX_PLACES_PER_TICK} placement packet in a single tick is a strong indicator of a
 * hacked client or multi-threaded placement exploit.
 *
 * <p><b>Detection algorithm:</b>
 * <ol>
 *   <li>Count placement packets per server tick (tracked via {@link WindfallPlayer#getTickCount()}).</li>
 *   <li>If the count exceeds {@value #MAX_PLACES_PER_TICK}, increase the buffer by 1.0.</li>
 *   <li>Flag when buffer exceeds {@value #BUFFER_THRESHOLD}, then reset.</li>
 * </ol>
 *
 * <p><b>Note:</b> Placements with a {@code null} face are ignored (air-place packets used by some
 * clients for inventory actions).
 *
 * @see InvalidPlaceCheck — companion check for occupied-block and self-intersection violations
 * @see ScaffoldCheck — companion check for block-placing speed over time
 */
@CheckData(name = "Multi Place", stableKey = "windfall.movement.multiplace", decay = 0.02, setbackVl = 10)
public class MultiPlaceCheck extends Check implements PacketCheck {

    /** Vanilla clients send at most 1 placement per server tick. */
    private static final int MAX_PLACES_PER_TICK = 1;

    /** Buffer must exceed this value before a flag is raised. */
    private static final int BUFFER_THRESHOLD = 2;

    /**
     * Per-player mutable state tracking placement counts within a server tick.
     */
    private static final class PlayerState {
        /** Number of placement packets received in the current server tick. */
        int placesThisTick;
        /** The last observed server tick number. */
        long lastTick;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily initialises the per-player state for this check.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT} packets with a
     * non-null face. Increments the per-tick placement counter and flags if it exceeds the limit.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        if (wrapper.getFace() == null) return;

        PlayerState state = getState(player);
        long currentTick = player.getTickCount();
        if (currentTick != state.lastTick) {
            state.placesThisTick = 0;
            state.lastTick = currentTick;
        }

        state.placesThisTick++;

        if (state.placesThisTick > MAX_PLACES_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
