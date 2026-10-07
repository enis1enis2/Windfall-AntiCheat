package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.compat.sprint.DirectionalMovement;
import io.windfall.anticheat.compat.sprint.OmniSprintModel;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects sprinting in directions vanilla physics cannot produce ("omni-sprint").
 *
 * <p>Vanilla sprint only persists while the client moves roughly forward of its yaw; strafing
 * or backpedaling decays sprint immediately. A client that keeps full sprint speed while
 * moving sideways/backwards (or mid-air at impossible input angles) is extrapolating its
 * own sprint state, which is what the {@link OmniSprintModel} exposes here.
 *
 * <p>The model signals two independent failures:
 * <ul>
 *   <li><b>Ground failure</b> — sprinting while the movement direction is offset &ge; 78°
 *       from the yaw, accumulated to the model's ground threshold.</li>
 *   <li><b>Air failure</b> — sprinting while airborne with a useful forward-ish input at an
 *       angle the model proves impossible, accumulated to the air threshold.</li>
 * </ul>
 *
 * <p>Context is assembled from the shared {@link PredictionContext} plus the player's
 * rotation/ground/cached flags (water, climbable, honey, riptide, gliding, velocity). World
 * layers without a cached signal (webs, ice, slime, boats, walls) are conservatively off:
 * the model only rewards those as exemptions, so omitting them keeps it stricter, never laxer.
 *
 * @see Check
 * @see PacketCheck
 * @see OmniSprintModel
 */
@CheckData(name = "Sprint B", stableKey = "windfall.movement.sprint", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.3)
public class OmniSprintCheck extends Check implements PacketCheck {

    /** Consecutive model failures required before this check flags — trust scales it. */
    private static final double FLAGS_REQUIRED = 2.0D;

    /** Per-player sprint state — model instance plus rollback inputs the model needs. */
    private static final class PlayerState {
        final OmniSprintModel model = new OmniSprintModel();
        boolean lastSprinting;
        /** Previous tick's yaw delta (degrees) — used to compute yaw acceleration. */
        double lastDeltaYaw;
        int clientGroundTicks;
        int serverGroundTicks;
        int clientAirTicks;
        int serverAirTicks;
        int consecutiveFlags;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), uuid -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        PlayerState state = getState(player);
        PredictionContext ctx = new PredictionContext(player);

        float yaw = player.getYaw();
        float lastYaw = player.getLastYaw();
        double deltaYaw = DirectionalMovement.yawDifference(yaw, lastYaw);

        OmniSprintModel.Context c = new OmniSprintModel.Context();
        c.deltaX = ctx.deltaX;
        c.deltaZ = ctx.deltaZ;
        c.yaw = yaw;
        c.lastYaw = lastYaw;
        c.deltaYaw = deltaYaw;
        c.yawAccel = Math.abs(deltaYaw - state.lastDeltaYaw);
        c.baseSpeed = ctx.baseSpeed;
        c.sprinting = ctx.sprinting;
        c.lastSprinting = state.lastSprinting;
        c.onGround = ctx.onGround;
        c.serverGround = player.isServerOnGround();
        c.clientGroundTicks = state.clientGroundTicks;
        c.serverGroundTicksPlus = state.serverGroundTicks;
        c.clientAirTicks = state.clientAirTicks;
        c.serverAirTicks = state.serverAirTicks;
        c.packetMoving = player.isMovedSinceTick();
        c.insideWater = ctx.inWater;
        c.nearWater = ctx.inWater;
        c.nearWebs = false;
        c.nearClimbable = ctx.climbing;
        c.onIce = false;
        c.onSlime = false;
        c.onSoulSand = false;
        c.onHoney = player.isCachedOnHoney();
        c.nearBoat = false;
        c.onBoat = false;
        c.nearWall = false;
        c.colliding = false;
        c.underBlock = false;
        c.predictDownwards = false;
        c.predictUpwards = false;
        c.recentCollision = false;
        c.recentGhostBlock = false;
        c.teleports = false;
        c.riptiding = player.isCachedHasRiptide();
        c.recentRiptiding = false;
        c.gliding = player.isGliding();
        c.takingVelocity = player.isVelocityReceived();
        c.inVehicle = false;
        c.recentVehicle = false;
        c.slimeBounce = false;
        c.cancelPending = false;

        OmniSprintModel.Result result = state.model.handle(c);

        if (result.groundFlag || result.airFlag) {
            state.consecutiveFlags++;
            double required = trustAdjustedThreshold(player, FLAGS_REQUIRED);
            if (state.consecutiveFlags >= required) {
                flag(player);
                resetBuffer(player);
                state.consecutiveFlags = 0;
            }
        } else {
            state.consecutiveFlags = Math.max(0, state.consecutiveFlags - 1);
        }

        state.lastSprinting = ctx.sprinting;
        state.lastDeltaYaw = deltaYaw;
        state.clientGroundTicks = ctx.onGround ? state.clientGroundTicks + 1 : 0;
        state.serverGroundTicks = player.isServerOnGround() ? state.serverGroundTicks + 1 : 0;
        state.clientAirTicks = !ctx.onGround ? state.clientAirTicks + 1 : 0;
        state.serverAirTicks = !player.isServerOnGround() ? state.serverAirTicks + 1 : 0;
    }

    /** No-op — sprint detection only requires incoming movement packets. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}