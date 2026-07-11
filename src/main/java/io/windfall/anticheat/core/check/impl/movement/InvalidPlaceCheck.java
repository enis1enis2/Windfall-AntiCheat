package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;

@CheckData(name = "Invalid Place A", stableKey = "windfall.movement.invalidplace", decay = 0.02, setbackVl = 10)
public class InvalidPlaceCheck extends Check implements PacketCheck {

    private static final int MAX_PLACEMENTS_PER_TICK = 4;

    private static final class PlayerState {
        int placementsThisTick;
        long lastTick;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        if (now - state.lastTick > 50) {
            state.placementsThisTick = 0;
            state.lastTick = now;
        }

        state.placementsThisTick++;
        if (state.placementsThisTick > MAX_PLACEMENTS_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
            return;
        }

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        var position = wrapper.getBlockPosition();

        int bx = position.getX();
        int by = position.getY();
        int bz = position.getZ();

        try {
            Material type = player.getPlayer().getWorld().getBlockAt(bx, by, bz).getType();

            String typeName = type.name();
            if (type != Material.AIR && !typeName.equals("CAVE_AIR") && !typeName.equals("VOID_AIR")) {
                flagDetail(player, "placing in occupied block " + typeName);
                return;
            }

            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            if (px >= bx && px < bx + 1 && pz >= bz && pz < bz + 1
                    && py + player.getHeight() > by && py < by + 1) {
                flagDetail(player, "placing block inside player");
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
        logger.warning("[Invalid Place A] " + player.getName() + ": " + detail);
    }
}
