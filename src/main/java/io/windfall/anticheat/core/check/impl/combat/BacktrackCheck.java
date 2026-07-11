package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;

@CheckData(name = "Backtrack A", stableKey = "windfall.combat.backtrack", decay = 0.01, setbackVl = 15, compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class BacktrackCheck extends Check implements PacketCheck {

    private static final long MAX_BACKTRACK_DELAY_MS = 500;
    private static final double MIN_ATTACK_REACH = 2.5;
    private static final double IMPOSSIBLE_REACH = 6.0;
    private static final int MIN_SAMPLES = 10;

    private final ArrayDeque<Long> attackTimestamps = new ArrayDeque<>();
    private long lastMovementTimestamp;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            long now = System.currentTimeMillis();
            long delay = now - lastMovementTimestamp;
            attackTimestamps.addLast(delay);
            while (attackTimestamps.size() > 30) {
                attackTimestamps.removeFirst();
            }

            if (delay > MAX_BACKTRACK_DELAY_MS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        } else if (isMovementPacket(type)) {
            lastMovementTimestamp = System.currentTimeMillis();
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isMovementPacket(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
