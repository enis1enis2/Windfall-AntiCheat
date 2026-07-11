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

@CheckData(name = "Fast Heal A", stableKey = "windfall.combat.fastheal", decay = 0.02, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class FastHealCheck extends Check implements PacketCheck {

    // 3 HP per swing is beyond normal regeneration — threshold for suspicious healing
    private static final double HEALTH_SWING_THRESHOLD = 3.0;
    private static final int HEAL_WINDOW_MS = 500;
    private static final int MIN_SWINGS_FOR_FLAG = 3;
    private static final double HEALTH_RATIO_THRESHOLD = 0.5;

    private final ArrayDeque<HealthSnapshot> recentHealth = new ArrayDeque<>();

    private double lastHealth;
    private boolean hasLastHealth;
    private int swingCount;

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
        swingCount++;

        double currentHealth = player.getPlayer().getHealth();

        if (hasLastHealth && lastHealth > 0) {
            double healthDelta = currentHealth - lastHealth;

            if (healthDelta > HEALTH_SWING_THRESHOLD) {
                long now = System.currentTimeMillis();
                recentHealth.addLast(new HealthSnapshot(currentHealth, healthDelta, now));

                while (!recentHealth.isEmpty() && now - recentHealth.peekFirst().timestamp > HEAL_WINDOW_MS) {
                    recentHealth.removeFirst();
                }

                checkFastHeal(player, healthDelta);
            }
        }

        lastHealth = currentHealth;
        hasLastHealth = true;
    }

    private void handleMovement(WindfallPlayer player) {
        double currentHealth = player.getPlayer().getHealth();

        if (hasLastHealth && Math.abs(currentHealth - lastHealth) > 0.01) {
            lastHealth = currentHealth;
        }
    }

    private void checkFastHeal(WindfallPlayer player, double healthDelta) {
        int recentHeals = recentHealth.size();

        if (recentHeals >= MIN_SWINGS_FOR_FLAG) {
            double avgHeal = recentHealth.stream()
                .mapToDouble(h -> h.healthDelta)
                .average()
                .orElse(0.0);

            double maxHealth = player.getPlayer().getMaxHealth();
            // Compare against max health to account for Health Boost effect scaling
            double healRatio = avgHeal / maxHealth;

            if (healRatio > HEALTH_RATIO_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        // Single massive heals bypass the multi-swing window check
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
