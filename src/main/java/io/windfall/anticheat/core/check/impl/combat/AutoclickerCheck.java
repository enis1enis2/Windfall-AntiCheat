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
import java.util.Iterator;
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
     * the consecutive inter-click intervals inside the window:</p>
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

    /**
     * Mean interval (in ms) below which a sustained rate is physically impossible for a human.
     *
     * <p>25 ms is 40 CPS. Legacy double-click exploits peak around 20 CPS (50 ms) and are
     * already bounded by {@link #HIGH_CPS_LEGACY}, so this floor only sees rates that no
     * legitimate click path can produce on any protocol version.</p>
     */
    private static final double IMPOSSIBLE_MEAN_INTERVAL_MS = 25.0;

    /** Buffer level at which the over-maximum-rate branch escalates to a flag. */
    private static final double HIGH_RATE_FLAG_BUFFER = 10.0;

    /** Per-player mutable state holding the sliding window of click timestamps. */
    static final class PlayerState {
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

        /* Below the version bound: simply slow, not suspicious. */
        if (cps < lowCPS) {
            decreaseBuffer(player, 0.2);
            return;
        }

        /*
         * Above the version bound: do not discard outright. Attack cooldown does not stop a
         * client from sending higher-rate attack inputs, so treat this as its own weak signal.
         * Only escalate when the sustained mean interval is below what a human can physically
         * produce, which keeps network/tick batching (a handful of clicks in one ms) out of
         * scope because batching raises variance rather than lowering the mean.
         */
        if (cps > highCPS) {
            double meanMs = meanInterval(state);
            if (meanMs < IMPOSSIBLE_MEAN_INTERVAL_MS) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > HIGH_RATE_FLAG_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.2);
            }
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
     * Computes the population standard deviation of the consecutive inter-click
     * intervals inside the sliding window.
     *
     * <p>Variance is measured across adjacent timestamp differences, not across offsets
     * from the first timestamp. Offsets grow monotonically with sample index, so a human
     * clicking at a steady 125 ms (8 CPS) yields offsets {@code 125, 250, ... 2375} and a
     * deviation of roughly 685 ms — indistinguishable from jitter to this check — while the
     * real intervals are all exactly 125 ms. Any threshold below 685 ms was therefore
     * unreachable for regular clicking.</p>
     *
     * <p>Population formula: {@code &sigma; = sqrt(&Sigma;(x - &mu;)^2 / N)}.</p>
     *
     * @param state the player state containing the click timestamp deque
     * @return the standard deviation in milliseconds, or {@link Double#MAX_VALUE}
     *         if there are fewer than 2 timestamps
     */
    static double calculateStdDev(PlayerState state) {
        if (state == null || state.clickTimestamps.size() < 2) return Double.MAX_VALUE;

        /* N timestamps yield N-1 intervals. The leading element starts the chain and has no
         * interval of its own, so it is stepped over instead of being folded in as a 0 ms
         * gap — counting it drags the mean down and inflates the deviation of a perfectly
         * regular pattern by one full interval width. */
        int intervals = state.clickTimestamps.size() - 1;
        Iterator<Long> cursor = state.clickTimestamps.iterator();
        long first = cursor.next();
        long previous = first;

        double mean = 0;
        while (cursor.hasNext()) {
            long ts = cursor.next();
            mean += ts - previous;
            previous = ts;
        }
        mean /= intervals;

        double variance = 0;
        cursor = state.clickTimestamps.iterator();
        previous = cursor.next();
        while (cursor.hasNext()) {
            long ts = cursor.next();
            double diff = (ts - previous) - mean;
            variance += diff * diff;
            previous = ts;
        }
        variance /= intervals;

        return Math.sqrt(variance);
    }

    /**
     * Mean inter-click interval in milliseconds for the current window.
     *
     * @param state the player state containing the click timestamp deque
     * @return the mean interval, or {@link Double#MAX_VALUE} when fewer than 2 samples exist
     */
    static double meanInterval(PlayerState state) {
        if (state == null || state.clickTimestamps.size() < 2) return Double.MAX_VALUE;
        long span = state.clickTimestamps.peekLast() - state.clickTimestamps.peekFirst();
        return (double) span / (state.clickTimestamps.size() - 1);
    }
}
