package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players who initiate more than one block break (START_DIGGING)
 * within a single server tick. In vanilla Minecraft, a player can only
 * break one block per tick; sending multiple start-dig packets in the
 * same tick is a strong indicator of a hacked client (e.g., Nuker, FastBreak).
 *
 * <p><b>Algorithm:</b> Counts START_DIGGING packets per server tick using
 * the player's tick counter. When the count exceeds {@link #MAX_BREAKS_PER_TICK}
 * (1), the buffer rises by 1.0. Two consecutive violations (buffer &gt; 2.0)
 * trigger a flag.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Multi Break", stableKey = "windfall.movement.multibreak", decay = 0.02, setbackVl = 10)
public class MultiBreakCheck extends Check implements PacketCheck {

    /** Maximum number of START_DIGGING packets allowed per server tick in vanilla. */
    private static final int MAX_BREAKS_PER_TICK = 1;

    /** Buffer level at which the player is flagged for multi-breaking. */
    private static final int BUFFER_THRESHOLD = 2;

    /** Per-player state tracking the break count within the current tick. */
    private static final class PlayerState {
        /** Number of START_DIGGING packets received in the current tick. */
        int breaksThisTick;
        /** The server tick counter value when the current counting window started. */
        long lastTick;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Counts START_DIGGING packets per tick. Resets the counter on new ticks
     * and flags when more than {@link #MAX_BREAKS_PER_TICK} breaks occur in
     * a single tick.
     *
     * @param player the player associated with this packet
     * @param event  the incoming digging packet
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        PlayerState state = getState(player);
        long currentTick = player.getTickCount();
        /** Reset the counter when the server tick advances. */
        if (currentTick != state.lastTick) {
            state.breaksThisTick = 0;
            state.lastTick = currentTick;
        }

        state.breaksThisTick++;

        if (state.breaksThisTick > MAX_BREAKS_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
