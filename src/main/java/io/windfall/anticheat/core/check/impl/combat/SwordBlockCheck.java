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

    // maxVersion 107 = 1.9.x — sword blocking removed in the combat update
    @CheckData(name = "Sword Block A", stableKey = "windfall.combat.swordblock", decay = 0.015, setbackVl = 10, minVersion = 5, maxVersion = 107)
public class SwordBlockCheck extends Check implements PacketCheck {

    // 200ms window for block-then-attack combo detection
    private static final double BLOCK_AND_ATTACK_WINDOW_MS = 200;
    private static final int BLOCK_SPAM_THRESHOLD = 4;
    private static final long BLOCK_SPAM_WINDOW_MS = 1000;

    private final ArrayDeque<Long> blockTimestamps = new ArrayDeque<>();
    private final ArrayDeque<Long> attackTimestamps = new ArrayDeque<>();

    private long lastBlockTime;
    private boolean hasBlock;
    private int consecutiveBlockAttacks;

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
        long now = System.currentTimeMillis();
        attackTimestamps.addLast(now);
        while (!attackTimestamps.isEmpty() && now - attackTimestamps.peekFirst() > BLOCK_SPAM_WINDOW_MS) {
            attackTimestamps.removeFirst();
        }

        if (hasBlock) {
            long blockAttackDelta = now - lastBlockTime;
            if (blockAttackDelta < BLOCK_AND_ATTACK_WINDOW_MS) {
                consecutiveBlockAttacks++;
                if (consecutiveBlockAttacks >= BLOCK_SPAM_THRESHOLD) {
                    increaseBuffer(player, 1.0);
                    if (getBuffer(player) > 3.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                    consecutiveBlockAttacks = 0;
                }
            } else {
                consecutiveBlockAttacks = Math.max(0, consecutiveBlockAttacks - 1);
            }
        }

        checkBlockAttackSpeed(player, now);
    }

    private void handleBlock(WindfallPlayer player) {
        long now = System.currentTimeMillis();
        lastBlockTime = now;
        hasBlock = true;
        blockTimestamps.addLast(now);
        while (!blockTimestamps.isEmpty() && now - blockTimestamps.peekFirst() > BLOCK_SPAM_WINDOW_MS) {
            blockTimestamps.removeFirst();
        }
    }

    private void handleBlockPlace(WindfallPlayer player) {
        long now = System.currentTimeMillis();
        lastBlockTime = now;
        hasBlock = true;
    }

    // Block-to-attack ratio > 70% in 500ms window = scripting behavior
    private void checkBlockAttackSpeed(WindfallPlayer player, long now) {
        long windowMs = 500;
        long recentAttacks = attackTimestamps.stream()
            .filter(t -> now - t <= windowMs)
            .count();

        if (recentAttacks > 8) {
            double blockRatio = (double) blockTimestamps.size() / Math.max(1, attackTimestamps.size());
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
