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

/**
 * Detects block-breaking actions that violate spatial consistency rules.
 *
 * <p>This check applies two heuristics on {@link DiggingAction#START_DIGGING} events:
 * <ol>
 *   <li><b>Vertical deviation</b> — If the Y difference between the player's feet and the broken
 *       block exceeds {@value #MAX_Y_DEVIATION} blocks, the player is likely reaching too far
 *       vertically (e.g. break-aura reaching blocks below/above normal range).</li>
 *   <li><b>Horizontal teleport</b> — If the horizontal (XZ) distance between two consecutive
 *       break targets exceeds 10 blocks, the player is likely teleporting between break targets,
 *       a hallmark of kill-aura with rapid target switching.</li>
 * </ol>
 *
 * <p><b>Buffer logic:</b> Vertical deviation adds 1.0; horizontal teleport adds 2.0 (more severe).
 * The check flags when the buffer exceeds {@value #BUFFER_THRESHOLD} and resets afterward.
 * Valid placements decay the buffer by 0.5.
 *
 * @see FarBreakCheck — companion check for break reach distance
 * @see AirLiquidBreakCheck — companion check for breaking while in air/liquid
 */
@CheckData(name = "Wrong Break", stableKey = "windfall.movement.wrongbreak", decay = 0.02, setbackVl = 10)
public class WrongBreakCheck extends Check implements PacketCheck {

    /**
     * Maximum allowed vertical (Y-axis) distance between the player's feet and the block
     * being broken. Vanilla reach allows roughly 4.5 blocks, but Y deviation beyond 2 blocks
     * from standing position is suspicious for normal gameplay.
     */
    private static final double MAX_Y_DEVIATION = 2.0;

    /** Buffer must exceed this value before a flag is raised. */
    private static final int BUFFER_THRESHOLD = 3;

    /**
     * Per-player mutable state tracking the position of the last broken block.
     */
    private static final class PlayerState {
        /** X coordinate of the previous break target. */
        double lastBreakX, lastBreakY, lastBreakZ;
        /** Whether at least one break has been recorded for this player. */
        boolean hasLastBreak;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily initialises the per-player state for this check.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Processes incoming packets for this check.
     *
     * <p>Inspects {@link PacketType.Play.Client.PLAYER_DIGGING} packets with action
     * {@link DiggingAction#START_DIGGING}. Applies vertical-deviation and horizontal-teleport
     * heuristics, then records the break position for the next invocation.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;

        WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        PlayerState state = getState(player);

        int blockX = wrapper.getBlockPosition().getX();
        int blockY = wrapper.getBlockPosition().getY();
        int blockZ = wrapper.getBlockPosition().getZ();

        /* Vertical deviation: distance between player feet Y and target block Y */
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

        /* Horizontal teleport: detect suspicious distance between consecutive break targets */
        if (state.hasLastBreak) {
            double dx = blockX - state.lastBreakX;
            double dz = blockZ - state.lastBreakZ;
            /* 2D Euclidean distance (XZ plane) between consecutive breaks */
            double dist = Math.sqrt(dx * dx + dz * dz);

            /* 10 blocks is well beyond normal movement speed between break actions */
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
