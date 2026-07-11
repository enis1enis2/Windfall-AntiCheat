package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Phase A", stableKey = "windfall.movement.phase", decay = 0.01, setbackVl = 20)
public class PhaseCheck extends Check implements PacketCheck {

    private static final double MAX_BLOCK_CLIP = 0.1;
    private static final double MAX_VELOCITY_INSIDE_BLOCK = 0.01;
    private static final double CLIP_CHECK_DISTANCE = 0.3;
    private static final int MIN_CLIPPING_TICKS = 3;

    private int clippingTicks;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double deltaY = player.getDeltaY();

        try {
            // Check blocks at feet, eye, and head level for collisions
            org.bukkit.Location loc = new org.bukkit.Location(player.getPlayer().getWorld(), x, y, z);
            org.bukkit.block.Block feetBlock = loc.getBlock();
            org.bukkit.block.Block headBlock = loc.clone().add(0, 1.6, 0).getBlock();
            org.bukkit.block.Block belowBlock = loc.clone().subtract(0, 0.1, 0).getBlock();

            boolean feetInside = feetBlock.getType().isSolid();
            boolean headInside = headBlock.getType().isSolid();
            boolean belowInside = belowBlock.getType().isSolid();

            if (feetInside || headInside) {
                clippingTicks++;
                if (clippingTicks >= MIN_CLIPPING_TICKS) {
                    double speed = Math.sqrt(player.getDeltaX() * player.getDeltaX()
                            + player.getDeltaZ() * player.getDeltaZ());
                    if (speed > MAX_VELOCITY_INSIDE_BLOCK || Math.abs(deltaY) > MAX_VELOCITY_INSIDE_BLOCK) {
                        increaseBuffer(player, 1.5);
                        if (getBuffer(player) > 3.0) {
                            flag(player);
                            resetBuffer(player);
                            clippingTicks = 0;
                        }
                    }
                }
            } else {
                clippingTicks = Math.max(0, clippingTicks - 1);
                decreaseBuffer(player, 0.2);
            }
        } catch (Exception e) {
            // World access might fail on edge cases — skip silently
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
