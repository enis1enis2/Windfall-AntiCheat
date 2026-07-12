package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects automated or inhuman click-rate patterns (autoclickers, macros, bots).
 *
 * <p>This check records attack-entity packet timestamps in a sliding time window
 * of {@value #CLICK_WINDOW_MS} milliseconds and requires at least
 * {@value #MIN_CLICKS_FOR_EVAL} samples before evaluating.</p>
 *
 * <h3>Detection Strategy</h3>
 * <p>The core insight is that human click intervals have significant natural
 * variance (muscle fatigue, reaction time jitter), whereas autoclickers produce
 * very consistent timing. The check computes the <em>standard deviation</em> of
 * click-interval offsets from the first timestamp in the window:</p>
 *
 * <ul>
 *   <li><b>Strong autoclicker signal</b> — standard deviation below
 *       {@value #STD_DEV_AUTOCLICKER_THRESHOLD} ms <em>and</em> CPS above the
 *       version-specific lower bound. Buffer increases by 1.5; flags at &gt; 4.0.</li>
 *   <li><b>Moderate signal</b> — standard deviation below
 *       {@value #MIN_HUMAN_STD_DEV} ms (higher threshold catches near-human
 *       macros). Buffer increases by 0.5; flags at &gt; 6.0.</li>
 *   <li><b>Human-like</b> — standard deviation &ge; 15 ms. Buffer decays by 0.2
 *       per evaluation.</li>
 * </ul>
 *
 * <h3>Version-Aware CPS Bounds</h3>
 * <p>Different protocol versions have different maximum achievable CPS:</p>
 * <ul>
 *   <li><b>Legacy</b> (1.7–1.8): 6–20 CPS — double-click exploits inflate the
 *       ceiling.</li>
 *   <li><b>Modern</b> (1.9+): 1–8 CPS — attack-cooldown limits reduce the
 *       maximum.</li>
 *   <li><b>Bedrock</b>: 1 CPS to a configurable upper bound ({@code bedrockCpsLimit}).</li>
 * </ul>
 * <p>CPS values outside the version range are discarded as they likely indicate
 * desync or a different mechanism entirely.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Autoclicker A", stableKey = "windfall.combat.autoclicker", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.5)
public class AutoclickerCheck extends Check implements PacketCheck {

    /** Minimum click count before the sliding window is evaluated. */
    private static final int MIN_CLICKS_FOR_EVAL = 20;

    /** Sliding time window in milliseconds over which clicks are sampled. */
    private static final long CLICK_WINDOW_MS = 3000;

    /** Lower CPS bound for legacy (1.7–1.8) clients where double-click is possible. */
    private static final double LOW_CPS_LEGACY = 6.0;

    /** Upper CPS bound for legacy clients. */
    private static final double HIGH_CPS_LEGACY = 20.0;

    /** Lower CPS bound for modern (1.9+) clients subject to attack cooldown. */
    private static final double LOW_CPS_MODERN = 1.0;

    /** Upper CPS bound for modern clients. */
    private static final double HIGH_CPS_MODERN = 8.0;

    /** Standard deviation (in ms) below which the click pattern is considered highly robotic. */
    private static final double STD_DEV_AUTOCLICKER_THRESHOLD = 3.0;

    /** Standard deviation (in ms) below which the pattern is considered moderately suspicious. */
    private static final double MIN_HUMAN_STD_DEV = 15.0;

    /** Per-player mutable state holding the sliding window of click timestamps. */
    private static final class PlayerState {
        final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();
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
     * Processes incoming attack-entity packets, records timestamps, and
     * evaluates the click-interval distribution.
     *
     * <p>Only {@code INTERACT_ENTITY} packets with an {@code ATTACK} action
     * are considered. The sliding window is pruned of entries older than
     * {@value #CLICK_WINDOW_MS} ms before evaluation.</p>
     *
     * @param player the player who performed the attack
     * @param event  the raw packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.clickTimestamps.addLast(now);

        /* Prune timestamps outside the sliding window. */
        while (!state.clickTimestamps.isEmpty() && now - state.clickTimestamps.peekFirst() > CLICK_WINDOW_MS) {
            state.clickTimestamps.removeFirst();
        }

        if (state.clickTimestamps.size() < MIN_CLICKS_FOR_EVAL) return;

        /* Determine version-specific CPS bounds. */
        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);

        double lowCPS, highCPS;

        if (player.isBedrock()) {
            WindfallConfig cfg = WindfallPlugin.getInstance().getWindfallConfig();
            lowCPS = LOW_CPS_MODERN;
            highCPS = cfg.getBedrockCpsLimit();
        } else if (bracket == VersionBracket.LEGACY) {
            lowCPS = LOW_CPS_LEGACY;
            highCPS = HIGH_CPS_LEGACY;
        } else {
            lowCPS = LOW_CPS_MODERN;
            highCPS = HIGH_CPS_MODERN;
        }

        /* CPS = sample count / window duration in seconds. */
        double cps = state.clickTimestamps.size() / (CLICK_WINDOW_MS / 1000.0);
        if (cps < lowCPS || cps > highCPS) {
            decreaseBuffer(player, 0.2);
            return;
        }

        double stdDev = calculateStdDev(state);

        if (stdDev < STD_DEV_AUTOCLICKER_THRESHOLD && cps > lowCPS) {
            /* Very low variance — strong autoclicker signal. */
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (stdDev < MIN_HUMAN_STD_DEV) {
            /* Moderate variance — possibly a macro with slight randomisation. */
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 6.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            /* Human-like variance — decay buffer. */
            decreaseBuffer(player, 0.2);
        }
    }

    /** No outbound packets are relevant to this check. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Computes the sample standard deviation of click-interval offsets from the
     * first timestamp in the sliding window.
     *
     * <p>The method converts absolute timestamps to relative offsets from the
     * earliest entry, then calculates the standard deviation using the
     * population formula ({@code &sigma; = sqrt(&Sigma;(x - &mu;)^2 / N)}).</p>
     *
     * @param state the player state containing the click timestamp deque
     * @return the standard deviation in milliseconds, or {@link Double#MAX_VALUE}
     *         if there are fewer than 2 samples
     */
    private double calculateStdDev(PlayerState state) {
        if (state.clickTimestamps.size() < 2) return Double.MAX_VALUE;

        long first = state.clickTimestamps.peekFirst();
        double mean = 0;
        int count = 0;
        for (Long ts : state.clickTimestamps) {
            if (ts == first) continue;
            mean += ts - first;
            count++;
        }
        if (count == 0) return Double.MAX_VALUE;
        mean /= count;

        double variance = 0;
        for (Long ts : state.clickTimestamps) {
            if (ts == first) continue;
            double diff = (ts - first) - mean;
            variance += diff * diff;
        }
        variance /= count;

        return Math.sqrt(variance);
    }
}
