package io.windfall.anticheat.core.platform;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class FoliaCompatTest {

    @Test
    void isAvailableFalseByDefault() {
        FoliaCompat compat = new FoliaCompat(false);
        assertFalse(compat.isFolia());
    }

    @Test
    void isFoliaTrueWhenConstructedWithTrue() {
        FoliaCompat compat = new FoliaCompat(true);
        assertTrue(compat.isFolia());
    }

    @Test
    void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new FoliaCompat(false));
        assertDoesNotThrow(() -> new FoliaCompat(true));
    }

    @Test
    void runOnEntityRunsTaskDirectlyWhenNotFolia() {
        FoliaCompat compat = new FoliaCompat(false);
        AtomicBoolean ran = new AtomicBoolean(false);
        compat.runOnEntity(null, () -> ran.set(true), null);
        assertTrue(ran.get());
    }

    @Test
    void runOnEntityWithNullEntityRunsTaskDirectly() {
        FoliaCompat compat = new FoliaCompat(false);
        AtomicBoolean ran = new AtomicBoolean(false);
        compat.runOnEntity(null, () -> ran.set(true), () -> {});
        assertTrue(ran.get());
    }

    @Test
    void runOnEntityWithNullTaskDoesNotThrow() {
        FoliaCompat compat = new FoliaCompat(false);
        assertDoesNotThrow(() -> compat.runOnEntity(null, null, null));
    }

    @Test
    void runOnEntityWithNullEntityAndNullTaskDoesNotThrow() {
        FoliaCompat compat = new FoliaCompat(true);
        assertDoesNotThrow(() -> compat.runOnEntity(null, null, null));
    }

    @Test
    void runOnPlayerDelegatesToRunOnEntity() {
        FoliaCompat compat = new FoliaCompat(false);
        AtomicBoolean ran = new AtomicBoolean(false);
        compat.runOnPlayer(null, () -> ran.set(true), null);
        assertTrue(ran.get());
    }

    @Test
    void isSameRegionReturnsTrueWhenNotFolia() {
        FoliaCompat compat = new FoliaCompat(false);
        assertTrue(compat.isSameRegion(null, null));
    }

    @Test
    void isSameRegionReturnsTrueWithNullInputs() {
        FoliaCompat compat = new FoliaCompat(true);
        assertTrue(compat.isSameRegion(null, null));
    }

    @Test
    void isSameRegionReturnsTrueWithNullFirst() {
        FoliaCompat compat = new FoliaCompat(true);
        assertTrue(compat.isSameRegion(null, null));
    }

    @Test
    void teleportAsyncThrowsWithNullPlayerOnNonFolia() {
        FoliaCompat compat = new FoliaCompat(false);
        assertThrows(NullPointerException.class,
            () -> compat.teleportAsync(null, null));
    }

    @Test
    void teleportAsyncThrowsWithNullPlayerOnFolia() {
        FoliaCompat compat = new FoliaCompat(true);
        assertThrows(NullPointerException.class,
            () -> compat.teleportAsync(null, null));
    }

    @Test
    void runOnEntityFallbackNotCalledWhenTaskRuns() {
        FoliaCompat compat = new FoliaCompat(false);
        AtomicBoolean fallbackRan = new AtomicBoolean(false);
        compat.runOnEntity(null, () -> {}, () -> fallbackRan.set(true));
        assertFalse(fallbackRan.get());
    }

    @Test
    void toStringReturnsNonNull() {
        FoliaCompat compat = new FoliaCompat(false);
        assertNotNull(compat.toString());
    }

    @Test
    void toStringReturnsNonEmpty() {
        FoliaCompat compat = new FoliaCompat(false);
        assertFalse(compat.toString().isEmpty());
    }

    @Test
    void multipleInstancesAreIndependent() {
        FoliaCompat a = new FoliaCompat(false);
        FoliaCompat b = new FoliaCompat(true);
        assertFalse(a.isFolia());
        assertTrue(b.isFolia());
    }
}
