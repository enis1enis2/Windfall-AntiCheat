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

@CheckData(name = "Hitboxes A", stableKey = "windfall.combat.hitboxes", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class HitboxesCheck extends Check implements PacketCheck {

    private static final double PLAYER_BOX_EXPANSION = 0.15;
    private static final int MIN_ATTACKS_PER_EVAL = 10;
    private static final double HIT_RATIO_FLAG_THRESHOLD = 0.8;

    private int attacksOnTarget;
    private int totalAttacks;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        totalAttacks++;

        // Check if attack is within expanded hitbox using eye direction vs entity position
        double eyeX = player.getX();
        double eyeY = player.getY() + getPlayerEyeHeight(player);
        double eyeZ = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        // Project look direction forward by max vanilla reach
        double lookX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 3.5;
        double lookY = -Math.sin(Math.toRadians(pitch)) * 3.5;
        double lookZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 3.5;

        // If the attack lands but the ray barely passes through the box, it's suspicious
        double maxReach = 3.5 + PLAYER_BOX_EXPANSION;
        double hitDistance = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);

        if (hitDistance < maxReach) {
            attacksOnTarget++;
        }

        if (totalAttacks >= MIN_ATTACKS_PER_EVAL) {
            double hitRatio = (double) attacksOnTarget / totalAttacks;
            if (hitRatio > HIT_RATIO_FLAG_THRESHOLD && totalAttacks > 20) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
            attacksOnTarget = 0;
            totalAttacks = 0;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double getPlayerEyeHeight(WindfallPlayer player) {
        if (player.isSneaking()) {
            return player.getProtocolVersion() >= 477 ? 1.27 : 1.54;
        }
        return 1.62;
    }
}
