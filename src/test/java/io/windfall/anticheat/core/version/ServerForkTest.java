package io.windfall.anticheat.core.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerForkTest {

    @Test
    void enumShouldHaveFiveValues() {
        assertEquals(5, ServerFork.values().length);
    }

    @Test
    void enumValuesAreInPriorityOrder() {
        ServerFork[] forks = ServerFork.values();
        assertEquals(ServerFork.FOLIA, forks[0]);
        assertEquals(ServerFork.PURPUR, forks[1]);
        assertEquals(ServerFork.PAPER, forks[2]);
        assertEquals(ServerFork.SPIGOT, forks[3]);
        assertEquals(ServerFork.BUKKIT, forks[4]);
    }

    @Test
    void detectReturnsNonNull() {
        ServerFork fork = ServerFork.detect(null);
        assertNotNull(fork);
    }

    @Test
    void detectReturnsBukkitInTestEnv() {
        ServerFork fork = ServerFork.detect(null);
        assertEquals(ServerFork.BUKKIT, fork);
    }

    @Test
    void detectIsFoliaReturnsFalseInTestEnv() {
        assertFalse(ServerFork.detect(null).isFolia());
    }

    @Test
    void isFoliaReturnsBooleanWithoutThrowing() {
        for (ServerFork fork : ServerFork.values()) {
            assertDoesNotThrow(fork::isFolia);
        }
    }

    @Test
    void isPurpurReturnsBooleanWithoutThrowing() {
        for (ServerFork fork : ServerFork.values()) {
            assertDoesNotThrow(fork::isPurpur);
        }
    }

    @Test
    void isPaperOrAboveReturnsBooleanWithoutThrowing() {
        for (ServerFork fork : ServerFork.values()) {
            assertDoesNotThrow(fork::isPaperOrAbove);
        }
    }

    @Test
    void isPaperOrAboveCorrectness() {
        assertTrue(ServerFork.FOLIA.isPaperOrAbove());
        assertTrue(ServerFork.PURPUR.isPaperOrAbove());
        assertTrue(ServerFork.PAPER.isPaperOrAbove());
        assertFalse(ServerFork.SPIGOT.isPaperOrAbove());
        assertFalse(ServerFork.BUKKIT.isPaperOrAbove());
    }

    @Test
    void priorityOrderingFoliaGreatest() {
        assertTrue(ServerFork.FOLIA.compareTo(ServerFork.PURPUR) < 0);
        assertTrue(ServerFork.PURPUR.compareTo(ServerFork.PAPER) < 0);
        assertTrue(ServerFork.PAPER.compareTo(ServerFork.SPIGOT) < 0);
        assertTrue(ServerFork.SPIGOT.compareTo(ServerFork.BUKKIT) < 0);
    }

    @Test
    void toStringReturnsNonNullNonEmpty() {
        for (ServerFork fork : ServerFork.values()) {
            assertNotNull(fork.toString());
            assertFalse(fork.toString().isEmpty());
        }
    }

    @Test
    void getDisplayNameReturnsNonNullNonEmpty() {
        for (ServerFork fork : ServerFork.values()) {
            assertNotNull(fork.getDisplayName());
            assertFalse(fork.getDisplayName().isEmpty());
        }
    }

    @Test
    void getDescriptionReturnsNonNullNonEmpty() {
        for (ServerFork fork : ServerFork.values()) {
            assertNotNull(fork.getDescription());
            assertFalse(fork.getDescription().isEmpty());
        }
    }
}
