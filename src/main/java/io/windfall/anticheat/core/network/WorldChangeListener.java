package io.windfall.anticheat.core.network;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.compensation.LatencyCompensator;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

/**
 * Feeds real world changes into {@link LatencyCompensator} so lag compensation has data.
 *
 * <p>{@link LatencyCompensator} was built to know which block edits a client had not yet seen,
 * but nothing reported edits to it — the per-tick history stayed empty, so
 * {@link io.windfall.anticheat.core.compensation.SimulationEngine} always fell back to its
 * single "no unconfirmed changes" scenario and the whole compensation path was inert.
 *
 * <p>Each edit is recorded for the players who could plausibly have observed it (same world,
 * within {@value #OBSERVE_RADIUS_SQUARED} blocks squared) and tagged with that player's own
 * current server tick, so a high-latency client gets its own confirmation window instead of
 * inheriting the server's.
 *
 * @see LatencyCompensator for the tracking this listener feeds
 * @see io.windfall.anticheat.core.compensation.SimulationEngine for the consumer
 */
public final class WorldChangeListener implements Listener {

    /** Squared observe radius — only players this close can have seen the edit */
    private static final double OBSERVE_RADIUS_SQUARED = 64.0 * 64.0;

    private final WindfallPlugin plugin;

    public WorldChangeListener(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Records a broken block for every nearby player.
     *
     * @param event the (non-cancelled) block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        broadcast(location, (compensator, uuid, tick) ->
            compensator.onBlockChange(uuid,
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                org.bukkit.Material.AIR, tick, location.getWorld()));
    }

    /**
     * Records a placed block for every nearby player.
     *
     * @param event the (non-cancelled) block place event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Location location = event.getBlock().getLocation();
        org.bukkit.Material material = location.getBlock().getType();
        broadcast(location, (compensator, uuid, tick) ->
            compensator.onBlockChange(uuid,
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                material, tick, location.getWorld()));
    }

    /**
     * Applies one recording action for every tracked player close enough to observe the edit.
     * Players far away, in other worlds, or not yet registered are skipped.
     */
    private void broadcast(Location location, PerPlayerAction action) {
        PlayerManager playerManager = plugin.getPlayerManager();
        LatencyCompensator compensator = plugin.getLatencyCompensator();
        PingPongManager pingPong = plugin.getPingPongManager();
        World world = location.getWorld();

        if (playerManager == null || compensator == null || pingPong == null || world == null) return;

        for (Player online : world.getPlayers()) {
            UUID uuid = online.getUniqueId();
            WindfallPlayer wp = playerManager.get(uuid);
            if (wp == null || !wp.isValid()) continue;
            if (online.getLocation().distanceSquared(location) > OBSERVE_RADIUS_SQUARED) continue;

            action.apply(compensator, uuid, pingPong.getCurrentTick(wp));
        }
    }

    /** Recording action bound to a single edit location. */
    private interface PerPlayerAction {
        void apply(LatencyCompensator compensator, UUID uuid, int tick);
    }
}