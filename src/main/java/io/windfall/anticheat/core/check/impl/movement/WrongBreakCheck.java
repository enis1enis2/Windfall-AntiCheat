package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Wrong Break", stableKey = "windfall.movement.wrongbreak", decay = 0.02, setbackVl = 10)
public class WrongBreakCheck extends Check implements PacketCheck {

    private static final double MAX_Y_DEVIATION = 2.0;
    private static final int BUFFER_THRESHOLD = 3;

    private static final class PlayerState {
        double lastBreakX, lastBreakY, lastBreakZ;
        boolean hasLastBreak;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        PlayerState state = getState(player);

        int blockX = wrapper.getBlockPosition().getX();
        int blockY = wrapper.getBlockPosition().getY();
        int blockZ = wrapper.getBlockPosition().getZ();

        double yDeviation = Math.abs(player.getY() - blockY);

        if (yDeviation > MAX_Y_DEVIATION) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }

        if (state.hasLastBreak) {
            double dx = blockX - state.lastBreakX;
            double dz = blockZ - state.lastBreakZ;
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 10.0) {
                increaseBuffer(player, 2.0);
                if (getBuffer(player) > BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastBreakX = blockX;
        state.lastBreakY = blockY;
        state.lastBreakZ = blockZ;
        state.hasLastBreak = true;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
