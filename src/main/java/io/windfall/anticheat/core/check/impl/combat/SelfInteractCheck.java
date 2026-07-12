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

/**
 * Detects self-interaction packets where a player sends an attack packet targeting themselves.
 *
 * <p><b>Why this matters:</b> Self-interaction packets are impossible to generate from the
 * vanilla client. They are only produced by hacked clients or packet manipulation tools,
 * typically to trigger unintended server-side behavior (e.g., hit registration exploits,
 * kill-self macros, or packet injection testing).</p>
 *
 * <p><b>Response:</b> Upon detection, the player is immediately flagged and kicked with
 * the message {@code "[Windfall] Self-interaction detected"}. The check has zero decay
 * ({@code decay = 0.0}) as any occurrence is a definitive violation.</p>
 */
@CheckData(name = "Self Interact A", stableKey = "windfall.combat.selfinteract", decay = 0.0, setbackVl = 5)
public class SelfInteractCheck extends Check implements PacketCheck {

    /**
     * Inspects attack packets and compares the target entity ID to the player's own
     * entity ID. If they match, the player is flagged and kicked.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int targetId = wrapper.getEntityId();
        int selfId = player.getPlayer().getEntityId();

        if (targetId == selfId) {
            flag(player);
            player.getPlayer().kickPlayer("[Windfall] Self-interaction detected");
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
