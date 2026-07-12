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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Hitboxes A", stableKey = "windfall.combat.hitboxes", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class HitboxesCheck extends Check implements PacketCheck {

    private static final double PLAYER_BOX_EXPANSION = 0.15;
    private static final int MIN_ATTACKS_PER_EVAL = 10;
    private static final double HIT_RATIO_FLAG_THRESHOLD = 0.8;

    private static final class PlayerState {
        int attacksOnTarget;
        int totalAttacks;
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
        state.totalAttacks++;

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight();
        double eyeZ = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        double lookX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 3.5;
        double lookY = -Math.sin(Math.toRadians(pitch)) * 3.5;
        double lookZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 3.5;

        double maxReach = 3.5 + PLAYER_BOX_EXPANSION;
        double hitDistance = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);

        if (hitDistance < maxReach) {
            state.attacksOnTarget++;
        }

        if (state.totalAttacks >= MIN_ATTACKS_PER_EVAL) {
            double hitRatio = (double) state.attacksOnTarget / state.totalAttacks;
            if (hitRatio > HIT_RATIO_FLAG_THRESHOLD && state.totalAttacks > 20) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
            state.attacksOnTarget = 0;
            state.totalAttacks = 0;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
