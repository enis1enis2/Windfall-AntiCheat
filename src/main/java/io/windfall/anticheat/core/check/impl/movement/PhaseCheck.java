package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.compat.world.GhostBlockResolver;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.platform.FoliaCompat;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects phase/noclip — clients that move inside or through solid blocks.
 *
 * <p>Algorithm: Each movement packet, the player's feet position and head position (1.6 blocks above
 * feet) are checked against the world block grid. If either position is inside a solid block, the
 * clipping tick counter increments. After {@value MIN_CLIPPING_TICKS} consecutive ticks inside a
 * solid block, the check verifies the player is still moving (horizontal or vertical speed &gt;
 * {@value MAX_VELOCITY_INSIDE_BLOCK}). Movement while clipping is heavily penalized because
 * legitimate players cannot move inside solid blocks.
 *
 * <p>Key thresholds:
 * <ul>
 *   <li>{@value MIN_CLIPPING_TICKS} ticks — grace period to avoid false positives from lag/teleport</li>
 *   <li>{@value MAX_VELOCITY_INSIDE_BLOCK} — minimum speed to confirm intentional movement while clipped</li>
 *   <li>{@value MAX_BLOCK_CLIP} — maximum allowed clip distance (currently unused, reserved for edge cases)</li>
 * </ul>
 *
 * @see FlightCheck for vertical movement abuse
 * @see SpeedCheck for horizontal speed validation
 */
@CheckData(name = "Phase A", stableKey = "windfall.movement.phase", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.4)
public class PhaseCheck extends Check implements PacketCheck {

    /** Maximum allowed block clipping distance before the clip is considered negligible */
    private static final double MAX_BLOCK_CLIP = 0.1;
    /** Minimum speed (blocks/tick) while inside a solid block to confirm intentional movement */
    private static final double MAX_VELOCITY_INSIDE_BLOCK = 0.01;
    /** Consecutive ticks inside a solid block required before the check activates */
    private static final int MIN_CLIPPING_TICKS = 3;

    private static final class PlayerState {
        int clippingTicks;
        /** Ticks of grace granted when the world around the player only resolves to air (ghost blocks). */
        int ghostGraceTicks;
    }

    /** Ghost-block resync resolver — dense air around the player indicates desync, not phase. */
    private final GhostBlockResolver ghostBlocks = new GhostBlockResolver(false, 2);

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Returns the per-player phase check state.
     *
     * @param player the player to retrieve state for
     * @return the player's {@link PlayerState}, creating one if absent
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming movement packets to detect phase/noclip violations.
     *
     * <p>Checks the player's feet and head positions against solid blocks in the world.
     * Consecutive ticks inside a solid block build the clipping counter; movement while
     * clipping triggers the buffer increase.
     *
     * @param player the player who sent the movement packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!PredictionEngine.isMovementPacket(event)) return;

        PlayerState state = getState(player);
        PredictionContext ctx = new PredictionContext(player);

        try {
            /** Ghost-block grace: if the world only resolves to air around the player, the server
             * may be resyncing blocks — defer clipping validation until the grace expires. */
            if (state.ghostGraceTicks > 0) {
                state.ghostGraceTicks--;
                decreaseBuffer(player, 0.1);
                return;
            }
            if (ghostBlocks.shouldResync(ghostWorldView(player),
                    (int) Math.floor(ctx.x), (int) Math.floor(ctx.y), (int) Math.floor(ctx.z))) {
                state.ghostGraceTicks = 10;
                decreaseBuffer(player, 0.1);
                return;
            }

            /** Build a location from the player's reported position */
            org.bukkit.Location loc = new org.bukkit.Location(player.getPlayer().getWorld(), ctx.x, ctx.y, ctx.z);
            org.bukkit.block.Block feetBlock = loc.getBlock();
            /** Head position: 1.6 blocks above feet (standard eye height approximation) */
            org.bukkit.block.Block headBlock = loc.clone().add(0, 1.6, 0).getBlock();

            boolean feetInside = feetBlock.getType().isSolid();
            boolean headInside = headBlock.getType().isSolid();

            if (feetInside || headInside) {
                // Bypass resistance: if unconfirmed world changes exist (e.g., block breaks),
                // the player might legitimately be in open space — check simulation engine
                SimulationEngine simEngine = WindfallPlugin.getInstance().getSimulationEngine();
                if (simEngine != null && simEngine.needsSimulation(player)) {
                    SimulationEngine.SimulationResult result = simEngine.simulate(
                        player, ctx.x, ctx.y, ctx.z);
                    if (result.matches) {
                        // Movement matches an alternate world state — suppress clipping penalty
                        state.clippingTicks = Math.max(0, state.clippingTicks - 1);
                        decreaseBuffer(player, 0.3);
                        return;
                    }
                }

                state.clippingTicks++;
                if (state.clippingTicks >= MIN_CLIPPING_TICKS) {
                    /** Player is inside a solid block and still moving — phase hack */
                    if (ctx.horizontalSpeed > MAX_VELOCITY_INSIDE_BLOCK
                            || Math.abs(ctx.deltaY) > MAX_VELOCITY_INSIDE_BLOCK) {
                        increaseBuffer(player, 1.5);
                        if (getBuffer(player) > 3.0) {
                            flag(player);
                            resetBuffer(player);
                            state.clippingTicks = 0;
                        }
                    }
                }
            } else {
                state.clippingTicks = Math.max(0, state.clippingTicks - 1);
                decreaseBuffer(player, 0.2);
            }
        } catch (Exception e) {
            WindfallPlugin.getInstance().getLogger().fine("PhaseCheck: chunk-load or world-access exception — " + e.getMessage());
        }
    }

    /** No-op — phase detection only requires incoming movement packets. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Builds a {@link GhostBlockResolver.WorldView} over the player's Bukkit world.
     *
     * <p>Guarded for Folia: world reads only happen when the current thread owns the player's
     * region, otherwise an empty view is returned so {@code shouldResync} stays false (no grant).
     *
     * @param player the player whose world is probed
     * @return a world view resolving chunk-load and air checks against Bukkit
     */
    private GhostBlockResolver.WorldView ghostWorldView(WindfallPlayer player) {
        org.bukkit.World world = player.getPlayer().getWorld();
        FoliaCompat folia = WindfallPlugin.getInstance().getFoliaCompat();
        boolean safeThread = !folia.isFolia() || folia.isOwnedByCurrentRegion(player.getPlayer());
        return new GhostBlockResolver.WorldView() {
            @Override
            public boolean isChunkLoaded(int chunkX, int chunkZ) {
                if (!safeThread || world == null) return false;
                return world.isChunkLoaded(chunkX, chunkZ);
            }

            @Override
            public boolean isAir(int x, int y, int z) {
                if (!safeThread || world == null) return false;
                return world.getBlockAt(x, y, z).getType() == org.bukkit.Material.AIR;
            }
        };
    }
}
