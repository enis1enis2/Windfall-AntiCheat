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
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects backtracking — a cheat that delays or freezes position updates to
 * strike a target from a stale (usually closer) position.
 *
 * <p>The check measures the time gap between the most recent movement packet
 * and the next attack-entity packet. Legitimate players send movement updates
 * frequently (every tick, ~50 ms). A backtracker suppresses or delays movement
 * packets while retaining a valid attack, producing an abnormally large gap.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>On each movement packet ({@code PLAYER_FLYING}, {@code PLAYER_POSITION},
 *       {@code PLAYER_POSITION_AND_ROTATION}), record the current server time
 *       as {@code lastMovementTimestamp}.</li>
 *   <li>On each attack packet, compute {@code delay = now - lastMovementTimestamp}.</li>
 *   <li>If {@code delay > {@value #MAX_BACKTRACK_DELAY_MS}} ms, increment the
 *       buffer by 1.0 and flag when it exceeds 3.0.</li>
 * </ol>
 *
 * <h3>Version Sensitivity</h3>
 * <p>This check is marked {@link CompatFlag#VIAVERSION_SENSITIVE} because
 * ViaVersion packet reordering can artificially inflate or deflate timing gaps
 * on cross-version servers.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Backtrack A", stableKey = "windfall.combat.backtrack", decay = 0.01, setbackVl = 15, compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class BacktrackCheck extends Check implements PacketCheck {

    /**
     * Maximum acceptable delay (in ms) between the last movement packet and an
     * attack. Values above this are considered evidence of backtracking.
     */
    private static final long MAX_BACKTRACK_DELAY_MS = 500;

    /**
     * Minimum reach distance (in blocks) expected for a valid melee hit.
     * Currently unused in the heuristic but kept for future reach-based
     * cross-validation.
     */
    private static final double MIN_ATTACK_REACH = 2.5;

    /**
     * Reach distance that is mathematically impossible without teleportation.
     * Reserved for future hard-flag logic.
     */
    private static final double IMPOSSIBLE_REACH = 6.0;

    /**
     * Minimum number of delay samples before statistical analysis could be
     * applied. Currently unused; the per-sample threshold is the primary gate.
     */
    private static final int MIN_SAMPLES = 10;

    /** Maximum attack-delay samples retained in the deque. */
    private static final int MAX_DELAY_SAMPLES = 30;

    /** Buffer level at which a backtrack flag is triggered. */
    private static final double FLAG_BUFFER_THRESHOLD = 3.0;

    /** Per-player mutable state for tracking attack and movement timing. */
    private static final class PlayerState {
        final ArrayDeque<Long> attackTimestamps = new ArrayDeque<>();
        long lastMovementTimestamp;
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
     * Processes incoming packets to track movement timestamps and detect
     * suspicious attack-delay gaps.
     *
     * <p>Attack packets trigger the delay evaluation; movement packets reset
     * the baseline timestamp.</p>
     *
     * @param player the player associated with this packet
     * @param event  the raw packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        PlayerState state = getState(player);

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            long now = System.currentTimeMillis();
            /* Time gap since the last confirmed movement update. */
            long delay = now - state.lastMovementTimestamp;
            state.attackTimestamps.addLast(delay);

            /* Retain only the most recent samples to bound memory. */
            while (state.attackTimestamps.size() > MAX_DELAY_SAMPLES) {
                state.attackTimestamps.removeFirst();
            }

            if (delay > MAX_BACKTRACK_DELAY_MS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > FLAG_BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        } else if (isMovementPacket(type)) {
            state.lastMovementTimestamp = System.currentTimeMillis();
        }
    }

    /** No outbound packets are relevant to this check. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
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
}
