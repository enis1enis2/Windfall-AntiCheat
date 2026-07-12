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

/**
 * Detects illegal elytra flight behaviour such as excessive horizontal speed,
 * sustained hovering (zero vertical movement), impossible upward boosts, and
 * ascent without a valid ground kick-off.
 *
 * <p><b>Variants checked:</b></p>
 * <ul>
 *   <li><b>Horizontal speed:</b> Flagged when horizontal speed exceeds
 *       {@link #ELYTRA_MAX_HORIZONTAL_SPEED} blocks/tick. A ratio above 2.0
 *       is an instant flag; otherwise the buffer scales proportionally.</li>
 *   <li><b>Hover:</b> When vertical delta stays below {@link #ELYTRA_HOVER_DELTA}
 *       for more than {@link #ELYTRA_HOVER_TICK_THRESHOLD} ticks, the player is
 *       assumed to be levitating.</li>
 *   <li><b>Kick-boost:</b> A vertical boost greater than {@link #ELYTRA_KICKBOOST_MAX}
 *       while on the ground indicates a spoofed launch.</li>
 *   <li><b>Ascent:</b> Upward movement after 5+ elytra ticks that exceeds the
 *       expected minimum descent + tolerance is flagged as unnatural ascent.</li>
 * </ul>
 *
 * <p>This check is only active on protocol version 107+ (1.9+) where elytra exist.</p>
 *
 * @see VersionPhysics#hasElytra(int)
 * @see CompatFlag#RELAX_ON_MISMATCH
 * @see Check
 * @see PacketCheck
 */
    // Elytra added in 1.9 (protocol 107) — check disabled on older versions
    @CheckData(name = "Elytra A", stableKey = "windfall.movement.elytra", decay = 0.01, setbackVl = 20, minVersion = 107, maxVersion = 999, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class ElytraCheck extends Check implements PacketCheck {

    /** Maximum horizontal speed (blocks/tick) during elytra flight in vanilla. */
    private static final double ELYTRA_MAX_HORIZONTAL_SPEED = 1.5;

    /** Tolerance added to the minimum-descent threshold when checking for impossible ascent. */
    private static final double ELYTRA_VERTICAL_TOLERANCE = 0.1;

    /** Minimum expected downward velocity (negative Y) while gliding in vanilla. */
    private static final double ELYTRA_MIN_DESCENT = -0.5;

    /** Number of ticks with near-zero vertical movement before a hover violation is raised. */
    private static final int ELYTRA_HOVER_TICK_THRESHOLD = 40;

    /** Vertical delta below which the player is considered to be hovering. */
    private static final double ELYTRA_HOVER_DELTA = 0.005;

    /** Maximum allowed vertical boost when the player is on the ground (kick-off). */
    private static final double ELYTRA_KICKBOOST_MAX = 0.5;

    /** Per-player elytra flight state. */
    private static final class PlayerState {
        /** Consecutive ticks with near-zero vertical delta (hover detection). */
        int elytraHoverTicks;
        /** Whether the player was gliding on the previous tick. */
        boolean wasGliding;
        /** Previous tick's Y delta, used for trend analysis. */
        double lastElytraDeltaY;
        /** Total ticks spent gliding in the current flight. */
        int elytraTicks;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** Cleans up player state on disconnect. */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes movement packets: delegates to {@link #handleElytraMovement} while
     * gliding, and to {@link #handleElytraLanding} when a flight ends.
     *
     * @param player the player associated with this packet
     * @param event  the incoming movement packet
     */
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

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Validates a single tick of elytra flight against horizontal speed, hover,
     * kick-boost, and ascent thresholds. Each sub-check independently adds to or
     * decays the violation buffer.
     *
     * @param player the player being checked
     * @param state  current elytra flight state for this player
     */
    private void handleElytraMovement(WindfallPlayer player, PlayerState state) {
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double deltaY = player.getDeltaY();

        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        /** Check 1: Horizontal speed exceeding the elytra maximum. */
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

        /** Check 2: Hover detection — near-zero vertical delta sustained over many ticks. */
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

        /** Check 3: Kick-boost — impossible vertical launch while still on ground. */
        if (deltaY > 0 && deltaY > ELYTRA_KICKBOOST_MAX && player.isOnGround()) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        /** Check 4: Ascent after sustained gliding — vanilla only allows descent. */
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

    /**
     * Called when a player transitions from gliding to non-gliding. Currently
     * reserved for future landing-velocity analysis; ignores flights shorter
     * than 5 ticks to avoid false positives during brief elytra taps.
     *
     * @param player the player who just landed
     * @param state  final elytra state at landing
     */
    private void handleElytraLanding(WindfallPlayer player, PlayerState state) {
        if (state.elytraTicks < 5) return;
    }

    /**
     * Reflectively invokes {@code isGliding()} on the CraftPlayer to determine
     * if the player is currently using an elytra. Falls back to {@code false}
     * if the method is unavailable (pre-1.9 servers).
     *
     * @param player the player to check
     * @return {@code true} if the player is elytra-gliding
     */
    private boolean checkGliding(WindfallPlayer player) {
        try {
            java.lang.reflect.Method m = player.getPlayer().getClass().getMethod("isGliding");
            return (Boolean) m.invoke(player.getPlayer());
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns {@code true} if the event is a movement-type packet. */
    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
