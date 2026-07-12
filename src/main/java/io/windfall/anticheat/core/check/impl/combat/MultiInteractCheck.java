package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects multi-interact (multi-aura) attacks where a player hits more than
 * {@value MAX_ENTITIES_PER_TICK} distinct entities within a single server tick window.
 *
 * <p><b>How it works:</b> All attack packets arriving within {@value TICK_WINDOW_MS}ms
 * (approximately one server tick) are collected into a set of entity IDs. If the set
 * size exceeds {@value MAX_ENTITIES_PER_TICK}, a consecutive violation counter increments.
 * After {@code 3} consecutive violations, a flag is raised. The counter decrements on
 * clean ticks to allow brief false-positive spikes to recover without flagging.</p>
 *
 * <p><b>Why this works:</b> Vanilla clients send at most one attack per tick per entity.
 * Hitting multiple entities in a single tick requires multi-aura cheats that send
 * fabricated attack packets.</p>
 *
 * @see io.windfall.anticheat.core.check.Check
 */
@CheckData(name = "Multi Interact A", stableKey = "windfall.combat.multiinteract", decay = 0.01, setbackVl = 15)
public class MultiInteractCheck extends Check implements PacketCheck {

    /** Maximum number of distinct entities a player may attack within one tick window. */
    private static final int MAX_ENTITIES_PER_TICK = 2;
    /** Duration in milliseconds that defines a "tick" for grouping attacks (~1 server tick). */
    private static final long TICK_WINDOW_MS = 60;

    /** Per-player state tracking entity hits within the current tick window. */
    private static final class PlayerState {
        final Set<Integer> entitiesThisTick = new HashSet<>();
        long lastAttackTime;
        int consecutiveViolations;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or initializes the tracking state for the given player.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming attack packets. Tracks distinct entity IDs hit within each tick
     * window and flags when the count exceeds {@value MAX_ENTITIES_PER_TICK} for
     * {@code 3} consecutive violations.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        if (now - state.lastAttackTime > TICK_WINDOW_MS) {
            state.entitiesThisTick.clear();
        }
        state.lastAttackTime = now;

        int targetId = wrapper.getEntityId();
        state.entitiesThisTick.add(targetId);

        if (state.entitiesThisTick.size() > MAX_ENTITIES_PER_TICK) {
            state.consecutiveViolations++;
            if (state.consecutiveViolations >= 3) {
                flag(player);
                state.consecutiveViolations = 0;
            }
            resetBuffer(player);
        } else {
            state.consecutiveViolations = Math.max(0, state.consecutiveViolations - 1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
