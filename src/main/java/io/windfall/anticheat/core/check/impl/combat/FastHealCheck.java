package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Fast Heal A", stableKey = "windfall.combat.fastheal", decay = 0.02, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class FastHealCheck extends Check implements PacketCheck {

    private static final double HEALTH_SWING_THRESHOLD = 3.0;
    private static final int HEAL_WINDOW_MS = 500;
    private static final int MIN_SWINGS_FOR_FLAG = 3;
    private static final double HEALTH_RATIO_THRESHOLD = 0.5;

    private static final class PlayerState {
        final ArrayDeque<HealthSnapshot> recentHealth = new ArrayDeque<>();
        double lastHealth;
        boolean hasLastHealth;
        int swingCount;
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
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleAttack(player);
        } else if (isMovementPacket(type)) {
            handleMovement(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void handleAttack(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.swingCount++;

        double currentHealth = player.getPlayer().getHealth();

        if (state.hasLastHealth && state.lastHealth > 0) {
            double healthDelta = currentHealth - state.lastHealth;

            if (healthDelta > HEALTH_SWING_THRESHOLD) {
                long now = System.currentTimeMillis();
                state.recentHealth.addLast(new HealthSnapshot(currentHealth, healthDelta, now));

                while (!state.recentHealth.isEmpty() && now - state.recentHealth.peekFirst().timestamp > HEAL_WINDOW_MS) {
                    state.recentHealth.removeFirst();
                }

                checkFastHeal(player, state, healthDelta);
            }
        }

        state.lastHealth = currentHealth;
        state.hasLastHealth = true;
    }

    private void handleMovement(WindfallPlayer player) {
        PlayerState state = getState(player);
        double currentHealth = player.getPlayer().getHealth();

        if (state.hasLastHealth && Math.abs(currentHealth - state.lastHealth) > 0.01) {
            state.lastHealth = currentHealth;
        }
    }

    private void checkFastHeal(WindfallPlayer player, PlayerState state, double healthDelta) {
        int recentHeals = state.recentHealth.size();

        if (recentHeals >= MIN_SWINGS_FOR_FLAG) {
            double avgHeal = state.recentHealth.stream()
                .mapToDouble(h -> h.healthDelta)
                .average()
                .orElse(0.0);

            double maxHealth = player.getPlayer().getMaxHealth();
            double healRatio = avgHeal / maxHealth;

            if (healRatio > HEALTH_RATIO_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else if (healthDelta > HEALTH_SWING_THRESHOLD * 2) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    private boolean isMovementPacket(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    private static final class HealthSnapshot {
        final double health;
        final double healthDelta;
        final long timestamp;

        HealthSnapshot(double health, double healthDelta, long timestamp) {
            this.health = health;
            this.healthDelta = healthDelta;
            this.timestamp = timestamp;
        }
    }
}
