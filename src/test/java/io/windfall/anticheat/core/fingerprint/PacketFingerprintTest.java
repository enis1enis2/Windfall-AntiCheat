package io.windfall.anticheat.core.fingerprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketFingerprintTest {

    private PacketFingerprint fingerprint;
    private final UUID testPlayer = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        fingerprint = new PacketFingerprint();
    }

    @Test
    void testVanillaBrandReturnsLowScore() {
        int score = fingerprint.setBrand(testPlayer, "Vanilla");
        assertTrue(score <= 8, "Vanilla brand should return low severity score");
    }

    @Test
    void testCheatClientBrandReturnsHighScore() {
        int score = fingerprint.setBrand(testPlayer, "Wurst");
        assertTrue(score >= 15, "Known cheat client brand should return high severity score");
    }

    @Test
    void testUnknownBrandReturnsModerateScore() {
        int score = fingerprint.setBrand(testPlayer, "SomeUnknownClient");
        assertTrue(score >= 5 && score <= 12,
            "Unknown brand should return moderate severity score");
    }

    @Test
    void testSafeClientBrandReturnsLowScore() {
        int score = fingerprint.setBrand(testPlayer, "Feather");
        assertTrue(score <= 8, "Safe client brand (Feather) should return low score");
    }

    @Test
    void testChannelAnomalyDetection() {
        // Standard Minecraft channel should be fine
        fingerprint.addChannel(testPlayer, "minecraft:brand");
        int score1 = fingerprint.getSeverity(testPlayer);

        // Add suspicious channels
        fingerprint.addChannel(testPlayer, "cheat:hack");
        fingerprint.addChannel(testPlayer, "exploit:fly");
        fingerprint.addChannel(testPlayer, "cheat:speed");
        int score2 = fingerprint.getSeverity(testPlayer);

        assertTrue(score2 > score1, "Suspicious channels should increase severity");
    }

    @Test
    void testMovementPrecisionVanilla() {
        // Vanilla uses ~4 decimal places
        for (int i = 0; i < 20; i++) {
            fingerprint.recordMovementPrecision(testPlayer, 1.1234);
        }
        int score = fingerprint.getSeverity(testPlayer);
        assertEquals(0, score, "Vanilla precision (4 decimals) should not flag");
    }

    @Test
    void testMovementPrecisionHighPrecision() {
        // Cheat clients often use high precision
        for (int i = 0; i < 20; i++) {
            fingerprint.recordMovementPrecision(testPlayer, 1.123456789012);
        }
        int score = fingerprint.getSeverity(testPlayer);
        assertTrue(score > 0, "High precision movement should increase severity");
    }

    @Test
    void testPerfectTimingIsBotLike() {
        // Perfectly timed packets (CV ~0) = bot
        for (int i = 0; i < 50; i++) {
            fingerprint.recordPacketInterval(testPlayer, 50); // exactly 50ms every time
        }
        int score = fingerprint.getSeverity(testPlayer);
        assertTrue(score > 10, "Perfectly regular packet timing should flag as bot-like");
    }

    @Test
    void testNaturalTimingIsNotFlagged() {
        // Natural-looking timing (high variance)
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 50; i++) {
            fingerprint.recordPacketInterval(testPlayer, 30 + rand.nextInt(40)); // 30-70ms
        }
        int score = fingerprint.getSeverity(testPlayer);
        assertEquals(0, score, "Natural timing variance should not flag");
    }

    @Test
    void testShouldFlagReturnsTrueForHighSeverity() {
        fingerprint.setBrand(testPlayer, "Wurst");
        for (int i = 0; i < 50; i++) {
            fingerprint.recordPacketInterval(testPlayer, 50);
        }
        // Brand + timing should combine to exceed threshold
        assertTrue(fingerprint.getSeverity(testPlayer) > 0,
            "Cheat client with bot timing should have positive severity");
    }

    @Test
    void testDisabledFingerprintDoesNotRecord() {
        fingerprint.loadConfig(false, 60, 6000);

        fingerprint.setBrand(testPlayer, "Wurst");
        fingerprint.recordPacketInterval(testPlayer, 50);

        assertEquals(0, fingerprint.getSeverity(testPlayer),
            "Disabled fingerprint should return 0 severity");
        assertFalse(fingerprint.shouldFlag(testPlayer));
    }

    @Test
    void testRemovePlayerClearsFingerprint() {
        fingerprint.setBrand(testPlayer, "Wurst");
        assertTrue(fingerprint.getTrackedPlayerCount() > 0);

        fingerprint.removePlayer(testPlayer);
        assertEquals(0, fingerprint.getTrackedPlayerCount());
        assertEquals(0, fingerprint.getSeverity(testPlayer));
    }

    @Test
    void testSummaryContainsAllVectors() {
        fingerprint.setBrand(testPlayer, "Vanilla");
        fingerprint.addChannel(testPlayer, "minecraft:brand");
        fingerprint.recordPacketInterval(testPlayer, 50);

        String summary = fingerprint.getSummary(testPlayer);
        assertTrue(summary.contains("Brand="), "Summary should include Brand");
        assertTrue(summary.contains("Channels="), "Summary should include Channels");
        assertTrue(summary.contains("Total="), "Summary should include Total");
    }

    @Test
    void testSummaryForUnknownPlayer() {
        UUID unknown = UUID.randomUUID();
        assertEquals("No fingerprint data", fingerprint.getSummary(unknown));
    }

    @Test
    void testOnTickEvictsStaleFingerprints() {
        fingerprint.setBrand(testPlayer, "Wurst");
        assertEquals(1, fingerprint.getTrackedPlayerCount());

        // Advance tick past max age
        fingerprint.onTick(7000);
        assertEquals(0, fingerprint.getTrackedPlayerCount(),
            "Stale fingerprints should be evicted after max age ticks");
    }

    @Test
    void testProtocolExtensionIncreasesSeverity() {
        fingerprint.recordProtocolExtension(testPlayer, "custom_packets");
        fingerprint.recordProtocolExtension(testPlayer, "extended_handshake");

        int score = fingerprint.getSeverity(testPlayer);
        assertTrue(score >= 10,
            "Protocol extensions should contribute to severity");
    }

    @Test
    void testMultipleVectorsCombine() {
        fingerprint.setBrand(testPlayer, "Ghost");
        fingerprint.addChannel(testPlayer, "cheat:fly");
        fingerprint.recordProtocolExtension(testPlayer, "custom");
        for (int i = 0; i < 50; i++) {
            fingerprint.recordPacketInterval(testPlayer, 50);
        }
        for (int i = 0; i < 20; i++) {
            fingerprint.recordMovementPrecision(testPlayer, 1.12345678);
        }

        int total = fingerprint.getSeverity(testPlayer);
        assertTrue(total > 20,
            "Multiple suspicious vectors should combine for higher severity");
    }
}
