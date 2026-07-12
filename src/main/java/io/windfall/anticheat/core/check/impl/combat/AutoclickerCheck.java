package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.version.VersionBracket;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Autoclicker A", stableKey = "windfall.combat.autoclicker", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.5)
public class AutoclickerCheck extends Check implements PacketCheck {

    private static final int MIN_CLICKS_FOR_EVAL = 20;
    private static final long CLICK_WINDOW_MS = 3000;

    private static final double LOW_CPS_LEGACY = 6.0;
    private static final double HIGH_CPS_LEGACY = 20.0;

    private static final double LOW_CPS_MODERN = 1.0;
    private static final double HIGH_CPS_MODERN = 8.0;

    private static final double STD_DEV_AUTOCLICKER_THRESHOLD = 3.0;
    private static final double MIN_HUMAN_STD_DEV = 15.0;

    private static final class PlayerState {
        final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.clickTimestamps.addLast(now);
        while (!state.clickTimestamps.isEmpty() && now - state.clickTimestamps.peekFirst() > CLICK_WINDOW_MS) {
            state.clickTimestamps.removeFirst();
        }

        if (state.clickTimestamps.size() < MIN_CLICKS_FOR_EVAL) return;

        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);

        double lowCPS, highCPS;

        if (player.isBedrock()) {
            WindfallConfig cfg = WindfallPlugin.getInstance().getWindfallConfig();
            lowCPS = LOW_CPS_MODERN;
            highCPS = cfg.getBedrockCpsLimit();
        } else if (bracket == VersionBracket.LEGACY) {
            lowCPS = LOW_CPS_LEGACY;
            highCPS = HIGH_CPS_LEGACY;
        } else {
            lowCPS = LOW_CPS_MODERN;
            highCPS = HIGH_CPS_MODERN;
        }

        double cps = state.clickTimestamps.size() / (CLICK_WINDOW_MS / 1000.0);
        if (cps < lowCPS || cps > highCPS) {
            decreaseBuffer(player, 0.2);
            return;
        }

        double stdDev = calculateStdDev(state);

        if (stdDev < STD_DEV_AUTOCLICKER_THRESHOLD && cps > lowCPS) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (stdDev < MIN_HUMAN_STD_DEV) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 6.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private double calculateStdDev(PlayerState state) {
        if (state.clickTimestamps.size() < 2) return Double.MAX_VALUE;

        long first = state.clickTimestamps.peekFirst();
        double mean = 0;
        int count = 0;
        for (Long ts : state.clickTimestamps) {
            if (ts == first) continue;
            mean += ts - first;
            count++;
        }
        if (count == 0) return Double.MAX_VALUE;
        mean /= count;

        double variance = 0;
        for (Long ts : state.clickTimestamps) {
            if (ts == first) continue;
            double diff = (ts - first) - mean;
            variance += diff * diff;
        }
        variance /= count;

        return Math.sqrt(variance);
    }
}
