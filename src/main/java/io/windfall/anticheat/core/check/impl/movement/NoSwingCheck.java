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

/**
 * Detects players who break or place blocks without performing the arm-swing
 * animation. In vanilla Minecraft, a {@code ANIMATION} packet must precede
 * any dig or placement action; hacked clients that skip the swing to gain
 * speed or hide actions are flagged.
 *
 * <p><b>Algorithm:</b> Tracks the timestamp of the last swing packet. When a
 * {@code START_DIGGING} or non-null face {@code PLAYER_BLOCK_PLACEMENT}
 * packet arrives and no swing has been received within {@link #SWING_TIMEOUT_MS},
 * a missing-swing counter increments. After {@link #BUFFER_THRESHOLD} consecutive
 * misses the violation buffer rises, and once it exceeds 3.0 the player is flagged.</p>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "No Swing A", stableKey = "windfall.movement.noswing", decay = 0.02, setbackVl = 10)
public class NoSwingCheck extends Check implements PacketCheck {

    /** Maximum milliseconds since last swing packet before a dig/place is considered unsawn. */
    private static final long SWING_TIMEOUT_MS = 300;

    /** Number of consecutive missing swings required before buffering a violation. */
    private static final int BUFFER_THRESHOLD = 3;

    /**
     * Per-player tracking state holding the last swing timestamp and the
     * count of consecutive actions performed without a swing.
     */
    private static final class PlayerState {
        /** {@link System#currentTimeMillis()} of the most recent ANIMATION packet. */
        long lastSwingTime;
        /** Consecutive dig/place actions received without an intervening swing. */
        int missingSwingCount;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming packets. Records swing timestamps for ANIMATION packets
     * and triggers a swing check on START_DIGGING and non-null-face block placements.
     *
     * @param player the player associated with this packet
     * @param event  the incoming packet event
     */
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

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Core swing-validation logic. Compares the time since the last swing against
     * {@link #SWING_TIMEOUT_MS}. If exceeded, the missing-swing counter increases;
     * otherwise it decays by 1 (minimum 0). Consecutive misses beyond
     * {@link #BUFFER_THRESHOLD} add 1.0 to the violation buffer, and a buffer
     * exceeding 3.0 triggers a flag.
     *
     * @param player the player to check
     */
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
