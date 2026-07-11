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

@CheckData(name = "Multi Interact A", stableKey = "windfall.combat.multiinteract", decay = 0.01, setbackVl = 15)
public class MultiInteractCheck extends Check implements PacketCheck {

    private static final int MAX_ENTITIES_PER_TICK = 2;
    private static final long TICK_WINDOW_MS = 60;

    private final Set<Integer> entitiesThisTick = new HashSet<>();
    private long lastAttackTime;
    private int consecutiveViolations;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackTime > TICK_WINDOW_MS) {
            entitiesThisTick.clear();
        }
        lastAttackTime = now;

        int targetId = wrapper.getEntityId();
        entitiesThisTick.add(targetId);

        if (entitiesThisTick.size() > MAX_ENTITIES_PER_TICK) {
            consecutiveViolations++;
            if (consecutiveViolations >= 3) {
                flag(player);
                consecutiveViolations = 0;
            }
            resetBuffer(player);
        } else {
            consecutiveViolations = Math.max(0, consecutiveViolations - 1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
