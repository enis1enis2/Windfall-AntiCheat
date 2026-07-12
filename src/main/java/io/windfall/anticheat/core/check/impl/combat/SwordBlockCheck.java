package io.windfall.anticheat.core.check.impl.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects impossible sword-blocking-while-attacking patterns on pre-1.9 clients (protocol ≤ 107).
 *
 * <p><b>Context:</b> In Minecraft versions 1.0–1.8.x, players can raise their sword to block
 * incoming damage while simultaneously attacking. Some cheat clients automate this with
 * inhuman speed — block-attacking in under {@value BLOCK_AND_ATTACK_WINDOW_MS}ms repeatedly.</p>
 *
 * <p><b>Detection algorithm:</b>
 * <ul>
 *   <li><b>Block-attack timing:</b> Measures the gap between a block event and the subsequent
 *       attack. If this gap is under {@value BLOCK_AND_ATTACK_WINDOW_MS}ms for
 *       {@value BLOCK_SPAM_THRESHOLD}+ consecutive occurrences, buffer increases.</li>
 *   <li><b>Block-attack speed ratio:</b> Counts attacks within a 500ms window and compares
 *       them to the block-to-attack ratio. If there are 8+ attacks per 500ms with a block
 *       ratio above 0.7, buffer increases (indicates automated block-on-click).</li>
 * </ul>
 *
 * <p>This check only runs on protocol versions 5–107 (Minecraft 1.0–1.8.x), as sword
 * blocking was removed in the 1.9 combat update.</p>
 *
 * @see io.windfall.anticheat.core.check.Check
 */
    // maxVersion 107 = 1.9.x — sword blocking removed in the combat update
    @CheckData(name = "Sword Block A", stableKey = "windfall.combat.swordblock", decay = 0.015, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class SwordBlockCheck extends Check implements PacketCheck {

    /** Maximum time (ms) between a block event and an attack to consider them "simultaneous". */
    private static final double BLOCK_AND_ATTACK_WINDOW_MS = 200;
    /** Number of consecutive block-attack pairs required before flagging. */
    private static final int BLOCK_SPAM_THRESHOLD = 4;
    /** Sliding window (ms) for counting attack frequency and block spam. */
    private static final long BLOCK_SPAM_WINDOW_MS = 1000;

    /** Per-player state tracking block/attack timing and frequency. */
    private static final class PlayerState {
        final ArrayDeque<Long> blockTimestamps = new ArrayDeque<>();
        final ArrayDeque<Long> attackTimestamps = new ArrayDeque<>();
        long lastBlockTime;
        boolean hasBlock;
        int consecutiveBlockAttacks;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or initializes the tracking state for the given player.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Routes incoming packets to the appropriate handler based on packet type:
     * attacks, item use (blocking), or block placement.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleAttack(player);
        } else if (type == PacketType.Play.Client.USE_ITEM) {
            handleBlock(player);
        } else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            handleBlockPlace(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Handles an attack packet. Records the attack timestamp, checks for inhuman
     * block-attack timing, and delegates to {@link #checkBlockAttackSpeed} for
     * frequency analysis.
     *
     * @param player the attacking player
     */
    private void handleAttack(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.attackTimestamps.addLast(now);
        while (!state.attackTimestamps.isEmpty() && now - state.attackTimestamps.peekFirst() > BLOCK_SPAM_WINDOW_MS) {
            state.attackTimestamps.removeFirst();
        }

        if (state.hasBlock) {
            long blockAttackDelta = now - state.lastBlockTime;
            if (blockAttackDelta < BLOCK_AND_ATTACK_WINDOW_MS) {
                state.consecutiveBlockAttacks++;
                if (state.consecutiveBlockAttacks >= BLOCK_SPAM_THRESHOLD) {
                    increaseBuffer(player, 1.0);
                    if (getBuffer(player) > 3.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                    state.consecutiveBlockAttacks = 0;
                }
            } else {
                state.consecutiveBlockAttacks = Math.max(0, state.consecutiveBlockAttacks - 1);
            }
        }

        checkBlockAttackSpeed(player, state, now);
    }

    /**
     * Handles a USE_ITEM packet (sword block). Records the timestamp and marks
     * the player as currently blocking.
     *
     * @param player the blocking player
     */
    private void handleBlock(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.lastBlockTime = now;
        state.hasBlock = true;
        state.blockTimestamps.addLast(now);
        while (!state.blockTimestamps.isEmpty() && now - state.blockTimestamps.peekFirst() > BLOCK_SPAM_WINDOW_MS) {
            state.blockTimestamps.removeFirst();
        }
    }

    /**
     * Handles a BLOCK_PLACEMENT packet. Updates the block state similarly to
     * {@link #handleBlock}, as block placement with a sword also represents blocking.
     *
     * @param player the player placing the block
     */
    private void handleBlockPlace(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.lastBlockTime = now;
        state.hasBlock = true;
    }

    /**
     * Analyzes block-to-attack ratio within a 500ms window. If the player has
     * 8+ attacks per window with a block ratio above 0.7, it indicates automated
     * block-on-click behavior (the cheat always right-clicks before left-clicking).
     *
     * @param player the player being checked
     * @param state  the player's tracking state
     * @param now    current timestamp in milliseconds
     */
    private void checkBlockAttackSpeed(WindfallPlayer player, PlayerState state, long now) {
        long windowMs = 500;
        long recentAttacks = state.attackTimestamps.stream()
            .filter(t -> now - t <= windowMs)
            .count();

        if (recentAttacks > 8) {
            double blockRatio = (double) state.blockTimestamps.size() / Math.max(1, state.attackTimestamps.size());
            if (blockRatio > 0.7) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }
    }
}
