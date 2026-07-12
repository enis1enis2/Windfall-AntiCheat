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

@CheckData(name = "Multi Interact A", stableKey = "windfall.combat.multiinteract", decay = 0.01, setbackVl = 15)
public class MultiInteractCheck extends Check implements PacketCheck {

    private static final int MAX_ENTITIES_PER_TICK = 2;
    private static final long TICK_WINDOW_MS = 60;

    private static final class PlayerState {
        final Set<Integer> entitiesThisTick = new HashSet<>();
        long lastAttackTime;
        int consecutiveViolations;
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
