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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects vehicle-related exploits including speed hacking and invalid vehicle interactions.
 *
 * <p>Detection strategy:
 * <ul>
 *   <li><b>Invalid INTERACT_AT</b> &mdash; flags INTERACT_AT packets sent when the player is not
 *       inside a vehicle (buffer +0.5, flags at buffer >2.0). Detects vehicle desync exploits.</li>
 *   <li><b>Steer rate limiting</b> &mdash; counts STEER_VEHICLE packets per tick (50ms). Flags if more than
 *       {@value #MAX_STEER_PER_TICK} steer packets per tick (buffer +1.0, flags at buffer >3.0).
 *       Normal clients send at most 1-2 steer packets per tick.</li>
 *   <li><b>Vehicle speed check</b> &mdash; computes the magnitude of the forward+sideways steering input.
 *       If the combined speed exceeds {@value #MAX_VEHICLE_SPEED}, flags immediately.
 *       Normal vehicles have a maximum steering input of ~1.0.</li>
 * </ul>
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value #MAX_VEHICLE_SPEED} &mdash; maximum combined forward+sideways steering input magnitude</li>
 *   <li>{@value #MAX_STEER_PER_TICK} &mdash; maximum steer packets per tick</li>
 * </ul>
 *
 * <p>Setback at VL 15, decay 0.01/tick.
 *
 * @see BadPacketsCheck for general packet field validation
 */
@CheckData(name = "Vehicle A", stableKey = "windfall.packet.vehicle", decay = 0.01, setbackVl = 15)
public class VehicleCheck extends Check implements PacketCheck {

    /** Maximum combined forward+sideways steering input magnitude before flag (sqrt(2) ≈ 1.41 for normal max) */
    private static final double MAX_VEHICLE_SPEED = 2.0;
    /** Maximum STEER_VEHICLE packets allowed per tick before rate-limit flag */
    private static final int MAX_STEER_PER_TICK = 3;

    /**
     * Per-player state tracking vehicle steer rate.
     */
    private static final class PlayerState {
        /** Number of STEER_VEHICLE packets in the current tick window */
        int steerCountThisTick;
        /** Start time of the current tick window in milliseconds */
        long lastSteerTick;
    }

    /** Thread-safe map of player UUID to their vehicle check state */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or creates per-player vehicle check state.
     *
     * @param player the player to get state for
     * @return the player's state
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** {@inheritDoc} Clears player state to prevent memory leaks on disconnect */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming packets for vehicle interaction and steering validation.
     * Handles INTERACT_ENTITY (for vehicle interaction checks) and STEER_VEHICLE (for
     * speed and rate limiting).
     *
     * @param player the player who sent the packet
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        /* Reset steer counter every tick (50ms) */
        long now = System.currentTimeMillis();
        PlayerState state = getState(player);

        if (now - state.lastSteerTick > 50) {
            state.steerCountThisTick = 0;
            state.lastSteerTick = now;
        }

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteract(player, event);
        } else if (type == PacketType.Play.Client.STEER_VEHICLE) {
            handleSteer(player, event, state);
        }
    }

    /** {@inheritDoc} No outgoing packet processing needed for vehicle checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Validates INTERACT_AT packets to detect vehicle interaction when not mounted.
     * A player sending INTERACT_AT while not inside a vehicle indicates a desync exploit.
     *
     * @param player the player who sent the packet
     * @param event  the packet event
     */
    private void handleInteract(WindfallPlayer player, PacketReceiveEvent event) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            if (!player.getPlayer().isInsideVehicle()) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }
    }

    /**
     * Processes STEER_VEHICLE packets for rate limiting and speed validation.
     * Computes steering input magnitude: sqrt(forward² + sideways²).
     * Normal max input is ~1.0; values above {@value #MAX_VEHICLE_SPEED} indicate speed hacks.
     *
     * @param player the player who sent the packet
     * @param event  the packet event
     * @param state  the player's check state for rate counting
     */
    private void handleSteer(WindfallPlayer player, PacketReceiveEvent event, PlayerState state) {
        WrapperPlayClientSteerVehicle wrapper = new WrapperPlayClientSteerVehicle(event);

        state.steerCountThisTick++;
        if (state.steerCountThisTick > MAX_STEER_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        /* Steering input magnitude: forward and sideways are [-1.0, 1.0] in vanilla.
         * A value above 2.0 is physically impossible without packet manipulation */
        float forward = wrapper.getForward();
        float sideways = wrapper.getSideways();

        double speed = Math.sqrt(forward * forward + sideways * sideways);
        if (speed > MAX_VEHICLE_SPEED) {
            flag(player);
        }
    }
}
