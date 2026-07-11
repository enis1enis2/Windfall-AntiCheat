package io.windfall.anticheat.core.player;

import io.windfall.anticheat.WindfallPlugin;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final ConcurrentHashMap<UUID, WindfallPlayer> players = new ConcurrentHashMap<>();

    public WindfallPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    public void add(WindfallPlayer player) {
        players.put(player.getUuid(), player);
    }

    public WindfallPlayer remove(UUID uuid) {
        WindfallPlayer player = players.remove(uuid);
        if (player != null) {
            player.setValid(false);
            WindfallPlugin plugin = WindfallPlugin.getInstance();
            if (plugin.getPunishmentEngine() != null) {
                plugin.getPunishmentEngine().cleanup(uuid);
            }
        }
        return player;
    }

    public ConcurrentHashMap<UUID, WindfallPlayer> getAll() {
        return players;
    }

    public Collection<WindfallPlayer> getAllPlayers() {
        return players.values();
    }

    public int size() {
        return players.size();
    }
}
