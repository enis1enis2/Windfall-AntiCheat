package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

    // Elytra added in 1.9 (protocol 107) — check disabled on older versions
    @CheckData(name = "Elytra A", stableKey = "windfall.movement.elytra", decay = 0.01, setbackVl = 20, minVersion = 107, maxVersion = 999, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class ElytraCheck extends Check implements PacketCheck {

    private static final double ELYTRA_MAX_HORIZONTAL_SPEED = 1.5;
    private static final double ELYTRA_VERTICAL_TOLERANCE = 0.1;
    private static final double ELYTRA_MIN_DESCENT = -0.5;
    private static final int ELYTRA_HOVER_TICK_THRESHOLD = 40;
    private static final double ELYTRA_HOVER_DELTA = 0.005;
    private static final double ELYTRA_KICKBOOST_MAX = 0.5;

    private static final class PlayerState {
        int elytraHoverTicks;
        boolean wasGliding;
        double lastElytraDeltaY;
        int elytraTicks;
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
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);
        boolean isGliding = checkGliding(player);
        int protocol = player.getProtocolVersion();

        if (!VersionPhysics.hasElytra(protocol)) return;

        if (isGliding) {
            state.elytraTicks++;
            handleElytraMovement(player, state);
            state.wasGliding = true;
        } else {
            if (state.wasGliding && state.elytraTicks > 0) {
                handleElytraLanding(player, state);
            }
            state.elytraTicks = 0;
            state.elytraHoverTicks = 0;
            state.wasGliding = false;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleElytraMovement(WindfallPlayer player, PlayerState state) {
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double deltaY = player.getDeltaY();

        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed > ELYTRA_MAX_HORIZONTAL_SPEED) {
            double ratio = horizontalSpeed / ELYTRA_MAX_HORIZONTAL_SPEED;
            if (ratio > 2.0) {
                flag(player);
                resetBuffer(player);
                return;
            }
            increaseBuffer(player, 0.5 * (ratio - 1.0));
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        if (Math.abs(deltaY) < ELYTRA_HOVER_DELTA) {
            state.elytraHoverTicks++;
            if (state.elytraHoverTicks > ELYTRA_HOVER_TICK_THRESHOLD) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.elytraHoverTicks = 0;
                }
            }
        } else {
            state.elytraHoverTicks = Math.max(0, state.elytraHoverTicks - 1);
        }

        if (deltaY > 0 && deltaY > ELYTRA_KICKBOOST_MAX && player.isOnGround()) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        if (deltaY > 0 && !player.isOnGround() && state.elytraTicks > 5) {
            double expectedDescent = ELYTRA_MIN_DESCENT;
            if (deltaY > Math.abs(expectedDescent) + ELYTRA_VERTICAL_TOLERANCE) {
                increaseBuffer(player, 0.4);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastElytraDeltaY = deltaY;
    }

    private void handleElytraLanding(WindfallPlayer player, PlayerState state) {
        if (state.elytraTicks < 5) return;
    }

    private boolean checkGliding(WindfallPlayer player) {
        try {
            java.lang.reflect.Method m = player.getPlayer().getClass().getMethod("isGliding");
            return (Boolean) m.invoke(player.getPlayer());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
