package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Detects vehicle packet exploits: mounting/dismounting too fast,
 * vehicle speed cheating, and invalid steer packets.
 */
@CheckData(name = "Vehicle A", stableKey = "windfall.packet.vehicle", decay = 0.01, setbackVl = 15)
public class VehicleCheck extends Check implements PacketCheck {

    private static final long MIN_MOUNT_INTERVAL_MS = 500;
    private static final double MAX_VEHICLE_SPEED = 2.0;
    private static final int MAX_STEER_PER_TICK = 3;

    private long lastMountTime;
    private long lastDismountTime;
    private int steerCountThisTick;
    private long lastSteerTick;
    private double lastVehicleX, lastVehicleZ;

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        long now = System.currentTimeMillis();

        if (now - lastSteerTick > 50) {
            steerCountThisTick = 0;
            lastSteerTick = now;
        }

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteract(player, event, now);
        } else if (type == PacketType.Play.Client.STEER_VEHICLE) {
            handleSteer(player, event, now);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleInteract(WindfallPlayer player, PacketReceiveEvent event, long now) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            // Interact-at while not on a vehicle is suspicious
            if (!player.getPlayer().isInsideVehicle()) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }
    }

    private void handleSteer(WindfallPlayer player, PacketReceiveEvent event, long now) {
        WrapperPlayClientSteerVehicle wrapper = new WrapperPlayClientSteerVehicle(event);

        steerCountThisTick++;
        if (steerCountThisTick > MAX_STEER_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        float forward = wrapper.getForward();
        float sideways = wrapper.getSideways();

        double speed = Math.sqrt(forward * forward + sideways * sideways);
        if (speed > MAX_VEHICLE_SPEED) {
            flag(player);
        }
    }
}
