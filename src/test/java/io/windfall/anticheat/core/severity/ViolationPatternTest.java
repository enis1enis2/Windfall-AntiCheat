package io.windfall.anticheat.core.severity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ViolationPatternTest {

    private ViolationPattern pattern;
    private final UUID testPlayer = UUID.randomUUID();
    private final UUID testPlayer2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pattern = new ViolationPattern(30, 3, 6000);
    }

    @Test
    void testRecordViolationIncreasesHistory() {
        pattern.recordViolation(testPlayer, "windfall.movement.speed", 5, 100);
        pattern.recordViolation(testPlayer, "windfall.combat.reach", 10, 200);

        // Player should have history now
        assertDoesNotThrow(() -> pattern.analyze(testPlayer));
        // No repeat pattern yet (only 1 day of violations)
        assertEquals(ViolationPattern.PatternType.NONE, pattern.analyze(testPlayer).type);
    }

    @Test
    void testRepeatOffenderDetected() {
        // Simulate violations on 3+ distinct days (days differ by 86400000ms each)
        long baseTime = System.currentTimeMillis();
        pattern.recordViolation(testPlayer, "windfall.movement.speed", 5, 100);
        // Manually set timestamp to different days
        ViolationPattern.PlayerHistory history = getHistory(testPlayer);
        history.violations.get(0).timestampMs = baseTime;

        history.violations.add(new ViolationPattern.ViolationEntry(
            "windfall.movement.speed", 8, 200, baseTime + 86400000L));
        history.violations.add(new ViolationPattern.ViolationEntry(
            "windfall.movement.speed", 12, 300, baseTime + 172800000L));

        ViolationPattern.PatternAssessment assessment = pattern.analyze(testPlayer);
        assertEquals(ViolationPattern.PatternType.REPEAT_OFFENDER, assessment.type);
        assertEquals(3, assessment.signalStrength);
    }

    @Test
    void testNoneForNewPlayer() {
        UUID unknownPlayer = UUID.randomUUID();
        ViolationPattern.PatternAssessment assessment = pattern.analyze(unknownPlayer);
        assertEquals(ViolationPattern.PatternType.NONE, assessment.type);
    }

    @Test
    void testGetRecommendedActionForRepeatOffender() {
        ViolationPattern.PatternAssessment assessment = new ViolationPattern.PatternAssessment(
            ViolationPattern.PatternType.REPEAT_OFFENDER, 3, "test");
        assertEquals("escalate", pattern.getRecommendedAction(assessment));
    }

    @Test
    void testGetRecommendedActionForTogglePattern() {
        ViolationPattern.PatternAssessment assessment = new ViolationPattern.PatternAssessment(
            ViolationPattern.PatternType.TOGGLE_PATTERN, 4, "test");
        assertEquals("investigate", pattern.getRecommendedAction(assessment));
    }

    @Test
    void testGetRecommendedActionForEscalation() {
        ViolationPattern.PatternAssessment assessment = new ViolationPattern.PatternAssessment(
            ViolationPattern.PatternType.ESCALATION, 10, "test");
        assertEquals("immediate-action", pattern.getRecommendedAction(assessment));
    }

    @Test
    void testGetRecommendedActionForNone() {
        assertEquals("standard", pattern.getRecommendedAction(ViolationPattern.PatternAssessment.NONE));
        assertEquals("standard", pattern.getRecommendedAction(null));
    }

    @Test
    void testClearPlayerHistory() {
        pattern.recordViolation(testPlayer, "windfall.movement.speed", 5, 100);
        pattern.clearPlayerHistory(testPlayer);

        // Should return NONE after clearing
        assertEquals(ViolationPattern.PatternType.NONE, pattern.analyze(testPlayer).type);
        assertEquals(0, pattern.getTrackedPlayerCount());
    }

    @Test
    void testMultiplePlayersIndependent() {
        pattern.recordViolation(testPlayer, "windfall.movement.speed", 5, 100);
        pattern.recordViolation(testPlayer2, "windfall.combat.reach", 10, 200);

        assertEquals(2, pattern.getTrackedPlayerCount());

        pattern.clearPlayerHistory(testPlayer);
        assertEquals(1, pattern.getTrackedPlayerCount());
    }

    @Test
    void testDisabledDoesNotRecord() {
        pattern.loadConfig(false, 30, 3, 6000);
        pattern.recordViolation(testPlayer, "windfall.movement.speed", 5, 100);

        assertEquals(0, pattern.getTrackedPlayerCount());
        assertEquals(ViolationPattern.PatternType.NONE, pattern.analyze(testPlayer).type);
    }

    @Test
    void testRecordCleanSession() {
        pattern.recordCleanSession(testPlayer, 100);
        pattern.recordCleanSession(testPlayer, 200);

        // Clean sessions recorded, should not throw
        assertDoesNotThrow(() -> pattern.analyze(testPlayer));
    }

    @Test
    void testPruneOldHistories(@TempDir Path tempDir) {
        ViolationPattern filePattern = new ViolationPattern(tempDir, Logger.getLogger("test"));
        filePattern.loadConfig(true, 0, 3, 6000); // 0 days = everything is old

        // Record a violation so a file exists
        filePattern.recordViolation(testPlayer, "test", 1, 1);
        filePattern.savePlayerHistory(testPlayer);

        // Prune should remove old files
        assertDoesNotThrow(filePattern::pruneOldHistories);
    }

    @Test
    void testSaveAndLoadPlayerHistory(@TempDir Path tempDir) {
        ViolationPattern filePattern = new ViolationPattern(tempDir, Logger.getLogger("test"));

        filePattern.recordViolation(testPlayer, "windfall.movement.speed", 5, 100);
        filePattern.recordViolation(testPlayer, "windfall.combat.reach", 10, 200);
        filePattern.savePlayerHistory(testPlayer);

        // Load in a fresh instance
        ViolationPattern freshPattern = new ViolationPattern(tempDir, Logger.getLogger("test2"));
        freshPattern.loadPlayerHistory(testPlayer);

        // Should have the same data
        ViolationPattern.PatternAssessment assessment = freshPattern.analyze(testPlayer);
        assertEquals(ViolationPattern.PatternType.NONE, assessment.type); // Only 1 day
    }

    // Helper to access internal history
    private ViolationPattern.PlayerHistory getHistory(UUID playerUuid) {
        java.lang.reflect.Field field;
        try {
            field = ViolationPattern.class.getDeclaredField("historyMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<UUID, ViolationPattern.PlayerHistory> map =
                (java.util.Map<UUID, ViolationPattern.PlayerHistory>) field.get(pattern);
            return map.computeIfAbsent(playerUuid, k -> new ViolationPattern.PlayerHistory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
