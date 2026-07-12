package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects critical-hit exploits — attacks executed while airborne without
 * legitimate jump motion.
 *
 * <p>In vanilla Minecraft, a critical hit requires the player to be falling
 * (i.e. {@code deltaY} between {@value #MIN_DELTA_Y_CRITICAL} and
 * {@value #MAX_DELTA_Y_CRITICAL} blocks/tick). Cheat clients can fake critical
 * motion by sending fabricated upward velocity while remaining on the ground,
 * or by attacking with zero or near-zero vertical delta.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>On each movement packet, track whether the player is on the ground. If
 *       on ground, reset {@code attacksSinceGround}.</li>
 *   <li>On each attack packet, if the player is airborne and the vertical delta
 *       ({@code deltaY}) does <em>not</em> fall within the valid critical range
 *       but is non-negative ({@code &ge; -0.01}), increment
 *       {@code consecutiveInvalid}.</li>
 *   <li>After {@code consecutiveInvalid &ge; 4}, flag with setback and reset
 *       the counter. Grounded attacks decrement the counter.</li>
 * </ol>
 *
 * <h3>Key Thresholds</h3>
 * <ul>
 *   <li>{@value #MIN_DELTA_Y_CRITICAL} — minimum downward velocity for a
 *       legitimate critical hit (vanilla: 0.11 blocks/tick).</li>
 *   <li>{@value #MAX_DELTA_Y_CRITICAL} — maximum downward velocity before the
 *       player is falling too fast for a critical.</li>
 *   <li>4 consecutive invalid attacks before flagging — reduces false positives
 *       from occasional desync.</li>
 * </ul>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Criticals A", stableKey = "windfall.combat.criticals", decay = 0.01, setbackVl = 10)
public class CriticalsCheck extends Check implements PacketCheck {

    /**
     * Minimum vertical velocity (blocks/tick, positive = falling) required for
     * a vanilla critical hit to register.
     */
    private static final double MIN_DELTA_Y_CRITICAL = 0.11;

    /**
     * Maximum vertical velocity for a critical hit. Above this the player is
     * falling too fast and criticals no longer apply.
     */
    private static final double MAX_DELTA_Y_CRITICAL = 0.5;

    /** Consecutive invalid critical attacks required before a flag is triggered. */
    private static final int INVALID_THRESHOLD = 4;

    /** Per-player mutable state for tracking critical-hit validity. */
    private static final class PlayerState {
        int attacksSinceGround;
        int consecutiveInvalid;
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
     * Routes incoming packets to the appropriate handler based on packet type.
     *
     * @param player the player associated with this packet
     * @param event  the raw packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handleAttack(player);
            }
        } else if (isMovementPacket(type)) {
            handleMovement(player);
        }
    }

    /** No outbound packets are relevant to this check. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Evaluates an attack for valid critical-hit motion.
     *
     * <p>Grounded attacks are ignored. Airborne attacks with a vertical delta
     * outside the valid critical range ({@link #MIN_DELTA_Y_CRITICAL} –
     * {@link #MAX_DELTA_Y_CRITICAL}) but non-negative increment the
     * {@code consecutiveInvalid} counter. After
     * {@value #INVALID_THRESHOLD} consecutive violations the player is flagged
     * with a setback.</p>
     *
     * @param player the attacking player
     */
    private void handleAttack(WindfallPlayer player) {
        PlayerState state = getState(player);

        /* Grounded attacks are always valid for criticals — reset the counter. */
        if (player.isOnGround()) {
            state.consecutiveInvalid = Math.max(0, state.consecutiveInvalid - 1);
            return;
        }

        double deltaY = player.getDeltaY();
        /* Check if deltaY falls within the legitimate critical-hit window. */
        boolean validCritMotion = deltaY > MIN_DELTA_Y_CRITICAL && deltaY < MAX_DELTA_Y_CRITICAL;

        if (!validCritMotion && deltaY >= -0.01) {
            state.consecutiveInvalid++;
            if (state.consecutiveInvalid >= INVALID_THRESHOLD) {
                flagWithSetback(player);
                state.consecutiveInvalid = 0;
            }
        } else {
            state.consecutiveInvalid = Math.max(0, state.consecutiveInvalid - 1);
        }
    }

    /**
     * Tracks ground-contact state by monitoring movement packets.
     * Resets the air-tick counter when the player touches the ground.
     *
     * @param player the player whose movement is being tracked
     */
    private void handleMovement(WindfallPlayer player) {
        PlayerState state = getState(player);
        if (player.isOnGround()) {
            state.attacksSinceGround = 0;
        } else {
            state.attacksSinceGround++;
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
}
