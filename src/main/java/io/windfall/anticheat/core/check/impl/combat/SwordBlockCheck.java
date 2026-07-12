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

    // maxVersion 107 = 1.9.x — sword blocking removed in the combat update
    @CheckData(name = "Sword Block A", stableKey = "windfall.combat.swordblock", decay = 0.015, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class SwordBlockCheck extends Check implements PacketCheck {

    private static final double BLOCK_AND_ATTACK_WINDOW_MS = 200;
    private static final int BLOCK_SPAM_THRESHOLD = 4;
    private static final long BLOCK_SPAM_WINDOW_MS = 1000;

    private static final class PlayerState {
        final ArrayDeque<Long> blockTimestamps = new ArrayDeque<>();
        final ArrayDeque<Long> attackTimestamps = new ArrayDeque<>();
        long lastBlockTime;
        boolean hasBlock;
        int consecutiveBlockAttacks;
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

    private void handleBlockPlace(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.lastBlockTime = now;
        state.hasBlock = true;
    }

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
