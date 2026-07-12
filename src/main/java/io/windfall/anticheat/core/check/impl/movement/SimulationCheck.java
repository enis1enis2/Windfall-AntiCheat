package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects movement simulation mismatches — clients whose vertical movement does not match
 * vanilla Minecraft's gravity + drag physics model.
 *
 * <p>Algorithm: Tracks a running expected vertical velocity ({@code expectedDeltaY}) and compares
 * it against the player's actual deltaY each tick. The prediction applies:
 * <ul>
 *   <li><b>Airborne</b>: {@code predicted = (expectedDeltaY - GRAVITY) × AIR_DRAG}</li>
 *   <li><b>Swimming</b>: {@code predicted = (expectedDeltaY - GRAVITY) × WATER_DRAG}</li>
 * </ul>
 *
 * <p>The absolute deviation between predicted and actual deltaY is compared against
 * {@value MAX_SIMULATION_DEVIATION} blocks. Horizontal movement must exceed 0.1 blocks to
 * avoid false positives from near-stationary floating-point imprecision.
 *
 * <p>Tolerance is widened for:
 * <ul>
 *   <li>Bedrock clients: +15% (different physics precision)</li>
 *   <li>Legacy/Combat protocol versions: +20% (older movement rounding)</li>
 * </ul>
 *
 * <p>After {@value MIN_SAMPLES} consecutive deviations, the buffer builds proportionally to the
 * deviation ratio. This is the most physics-accurate vertical check in the movement suite.
 *
 * @see PhysicsConstants for GRAVITY and AIR_DRAG values
 * @see FlightCheck for a complementary hover/flight detection
 * @see SpeedCheck for horizontal speed validation
 */
@CheckData(name = "Simulation A", stableKey = "windfall.movement.simulation", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.4)
public class SimulationCheck extends Check implements PacketCheck {

    /** Maximum allowed deviation between predicted and actual deltaY (blocks) before counting as a mismatch */
    private static final double MAX_SIMULATION_DEVIATION = 0.15;
    /** Minimum consecutive mismatch samples before the buffer starts building */
    private static final int MIN_SAMPLES = 10;

    private static final class PlayerState {
        double expectedDeltaY;
        int samples;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Returns the per-player simulation check state.
     *
     * @param player the player to retrieve state for
     * @return the player's {@link PlayerState}, creating one if absent
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** Clears per-player state on disconnect to prevent memory leaks. */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming movement packets to simulate vertical physics and detect deviations.
     *
     * <p>Resets when the player touches the ground. For airborne players, predicts the next
     * deltaY using gravity and drag (air or water), then compares against actual movement.
     *
     * @param player the player who sent the movement packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);
        boolean onGround = player.isOnGround();
        double deltaY = player.getDeltaY();
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();

        /** Reset simulation when grounded — vertical velocity is zeroed */
        if (onGround) {
            state.expectedDeltaY = 0;
            state.samples = 0;
            return;
        }

        int protocol = player.getProtocolVersion();
        double gravity = PhysicsConstants.GRAVITY;
        double airDrag = PhysicsConstants.AIR_DRAG;

        if (player.isSwimming()) {
            /** Water drag: version-dependent (0.8 for pre-1.14, IEEE double for 1.14+) */
            double waterDrag = protocol >= 393 ? PhysicsConstants.WATER_DRAG : 0.8;
            double predictedDeltaY = (state.expectedDeltaY - gravity) * waterDrag;
            double verticalDeviation = Math.abs(deltaY - predictedDeltaY);
            checkDeviation(player, verticalDeviation, deltaX, deltaZ, state);
            state.expectedDeltaY = deltaY;
            return;
        }

        /** Airborne prediction: (previousVelocity - gravity) × airDrag */
        double predictedDeltaY = (state.expectedDeltaY - gravity) * airDrag;

        double verticalDeviation = Math.abs(deltaY - predictedDeltaY);
        checkDeviation(player, verticalDeviation, deltaX, deltaZ, state);

        /** Store the actual deltaY as the basis for the next tick's prediction */
        state.expectedDeltaY = deltaY;
    }

    /**
     * Evaluates a single vertical deviation sample and updates the buffer.
     *
     * <p>Only counts deviations when the player is moving horizontally (speed &gt; 0.1) to
     * avoid false positives from near-stationary floating-point imprecision. Tolerance is
     * widened for bedrock and legacy/combat protocol clients.
     *
     * @param player            the player being checked
     * @param verticalDeviation absolute difference between predicted and actual deltaY
     * @param deltaX            current tick horizontal X delta
     * @param deltaZ            current tick horizontal Z delta
     * @param state             mutable per-player state
     */
    private void checkDeviation(WindfallPlayer player, double verticalDeviation, double deltaX, double deltaZ, PlayerState state) {
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double maxDeviation = MAX_SIMULATION_DEVIATION;
        /** Bedrock clients: +15% tolerance for different physics precision */
        if (player.isBedrock()) {
            maxDeviation *= 1.15;
        }
        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);
        /** Legacy/Combat protocol versions: +20% tolerance for older movement rounding */
        if (bracket == VersionBracket.LEGACY || bracket == VersionBracket.COMBAT) {
            maxDeviation *= 1.2;
        }

        if (verticalDeviation > maxDeviation && horizontalSpeed > 0.1) {
            state.samples++;
            if (state.samples >= MIN_SAMPLES) {
                /** Buffer proportional to the deviation ratio — larger deviations penalized more */
                increaseBuffer(player, 0.5 * (verticalDeviation / maxDeviation));
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.samples = 0;
                }
            }
        } else {
            state.samples = Math.max(0, state.samples - 1);
            decreaseBuffer(player, 0.1);
        }
    }

    /** No-op — simulation detection only requires incoming movement packets. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Checks if the incoming packet is a player movement update.
     *
     * @param event the incoming packet event
     * @return true if the packet is a flying, position, or position-and-rotation packet
     */
    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
