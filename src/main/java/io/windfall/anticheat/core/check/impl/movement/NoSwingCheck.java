package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "No Swing A", stableKey = "windfall.movement.noswing", decay = 0.02, setbackVl = 10)
public class NoSwingCheck extends Check implements PacketCheck {

    private static final long SWING_TIMEOUT_MS = 300;
    private static final int BUFFER_THRESHOLD = 3;

    private static final class PlayerState {
        long lastSwingTime;
        int missingSwingCount;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.ANIMATION) {
            getState(player).lastSwingTime = System.currentTimeMillis();
            return;
        }

        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = wrapper.getAction();

            if (action == DiggingAction.START_DIGGING) {
                checkSwing(player);
            }
        } else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            if (wrapper.getFace() != null) {
                checkSwing(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private void checkSwing(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        if (now - state.lastSwingTime > SWING_TIMEOUT_MS) {
            state.missingSwingCount++;
            if (state.missingSwingCount >= BUFFER_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.missingSwingCount = Math.max(0, state.missingSwingCount - 1);
        }
    }
}
