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

/**
 * Detects fast-heal exploits — health regeneration that occurs faster than
 * vanilla mechanics allow.
 *
 * <p>This check monitors health changes between consecutive attack packets and
 * cross-references them with movement packets. When health jumps by more than
 * {@value #HEALTH_SWING_THRESHOLD} hearts in a single attack cycle, a
 * {@link HealthSnapshot} is recorded. If multiple such snapshots accumulate
 * within a {@value #HEAL_WINDOW_MS} ms sliding window, the average heal is
 * compared against the player's maximum health.</p>
 *
 * <h3>Detection Strategy</h3>
 * <ol>
 *   <li><b>Frequency-based</b> — When {@value #MIN_SWINGS_FOR_FLAG} or more
 *       heal events occur within the window and the average heal exceeds
 *       {@value #HEALTH_RATIO_THRESHOLD} of max health, the buffer increments
 *       by 1.0 and flags at &gt; 3.0.</li>
 *   <li><b>Single-event spike</b> — A single health increase exceeding
 *       {@value #HEALTH_SWING_THRESHOLD} &times; 2 (= 6 hearts) increments the
 *       buffer by 0.5 and flags at &gt; 5.0. This catches instant full-heal
 *       exploits even when they are isolated.</li>
 * </ol>
 *
 * <h3>Version Constraints</h3>
 * <p>This check is only active for protocol versions 5–107 (roughly 1.6–8.2)
 * via {@code minVersion/maxVersion}, as regeneration mechanics differ
 * significantly outside this range.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Fast Heal A", stableKey = "windfall.combat.fastheal", decay = 0.02, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class FastHealCheck extends Check implements PacketCheck {

    /** Minimum health increase (in half-hearts) to qualify as a suspicious heal event. */
    private static final double HEALTH_SWING_THRESHOLD = 3.0;

    /** Sliding time window in milliseconds for accumulating heal snapshots. */
    private static final int HEAL_WINDOW_MS = 500;

    /** Minimum number of heal events within the window to trigger the frequency-based detection. */
    private static final int MIN_SWINGS_FOR_FLAG = 3;

    /**
     * Ratio of average heal to max health above which the heal rate is
     * considered inhuman (0.5 = 50% of max health per swing).
     */
    private static final double HEALTH_RATIO_THRESHOLD = 0.5;

    /** Per-player mutable state for tracking health changes and heal history. */
    private static final class PlayerState {
        final ArrayDeque<HealthSnapshot> recentHealth = new ArrayDeque<>();
        double lastHealth;
        boolean hasLastHealth;
        int swingCount;
    }

    /** Player state lookup keyed by UUID. */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily initialises the per-player state.
     *
     * @param player the player whose state is requested
     * @return the current {@link PlayerState}
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Evicts cached state when a player disconnects.
     *
     * @param uuid UUID of the departing player
     */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Routes incoming packets to the attack or movement handler.
     *
     * @param player the player associated with this packet
     * @param event  the raw packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleAttack(player);
        } else if (isMovementPacket(type)) {
            handleMovement(player);
        }
    }

    /** No outbound packets are relevant to this check. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Processes an attack packet by comparing the player's current health to
     * the previously recorded value.
     *
     * <p>If health increased by more than {@value #HEALTH_SWING_THRESHOLD}
     * hearts, a {@link HealthSnapshot} is added to the sliding window and
     * {@link #checkFastHeal} is invoked. The last-known health is always
     * updated at the end.</p>
     *
     * @param player the attacking player
     */
    private void handleAttack(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.swingCount++;

        double currentHealth = player.getPlayer().getHealth();

        if (state.hasLastHealth && state.lastHealth > 0) {
            double healthDelta = currentHealth - state.lastHealth;

            if (healthDelta > HEALTH_SWING_THRESHOLD) {
                long now = System.currentTimeMillis();
                state.recentHealth.addLast(new HealthSnapshot(currentHealth, healthDelta, now));

                /* Prune heal snapshots outside the sliding window. */
                while (!state.recentHealth.isEmpty() && now - state.recentHealth.peekFirst().timestamp > HEAL_WINDOW_MS) {
                    state.recentHealth.removeFirst();
                }

                checkFastHeal(player, state, healthDelta);
            }
        }

        state.lastHealth = currentHealth;
        state.hasLastHealth = true;
    }

    /**
     * Synchronises the last-known health from the live entity state on each
     * movement packet, catching heals that arrive between attack packets.
     *
     * @param player the player being tracked
     */
    private void handleMovement(WindfallPlayer player) {
        PlayerState state = getState(player);
        double currentHealth = player.getPlayer().getHealth();

        if (state.hasLastHealth && Math.abs(currentHealth - state.lastHealth) > 0.01) {
            state.lastHealth = currentHealth;
        }
    }

    /**
     * Evaluates the accumulated heal snapshots for suspicious patterns.
     *
     * <p><b>Frequency-based:</b> If the sliding window contains
     * {@value #MIN_SWINGS_FOR_FLAG} or more entries and the average heal
     * exceeds {@value #HEALTH_RATIO_THRESHOLD} &times; max health, the
     * buffer increments by 1.0 and flags at &gt; 3.0.</p>
     *
     * <p><b>Spike-based:</b> A single heal exceeding
     * {@code HEALTH_SWING_THRESHOLD &times; 2} (6 hearts) increments the
     * buffer by 0.5 and flags at &gt; 5.0.</p>
     *
     * @param player      the player being evaluated
     * @param state       the player's heal tracking state
     * @param healthDelta the most recent health increase
     */
    private void checkFastHeal(WindfallPlayer player, PlayerState state, double healthDelta) {
        int recentHeals = state.recentHealth.size();

        if (recentHeals >= MIN_SWINGS_FOR_FLAG) {
            /* Compute average heal magnitude across the sliding window. */
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
            /* Single extreme spike — likely an instant full-heal exploit. */
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    /**
     * Tests whether the given packet type is a player movement update.
     *
     * @param type the packet type to classify
     * @return {@code true} if the packet carries position or look data
     */
    private boolean isMovementPacket(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    /**
     * Immutable snapshot of a single heal event, capturing the health value,
     * the delta from the previous snapshot, and the timestamp.
     */
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
