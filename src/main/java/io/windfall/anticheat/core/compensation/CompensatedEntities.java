package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.physics.BoundingBox;
import io.windfall.anticheat.core.physics.VersionPhysics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Tracks entity positions from server packets for hit-accuracy and reach checks
// Entity positions lag behind the client's view due to server tick delay
public final class CompensatedEntities {

    // 40 ticks ≈ 2 seconds of history — enough for high-ping players
    private static final int HISTORY_SIZE = 40;

    private final Map<Integer, History> entities = new ConcurrentHashMap<>();

    public void updateEntity(int entityId, double x, double y, double z) {
        entities.computeIfAbsent(entityId, k -> new History()).add(x, y, z);
    }

    public void removeEntity(int entityId) {
        entities.remove(entityId);
    }

    /** Clears all tracked entities — called on player disconnect to free memory */
    public void clear() {
        entities.clear();
    }

    public double[] getEntityPosition(int entityId) {
        History history = entities.get(entityId);
        if (history == null || history.size() == 0) return null;
        return history.getLatest();
    }

    public BoundingBox getEntityBoundingBox(int entityId, boolean sneaking, int protocolVersion) {
        double[] pos = getEntityPosition(entityId);
        if (pos == null) return null;
        double halfWidth = 0.3;
        double height = 1.8;
        return new BoundingBox(
                pos[0] - halfWidth, pos[1], pos[2] - halfWidth,
                pos[0] + halfWidth, pos[1] + height, pos[2] + halfWidth
        );
    }

    // Fixed-size circular buffer — avoids GC pressure from list resizing
    private static class History {
        private final double[][] positions = new double[HISTORY_SIZE][3];
        private int head = 0;
        private int size = 0;

        void add(double x, double y, double z) {
            positions[head][0] = x;
            positions[head][1] = y;
            positions[head][2] = z;
            head = (head + 1) % HISTORY_SIZE;
            if (size < HISTORY_SIZE) size++;
        }

        // Returns the most recent position — callers must handle null
        double[] getLatest() {
            if (size == 0) return null;
            int idx = (head - 1 + HISTORY_SIZE) % HISTORY_SIZE;
            return new double[]{positions[idx][0], positions[idx][1], positions[idx][2]};
        }

        int size() {
            return size;
        }
    }
}
