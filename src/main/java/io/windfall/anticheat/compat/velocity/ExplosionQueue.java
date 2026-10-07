package io.windfall.anticheat.compat.velocity;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ExplosionQueue {

    public static final class Vec3 {
        public double x;
        public double y;
        public double z;

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void add(Vec3 other) {
            this.x += other.x;
            this.y += other.y;
            this.z += other.z;
        }

        public Vec3 copy() {
            return new Vec3(x, y, z);
        }
    }

    public static final class VelocityData {
        public final int transaction;
        public final boolean setback;
        public final Vec3 vector;
        public double offset;
        boolean offsetInitialized;

        public VelocityData(int transaction, boolean setback, Vec3 vector) {
            this.transaction = transaction;
            this.setback = setback;
            this.vector = vector;
            this.offset = 0.0D;
        }
    }

    private final Deque<VelocityData> firstBread = new ArrayDeque<>();
    private VelocityData knownTaken;
    private VelocityData firstBreadMerged;

    public void addPlayerExplosion(int transaction, Vec3 velocity) {
        firstBread.addLast(new VelocityData(transaction, false, velocity.copy()));
    }

    public Vec3 peekFutureExplosion() {
        VelocityData head = firstBread.peekFirst();
        return head == null ? null : head.vector.copy();
    }

    public int getPendingCount() {
        return firstBread.size();
    }

    public boolean isEmpty() {
        return firstBread.isEmpty();
    }

    public VelocityData knownTaken() {
        return knownTaken;
    }

    public VelocityData merged() {
        return firstBreadMerged;
    }

    private void handleTransaction(int transactionID) {
        VelocityData data = firstBread.peekFirst();

        while (data != null) {
            if (data.transaction == transactionID) {
                if (knownTaken != null) {
                    firstBreadMerged = new VelocityData(-1, knownTaken.setback, knownTaken.vector.copy());
                    firstBreadMerged.vector.add(data.vector);
                } else {
                    firstBreadMerged = new VelocityData(-1, data.setback, data.vector.copy());
                }
                break;
            } else if (data.transaction < transactionID) {
                if (knownTaken != null) {
                    knownTaken.vector.add(data.vector);
                } else {
                    knownTaken = new VelocityData(data.transaction, data.setback, data.vector.copy());
                }

                firstBreadMerged = null;
                firstBread.pollFirst();
                data = firstBread.peekFirst();
            } else {
                break;
            }
        }
    }

    public Vec3 getPossibleExplosions(int lastTransaction, boolean justTesting) {
        handleTransaction(lastTransaction);

        if (knownTaken == null) {
            return null;
        }

        Vec3 result = knownTaken.vector.copy();
        if (!justTesting) {
            knownTaken = null;
        }
        return result;
    }

    public Vec3 getFirstBreadAddedExplosion(int lastTransaction) {
        handleTransaction(lastTransaction);
        return firstBreadMerged == null ? null : firstBreadMerged.vector.copy();
    }

    public void handlePredictionAnalysis(double offset) {
        if (knownTaken != null) {
            knownTaken.offset = minOffset(knownTaken, offset);
        }
        if (firstBreadMerged != null) {
            firstBreadMerged.offset = minOffset(firstBreadMerged, offset);
        }
    }

    private static double minOffset(VelocityData data, double offset) {
        if (!data.offsetInitialized) {
            data.offsetInitialized = true;
            return offset;
        }
        return Math.min(data.offset, offset);
    }

    public void forceExempt() {
        if (knownTaken != null) {
            knownTaken.offset = 0.0D;
            knownTaken.offsetInitialized = true;
        }
        if (firstBreadMerged != null) {
            firstBreadMerged.offset = 0.0D;
            firstBreadMerged.offsetInitialized = true;
        }
    }

    public boolean wouldFlag(double threshold) {
        return (knownTaken != null && knownTaken.offset > threshold)
                || (firstBreadMerged != null && firstBreadMerged.offset > threshold);
    }
}