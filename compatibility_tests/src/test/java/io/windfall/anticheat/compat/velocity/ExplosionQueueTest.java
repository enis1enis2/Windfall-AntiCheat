package io.windfall.anticheat.compat.velocity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplosionQueueTest {

    private ExplosionQueue queue() {
        ExplosionQueue queue = new ExplosionQueue();
        queue.addPlayerExplosion(5, new ExplosionQueue.Vec3(1.0D, 0.0D, 0.0D));
        queue.addPlayerExplosion(6, new ExplosionQueue.Vec3(0.0D, 1.0D, 0.0D));
        return queue;
    }

    @Test
    void olderTransactionsMergeAndConsume() {
        ExplosionQueue queue = queue();

        ExplosionQueue.Vec3 first = queue.getPossibleExplosions(7, false);

        assertNotNull(first);
        assertEquals(1.0D, first.x, 1.0E-9D);
        assertEquals(1.0D, first.y, 1.0E-9D, "second explosion merges into first taken velocity");

        assertNull(queue.getPossibleExplosions(7, false), "taken velocity is consumed exactly once");
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void exactMatchKeepsEntryInFirstBread() {
        ExplosionQueue queue = new ExplosionQueue();
        queue.addPlayerExplosion(5, new ExplosionQueue.Vec3(1.0D, 0.0D, 0.0D));

        ExplosionQueue.Vec3 added = queue.getFirstBreadAddedExplosion(5);

        assertNotNull(added);
        assertEquals(1.0D, added.x, 1.0E-9D);
        assertEquals(1, queue.getPendingCount(), "exact match must not poll the first-bread entry");
    }

    @Test
    void futureTransactionConsumesNothing() {
        ExplosionQueue queue = queue();

        assertNull(queue.getPossibleExplosions(4, false));
        assertEquals(2, queue.getPendingCount());
    }

    @Test
    void justTestingLeavesKnownTakenInPlace() {
        ExplosionQueue queue = queue();

        ExplosionQueue.Vec3 firstProbe = queue.getPossibleExplosions(7, true);
        ExplosionQueue.Vec3 secondProbe = queue.getPossibleExplosions(7, true);

        assertNotNull(firstProbe);
        assertNotNull(secondProbe);
        assertEquals(firstProbe.x, secondProbe.x, 0.0D);
        assertEquals(firstProbe.y, secondProbe.y, 0.0D);
    }

    @Test
    void returnedVectorIsACopy() {
        ExplosionQueue queue = new ExplosionQueue();
        queue.addPlayerExplosion(5, new ExplosionQueue.Vec3(1.0D, 0.0D, 0.0D));

        ExplosionQueue.Vec3 result = queue.getPossibleExplosions(8, false);
        assertNotNull(result);

        result.x = 500.0D;

        ExplosionQueue.Vec3 peek = queue.peekFutureExplosion();
        assertNull(peek, "queue is empty after consumption");

        ExplosionQueue tail = new ExplosionQueue();
        tail.addPlayerExplosion(5, new ExplosionQueue.Vec3(1.0D, 2.0D, 3.0D));
        ExplosionQueue.Vec3 head = tail.peekFutureExplosion();
        head.x = 99.0D;
        assertTrue(tail.peekFutureExplosion().x == 1.0D, "peek must not alias internal state");
    }

    @Test
    void offsetsDriveFlagDecision() {
        ExplosionQueue queue = queue();

        ExplosionQueue.Vec3 taken = queue.getPossibleExplosions(7, true);
        assertNotNull(taken);

        queue.handlePredictionAnalysis(0.5D);
        assertTrue(queue.wouldFlag(0.4D));
        assertFalse(queue.wouldFlag(0.6D));

        queue.forceExempt();
        assertFalse(queue.wouldFlag(0.4D));
    }

    @Test
    void mergedEntryKeepsOffsetAccounting() {
        ExplosionQueue queue = new ExplosionQueue();
        queue.addPlayerExplosion(5, new ExplosionQueue.Vec3(1.0D, 0.0D, 0.0D));

        assertNotNull(queue.getFirstBreadAddedExplosion(5));
        queue.handlePredictionAnalysis(0.25D);

        assertTrue(queue.wouldFlag(0.2D));
        assertEquals(0.25D, queue.merged().offset, 1.0E-9D);
    }
}