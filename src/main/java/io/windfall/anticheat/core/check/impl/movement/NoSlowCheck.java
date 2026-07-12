package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players who bypass the vanilla slowdown applied when using items
 * (bow, shield, food, etc.). In survival Minecraft, using an item reduces
 * movement speed to ~20% of normal; hacked clients often ignore this penalty.
 *
 * <p><b>Algorithm:</b> When a player is flagged as using an item, each movement
 * packet is checked against the maximum expected speed ({@link #BASE_WALK_SPEED} *
 * {@link #SPRINT_MULTIPLIER} * 0.9). If the actual horizontal speed exceeds
 * 90% of that ceiling, the buffer increases by 0.8 per tick. Once the buffer
 * surpasses 4.0 the player is flagged. The buffer decays by 0.1 each tick when
 * no violation is detected.</p>
 *
 * @see CompatFlag#RELAX_ON_MISMATCH
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "NoSlow A", stableKey = "windfall.movement.noslow", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class NoSlowCheck extends Check implements PacketCheck {

    /** Base horizontal walk speed in blocks/tick for a non-sprinting player (~4.317 m/s). */
    private static final double BASE_WALK_SPEED = 0.102;

    /** Sprint speed multiplier applied on top of base walk speed. */
    private static final double SPRINT_MULTIPLIER = 1.3;

    /** Sneak speed multiplier — players move at 30% of base while sneaking. */
    private static final double SNEAK_MULTIPLIER = 0.3;

    /** Vanilla slowdown factor when using an item (speed reduced to 20%). */
    private static final double USING_ITEM_SLOWDOWN = 0.2;

    /** Minimum horizontal speed (blocks/tick) required before the check activates. */
    private static final double MIN_SPEED_FOR_CHECK = 0.05;

    /** Number of ticks an item must be in use before the slowdown check applies. */
    private static final int MIN_USING_ITEM_TICKS = 3;

    /** Per-player state tracking item-use status. */
    private static final class PlayerState {
        /** Whether the player is currently using an item (bow, food, shield, etc.). */
        boolean usingItem;
        /** Consecutive ticks the item has been in use. */
        int usingItemTicks;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /** Cleans up player state on disconnect to prevent memory leaks. */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Evaluates each movement packet for illegal speed while an item is in use.
     * Computes horizontal speed from deltaX/deltaZ and compares against the
     * vanilla sprint cap reduced by the item-use slowdown factor.
     *
     * @param player the player associated with this packet
     * @param event  the incoming movement packet
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (!isMovementPacket(event)) return;

        PlayerState state = getState(player);
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        /** Pythagorean horizontal speed magnitude (blocks/tick). */
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed < MIN_SPEED_FOR_CHECK) {
            decreaseBuffer(player, 0.1);
            return;
        }

        /** Maximum allowed horizontal speed while using an item (sprint * 0.9 tolerance). */
        double maxExpectedSpeed = BASE_WALK_SPEED * SPRINT_MULTIPLIER;

        if (state.usingItem && horizontalSpeed > maxExpectedSpeed * 0.9) {
            increaseBuffer(player, 0.8);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Returns {@code true} if the packet is a movement-type packet
     * (flying, position, or position-and-rotation).
     */
    private boolean isMovementPacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_FLYING
                || type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
