package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.windfall.anticheat.compat.combat.CrystalAttackWindow;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects crystal-cart "place-break" exploits and ID-prediction structs.
 *
 * <p>Crystal PvP follows a tight rhythm: a crystal is spawned, shaken for a tick, then attacked.
 * A legitimate attack takes at least one full tick after placement, and the interaction is
 * resolved by the server (the client cannot pick the entity). The {@link CrystalAttackWindow}
 * model exposes two signals:
 *
 * <ul>
 *   <li><b>FAST_PLACE_BREAK</b> — an attack on <em>anything</em> within 100 ms of a crystal
 *       placement, accumulating into a buffer that flags when it exceeds the model's threshold.
 *       A constant stream of sub-100 ms place→break cycles is a crystal-cart sign.</li>
 *   <li><b>ID_PREDICT</b> — the client attacks an end crystal the same tick it spawned (0 ticks
 *       lived), which is impossible without predicting the entity id. This is a protocol-level
 *       cheat and triggers an immediate setback.</li>
 * </ul>
 *
 * <p>Entity spawns arrive on the outgoing path; attacks on the incoming path. Both are per-player
 * so the model stays correctly isolated on a shared server.
 *
 * @see Check
 * @see PacketCheck
 * @see CrystalAttackWindow
 */
@CheckData(name = "Crystal A", stableKey = "windfall.combat.crystal", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH},
    relaxMultiplier = 1.3)
public class CrystalCheck extends Check implements PacketCheck {

    /** How long a tracked crystal entity id is considered recent/relevant. */
    private static final long SPAWN_TRACK_WINDOW_MS = 5000L;

    /** Consecutive FAST_PLACE_BREAK model flags required before this check flags — trust scales it. */
    private static final double FLAGS_REQUIRED = 2.0D;

    /** Per-player crystal state — spawn ages and the attack-window model. */
    private static final class PlayerState {
        final CrystalAttackWindow window = new CrystalAttackWindow();
        final Map<Integer, Long> spawnTimes = new HashMap<>();
        int consecutiveFlags;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), uuid -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SPAWN_ENTITY) return;

        WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
        if (wrapper.getEntityType() != EntityTypes.END_CRYSTAL) return;

        long now = System.currentTimeMillis();
        PlayerState state = getState(player);
        state.window.onCrystalSpawn(wrapper.getEntityId(), now);
        state.window.onCrystalPlace(now);
        state.spawnTimes.put(wrapper.getEntityId(), now);
        pruneSpawns(state, now);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        long now = System.currentTimeMillis();
        PlayerState state = getState(player);
        int entityId = wrapper.getEntityId();

        Long spawnedAt = state.spawnTimes.get(entityId);
        boolean targetIsCrystal = spawnedAt != null;
        int targetTicksLived = targetIsCrystal
            ? (int) ((now - spawnedAt) / 50L)
            : Integer.MAX_VALUE;

        CrystalAttackWindow.Flag flag =
            state.window.onAttack(now, targetIsCrystal, targetIsCrystal, targetTicksLived);

        /** ID prediction is a protocol exploit — setback immediately, never wait for repeat. */
        if (flag == CrystalAttackWindow.Flag.ID_PREDICT) {
            state.consecutiveFlags = 0;
            flagWithSetback(player);
            resetBuffer(player);
            return;
        }

        if (flag == CrystalAttackWindow.Flag.FAST_PLACE_BREAK) {
            state.consecutiveFlags++;
            double required = trustAdjustedThreshold(player, FLAGS_REQUIRED);
            if (state.consecutiveFlags >= required) {
                flag(player);
                resetBuffer(player);
                state.consecutiveFlags = 0;
            }
        } else {
            state.consecutiveFlags = Math.max(0, state.consecutiveFlags - 1);
        }
    }

    /** Drops stale spawn entries so the id-predict window stays tight after long gaps. */
    private void pruneSpawns(PlayerState state, long now) {
        state.spawnTimes.entrySet().removeIf(entry -> now - entry.getValue() > SPAWN_TRACK_WINDOW_MS);
    }
}