package io.windfall.anticheat.core.player;

import io.windfall.anticheat.WindfallPlugin;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of all active {@link WindfallPlayer} instances.
 *
 * <p>Players are added at LOGIN_SUCCESS and removed on PlayerQuitEvent.
 * The underlying {@link ConcurrentHashMap} supports concurrent access from
 * Netty packet threads, the main thread, and async tasks.
 *
 * @see WindfallPlayer for per-player state tracking
 * @see io.windfall.anticheat.core.network.PacketListener for login/quit lifecycle
 */
public class PlayerManager {

    // ConcurrentHashMap: concurrent access from Netty (packet threads), main thread, and async tasks
    private final ConcurrentHashMap<UUID, WindfallPlayer> players = new ConcurrentHashMap<>();

    /** Returns the tracked player for the given UUID, or null if not online */
    public WindfallPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    /** Registers a player — called once at LOGIN_SUCCESS */
    public void add(WindfallPlayer player) {
        players.put(player.getUuid(), player);
    }

    /**
     * Removes and invalidates a player — called on PlayerQuitEvent.
     *
     * <p>After removal, {@link WindfallPlayer#isValid()} returns false,
     * causing packet callbacks to skip this player immediately.
     * Also cleans up punishment engine state to prevent memory leaks.
     *
     * @return the removed player, or null if already absent
     */
    public WindfallPlayer remove(UUID uuid) {
        WindfallPlayer player = players.remove(uuid);
        if (player != null) {
            // Mark invalid so packet callbacks stop processing this player
            player.setValid(false);
            WindfallPlugin plugin = WindfallPlugin.getInstance();
            if (plugin.getPunishmentEngine() != null) {
                plugin.getPunishmentEngine().cleanup(uuid);
            }
        }
        return player;
    }

    /** Returns the raw map — used by CheckManager.onTick() for iteration */
    public ConcurrentHashMap<UUID, WindfallPlayer> getAll() {
        return players;
    }

    /** Returns all online players as a collection — safe for iteration during tick */
    public Collection<WindfallPlayer> getAllPlayers() {
        return players.values();
    }

    /** Returns the number of currently tracked players */
    public int size() {
        return players.size();
    }
}
