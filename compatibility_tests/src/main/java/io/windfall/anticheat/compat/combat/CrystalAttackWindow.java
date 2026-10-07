package io.windfall.anticheat.compat.combat;

import java.util.HashMap;
import java.util.Map;

public final class CrystalAttackWindow {

    public enum Flag {
        NONE,
        FAST_PLACE_BREAK,
        ID_PREDICT
    }

    public static final double FAST_PLACE_BREAK_FLAG = 6.0D;
    public static final double ID_PREDICT_FLAG = 5.0D;

    private static final double FAST_LIMIT_MS = 40.0D;
    private static final double FAST_WINDOW_MS = 100.0D;
    private static final long SPAWN_CACHE_TTL_MS = 60000L;

    private static final double PLACE_BREAK_STEP = 2.0D;
    private static final double PLACE_BREAK_SOFT_DECAY = 0.25D;
    private static final double PLACE_BREAK_DECAY = 0.1D;
    private static final double ID_PREDICT_STEP = 1.5D;

    private double buffer;
    private long lastPlaceMillis = Long.MIN_VALUE;

    private final Map<Integer, Long> crystalSpawnTimes = new HashMap<>();
    private long lastCleanupMillis = 0L;

    public void reset() {
        buffer = 0.0D;
        lastPlaceMillis = Long.MIN_VALUE;
        crystalSpawnTimes.clear();
    }

    public void onCrystalPlace(long nowMillis) {
        this.lastPlaceMillis = nowMillis;
    }

    public void onCrystalSpawn(int entityId, long nowMillis) {
        cleanup(nowMillis);
        crystalSpawnTimes.put(entityId, nowMillis);
    }

    public boolean wasSpawnedWithin(int entityId, long nowMillis, long windowMillis) {
        Long spawnedAt = crystalSpawnTimes.get(entityId);
        return spawnedAt != null && nowMillis - spawnedAt <= windowMillis;
    }

    public Flag onAttack(long nowMillis,
                         boolean targetResolved,
                         boolean targetIsCrystal,
                         int targetTicksLived) {
        cleanup(nowMillis);

        Flag flag = Flag.NONE;

        long lastPlace = lastPlaceMillis == Long.MIN_VALUE ? 0L : lastPlaceMillis;
        long diff = nowMillis - lastPlace;

        if (diff < FAST_WINDOW_MS) {
            if (diff < FAST_LIMIT_MS) {
                buffer += PLACE_BREAK_STEP;
                if (buffer > FAST_PLACE_BREAK_FLAG) {
                    flag = Flag.FAST_PLACE_BREAK;
                    buffer = 3.0D;
                }
            } else {
                buffer = Math.max(0.0D, buffer - PLACE_BREAK_SOFT_DECAY);
            }
        } else {
            buffer = Math.max(0.0D, buffer - PLACE_BREAK_DECAY);
        }

        if (targetResolved && targetIsCrystal && targetTicksLived <= 0) {
            buffer += ID_PREDICT_STEP;
            if (buffer > ID_PREDICT_FLAG) {
                flag = Flag.ID_PREDICT;
            }
        }

        return flag;
    }

    public double getBuffer() {
        return buffer;
    }

    private void cleanup(long nowMillis) {
        if (nowMillis - lastCleanupMillis > SPAWN_CACHE_TTL_MS) {
            crystalSpawnTimes.clear();
            lastCleanupMillis = nowMillis;
        }
    }
}