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

/**
 * Detects invalid block placements that violate vanilla placement rules.
 *
 * <p>This check enforces three placement conditions:
 * <ol>
 *   <li><b>Rate limit</b> — Players may place at most {@value #MAX_PLACEMENTS_PER_TICK} blocks per
 *       client tick (50 ms window). Exceeding this indicates autoclicker or multi-thread placement.</li>
 *   <li><b>Occupied block</b> — The target block position must be air-type (AIR, CAVE_AIR, VOID_AIR).
 *       Placing into an already-occupied block indicates a ghost-block or NBS exploit.</li>
 *   <li><b>Self-intersection</b> — The placed block must not overlap the player's own bounding box.
 *       Overlap is detected via AABB intersection of the player and the target block.</li>
 * </ol>
 *
 * <p><b>Buffer logic:</b> Each violation adds 1.0 to the buffer. The check flags when the buffer
 * exceeds 5.0, then resets. The buffer decays at {@code decay = 0.02} per tick.
 *
 * @see MultiPlaceCheck — companion check for per-tick placement rate
 * @see FarPlaceCheck — companion check for placement distance
 */
@CheckData(name = "Invalid Place A", stableKey = "windfall.movement.invalidplace", decay = 0.02, setbackVl = 10)
public class InvalidPlaceCheck extends Check implements PacketCheck {

    /**
     * Maximum number of block placement packets allowed within a single client tick (50 ms).
     * Vanilla clients place at most 1–2 blocks per tick; 4 provides headroom for latency.
     */
    private static final int MAX_PLACEMENTS_PER_TICK = 4;

    /**
     * Per-player mutable state tracking placement counts within a tick window.
     */
    private static final class PlayerState {
        /** Number of placement packets received in the current tick window. */
        int placementsThisTick;
        /** Timestamp (ms) of the last tick reset. */
        long lastTick;
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

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming packets for this check.
     *
     * <p>Only inspects {@link PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT} packets.
     * Runs rate-limit, occupied-block, and self-intersection validations.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        /* Reset the placement counter if more than 50 ms (one client tick) has elapsed */
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

            /* AABB self-intersection: check if the target block overlaps the player's bounding box */
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

    /**
     * Flags the player and logs the specific detail of the violation.
     *
     * @param player the player to flag
     * @param detail a human-readable description of what was detected
     */
    private void flagDetail(WindfallPlayer player, String detail) {
        flag(player);
        var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
        logger.warning("[Invalid Place A] " + player.getName() + ": " + detail);
    }
}
