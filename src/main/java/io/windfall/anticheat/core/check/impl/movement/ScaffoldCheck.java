package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Scaffold A", stableKey = "windfall.movement.scaffold", decay = 0.005, setbackVl = 30, compat = {CompatFlag.FOLIA_UNSAFE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3, disableOnFolia = false)
public class ScaffoldCheck extends Check implements PacketCheck {

    private static final double JAVA_MAX_BLOCK_PLACE_PER_SECOND = 12.0;
    private static final double BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC = 8.0;
    private static final double BEDROCK_KB_MAX_BLOCKS_PER_SEC = 10.0;
    private static final double BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC = 9.0;

    private static final double SPRINTING_BLOCKS_PER_SEC_THRESHOLD = 4.0;
    private static final int ROLLING_WINDOW_SIZE = 10;
    private static final long PLACE_WINDOW_MS = 1000;

    private static final class PlayerState {
        int blocksPlacedThisWindow;
        long windowStartTime;
        int lastSlot;
        double blocksPerSecondAccum;
        int samplesCollected;
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
        if (isBlockPlacePacket(event)) {
            handleBlockPlace(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    private boolean isBlockPlacePacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT;
    }

    private void handleBlockPlace(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        if (state.windowStartTime == 0 || now - state.windowStartTime > PLACE_WINDOW_MS) {
            if (state.blocksPlacedThisWindow > 0) {
                double bps = state.blocksPlacedThisWindow;
                state.blocksPerSecondAccum += bps;
                state.samplesCollected++;
            }
            state.blocksPlacedThisWindow = 0;
            state.windowStartTime = now;
        }

        state.blocksPlacedThisWindow++;

        double bps = state.blocksPlacedThisWindow / Math.max(1.0, (now - state.windowStartTime) / 1000.0);

        if (player.isBedrock()) {
            checkBedrockScaffold(player, bps);
        } else {
            checkJavaScaffold(player, bps);
        }
    }

    private void checkJavaScaffold(WindfallPlayer player, double bps) {
        if (bps > JAVA_MAX_BLOCK_PLACE_PER_SECOND) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (player.isSprinting() && bps > SPRINTING_BLOCKS_PER_SEC_THRESHOLD) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    private void checkBedrockScaffold(WindfallPlayer player, double bps) {
        BedrockInfo info = player.getBedrockInfo();
        if (info == null) return;

        double maxBps;
        if (info.isTouchDevice()) {
            maxBps = BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC;
        } else if (info.isController()) {
            maxBps = BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC;
        } else {
            maxBps = BEDROCK_KB_MAX_BLOCKS_PER_SEC;
        }

        if (bps > maxBps) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 8.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }
}
