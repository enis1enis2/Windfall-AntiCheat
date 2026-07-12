package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects hitbox expansion cheats that artificially enlarge a player's or
 * entity's bounding box to make hits register at impossible distances.
 *
 * <p>This check reconstructs the player's look vector from their yaw and pitch,
 * projects it to the vanilla maximum reach distance ({@value #MAX_REACH} blocks
 * + {@value #PLAYER_BOX_EXPANSION} tolerance), and counts each attack as
 * "on-target" if the projected endpoint is within the expanded hitbox radius.</p>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>On each attack packet, compute the look-vector endpoint using the
 *       standard Minecraft rotation-to-direction formula projected
 *       {@value #MAX_REACH} blocks.</li>
 *   <li>If the endpoint is within {@value #MAX_REACH} + {@value #PLAYER_BOX_EXPANSION}
 *       blocks (the expanded bounding-box allowance), count it as a hit.</li>
 *   <li>After {@value #MIN_ATTACKS_PER_EVAL} attacks, compute the hit ratio.
 *       If the ratio exceeds {@value #HIT_RATIO_FLAG_THRESHOLD} (80%) and
 *       there are more than 20 total attacks, increment the buffer.</li>
 *   <li>The buffer flags at &gt; 5.0 and is reset after each flag.</li>
 * </ol>
 *
 * <h3>Key Constants</h3>
 * <ul>
 *   <li>{@value #MAX_REACH} (3.5) — vanilla maximum melee reach in blocks.</li>
 *   <li>{@value #PLAYER_BOX_EXPANSION} (0.15) — tolerance added to account for
 *       legitimate hitbox edge cases (latency, movement interpolation).</li>
 *   <li>{@value #HIT_RATIO_FLAG_THRESHOLD} (0.8) — above this, the player is
 *       hitting far more often than legitimately possible, suggesting expanded
 *       hitboxes.</li>
 * </ul>
 *
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "Hitboxes A", stableKey = "windfall.combat.hitboxes", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class HitboxesCheck extends Check implements PacketCheck {

    /**
     * Tolerance (in blocks) added to the vanilla reach distance to account for
     * legitimate edge cases such as latency and entity-size variance.
     */
    private static final double PLAYER_BOX_EXPANSION = 0.15;

    /** Minimum attack count before the hit-ratio evaluation is performed. */
    private static final int MIN_ATTACKS_PER_EVAL = 10;

    /**
     * Hit-ratio threshold above which the player is flagged. 0.8 means 80% of
     * attacks land, which is inhumanly consistent at maximum reach.
     */
    private static final double HIT_RATIO_FLAG_THRESHOLD = 0.8;

    /** Vanilla maximum melee reach distance in blocks. */
    private static final double MAX_REACH = 3.5;

    /** Buffer level at which a hitboxes flag is triggered. */
    private static final double FLAG_BUFFER_THRESHOLD = 5.0;

    /** Per-player mutable state for tracking hit-ratio statistics. */
    private static final class PlayerState {
        int attacksOnTarget;
        int totalAttacks;
    }

    /** Player state lookup keyed by UUID. */
    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily initialises the per-player state.
     *
     * @param player the player whose state is requested
     * @return the current {@link PlayerState}
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Evicts cached state when a player disconnects.
     *
     * @param uuid UUID of the departing player
     */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes attack-entity packets to evaluate hit-ratio consistency.
     *
     * <p>For each attack the look vector is projected from the player's eye
     * position and the hit distance is compared against the expanded reach
     * allowance. After enough samples the ratio of hits to total attacks is
     * evaluated against the flag threshold.</p>
     *
     * @param player the player performing the attack
     * @param event  the raw packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PlayerState state = getState(player);
        state.totalAttacks++;

        /* Player eye-position (origin of the look vector). */
        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight();
        double eyeZ = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        /*
         * Project the look vector to the maximum reach distance using the
         * standard Minecraft rotation-to-direction formula:
         *   lookX = -sin(yaw) * cos(pitch) * reach
         *   lookY = -sin(pitch) * reach
         *   lookZ =  cos(yaw) * cos(pitch) * reach
         */
        double lookX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * MAX_REACH;
        double lookY = -Math.sin(Math.toRadians(pitch)) * MAX_REACH;
        double lookZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * MAX_REACH;

        /* Maximum allowed reach including the expansion tolerance. */
        double maxReach = MAX_REACH + PLAYER_BOX_EXPANSION;

        /* Euclidean distance from eye to projected endpoint. */
        double hitDistance = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);

        if (hitDistance < maxReach) {
            state.attacksOnTarget++;
        }

        if (state.totalAttacks >= MIN_ATTACKS_PER_EVAL) {
            double hitRatio = (double) state.attacksOnTarget / state.totalAttacks;
            if (hitRatio > HIT_RATIO_FLAG_THRESHOLD && state.totalAttacks > 20) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > FLAG_BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
            /* Reset counters for the next evaluation window. */
            state.attacksOnTarget = 0;
            state.totalAttacks = 0;
        }
    }

    /** No outbound packets are relevant to this check. */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
