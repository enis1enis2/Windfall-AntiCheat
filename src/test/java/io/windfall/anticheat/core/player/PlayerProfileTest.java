package io.windfall.anticheat.core.player;

import static org.junit.jupiter.api.Assertions.*;

import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.version.VersionBracket;
import org.junit.jupiter.api.Test;

class PlayerProfileTest {

    // --- helpers ---

    private static PlayerProfile java(int server, int client, boolean via) {
        return new PlayerProfile(server, client, null, via);
    }

    private static PlayerProfile bedrock(int server, int client, String inputMode) {
        BedrockInfo info = new BedrockInfo("ANDROID", inputMode, "CLASSIC", "1.21.50", "en_US");
        return new PlayerProfile(server, client, info, false);
    }

    private static BedrockInfo bedrockInfo(String inputMode) {
        return new BedrockInfo("ANDROID", inputMode, "CLASSIC", "1.21.50", "en_US");
    }

    // --- 1. Constructor with known values ---

    @Test
    void constructor_setsAllFields() {
        PlayerProfile profile = new PlayerProfile(760, 769, null, false);

        assertEquals(769, profile.getClientProtocol());
        assertEquals(VersionBracket.LATEST, profile.getClientBracket());
        assertFalse(profile.isBedrock());
        assertFalse(profile.isViaVersionClient());
        assertFalse(profile.isTouchDevice());
        assertFalse(profile.isController());
        assertFalse(profile.isBedrockKeyboard());
    }

    // --- 2. Constructor with BedrockInfo ---

    @Test
    void constructor_bedrockProfile() {
        BedrockInfo info = bedrockInfo("TOUCH");
        PlayerProfile profile = new PlayerProfile(760, 769, info, false);

        assertTrue(profile.isBedrock());
        assertEquals("TOUCH", profile.getBedrockInputMode());
        assertEquals("ANDROID", profile.getBedrockDeviceOs());
        assertTrue(profile.isTouchDevice());
        assertFalse(profile.isController());
        assertFalse(profile.isBedrockKeyboard());
    }

    @Test
    void constructor_nullBedrockInfo_isNotBedrock() {
        PlayerProfile profile = new PlayerProfile(760, 769, null, false);

        assertFalse(profile.isBedrock());
        assertNull(profile.getBedrockInputMode());
        assertNull(profile.getBedrockDeviceOs());
    }

    // --- 3. getClientProtocol ---

    @Test
    void getClientProtocol_returnsCorrectValue() {
        PlayerProfile profile = java(760, 769, false);
        assertEquals(769, profile.getClientProtocol());
    }

    // --- 4. getClientBracket ---

    @Test
    void getClientBracket_returnsCorrectBracket() {
        PlayerProfile profile = java(760, 769, false);
        assertEquals(VersionBracket.LATEST, profile.getClientBracket());
    }

    // --- 5. isBedrock ---

    @Test
    void isBedrock_falseForJavaPlayer() {
        assertFalse(java(760, 769, false).isBedrock());
    }

    @Test
    void isBedrock_trueForBedrockPlayer() {
        assertTrue(bedrock(760, 769, "KEYBOARD_MOUSE").isBedrock());
    }

    // --- 6. isViaVersionClient ---

    @Test
    void isViaVersionClient_true() {
        assertTrue(java(760, 769, true).isViaVersionClient());
    }

    @Test
    void isViaVersionClient_false() {
        assertFalse(java(760, 769, false).isViaVersionClient());
    }

    // --- 7. getVersionGap ---

    @Test
    void getVersionGap_zeroWhenSameBracket() {
        PlayerProfile profile = java(769, 769, false);
        assertEquals(0, profile.getVersionGap());
    }

    @Test
    void getVersionGap_oneForAdjacentBrackets() {
        // server=760 (MODERN, ord=4), client=769 (LATEST, ord=5)
        PlayerProfile profile = java(760, 769, false);
        assertEquals(1, profile.getVersionGap());
    }

    @Test
    void getVersionGap_wideGap() {
        // server=769 (LATEST, ord=5), client=47 (LEGACY, ord=0)
        PlayerProfile profile = java(769, 47, false);
        assertEquals(5, profile.getVersionGap());
    }

    // --- 8. getBedrockToleranceMultiplier ---

    @Test
    void getBedrockToleranceMultiplier_javaPlayer_returns1() {
        PlayerProfile profile = java(769, 769, false);
        assertEquals(1.0, profile.getBedrockToleranceMultiplier());
    }

    @Test
    void getBedrockToleranceMultiplier_bedrockTouch_returns1_15() {
        PlayerProfile profile = bedrock(769, 769, "TOUCH");
        assertEquals(1.15, profile.getBedrockToleranceMultiplier());
    }

    @Test
    void getBedrockToleranceMultiplier_bedrockController_returns1_10() {
        PlayerProfile profile = bedrock(769, 769, "CONTROLLER");
        assertEquals(1.10, profile.getBedrockToleranceMultiplier());
    }

    @Test
    void getBedrockToleranceMultiplier_bedrockKeyboard_returns1_05() {
        PlayerProfile profile = bedrock(769, 769, "KEYBOARD_MOUSE");
        assertEquals(1.05, profile.getBedrockToleranceMultiplier());
    }

    @Test
    void getBedrockToleranceMultiplier_bedrock_any_returnsGreaterThan1() {
        PlayerProfile profile = bedrock(769, 769, "TOUCH");
        assertTrue(profile.getBedrockToleranceMultiplier() > 1.0);
    }

    // --- 9. getCombinedToleranceMultiplier ---

    @Test
    void getCombinedToleranceMultiplier_javaPlayer_noGap_returns1() {
        PlayerProfile profile = java(769, 769, false);
        assertEquals(1.0, profile.getCombinedToleranceMultiplier());
    }

    @Test
    void getCombinedToleranceMultiplier_bedrockPlayer_returnsGreaterThan1() {
        PlayerProfile profile = bedrock(769, 769, "TOUCH");
        assertTrue(profile.getCombinedToleranceMultiplier() > 1.0);
    }

    @Test
    void getCombinedToleranceMultiplier_javaPlayer_withVersionGap_returnsGreaterThan1() {
        // server=760 (MODERN), client=769 (LATEST) → gap=1
        PlayerProfile profile = java(760, 769, false);
        assertTrue(profile.getCombinedToleranceMultiplier() > 1.0);
        // gap multiplier = 1.0 + (1 * 0.05) = 1.05
        assertEquals(1.05, profile.getCombinedToleranceMultiplier());
    }

    @Test
    void getCombinedToleranceMultiplier_bedrockWithGap_multipliesBoth() {
        // Bedrock TOUCH (1.15) * gap=1 (1.05) = 1.2075
        PlayerProfile profile = bedrock(760, 769, "TOUCH");
        assertEquals(1.2075, profile.getCombinedToleranceMultiplier());
    }

    // --- 10. getVersionGapMultiplier ---

    @Test
    void getVersionGapMultiplier_zeroGap_returns1() {
        PlayerProfile profile = java(769, 769, false);
        assertEquals(1.0, profile.getVersionGapMultiplier());
    }

    @Test
    void getVersionGapMultiplier_gapOf2() {
        // server=769 (LATEST, ord=5), client=47 (LEGACY, ord=0) → gap=5
        PlayerProfile profile = java(769, 47, false);
        assertEquals(1.0 + (5 * 0.05), profile.getVersionGapMultiplier());
    }

    // --- 11. toString ---

    @Test
    void toString_returnsNonNull() {
        PlayerProfile profile = java(760, 769, false);
        assertNotNull(profile.toString());
    }

    // --- 12. isVersionMismatch ---

    @Test
    void isVersionMismatch_sameServer_returnsFalse() {
        PlayerProfile profile = java(769, 769, false);
        assertFalse(profile.isVersionMismatch(769));
    }

    @Test
    void isVersionMismatch_differentServer_returnsTrue() {
        PlayerProfile profile = java(760, 769, false);
        assertTrue(profile.isVersionMismatch(760));
    }

    // --- 13. Bracket mapping for known protocols ---

    @Test
    void bracketMapping_protocol47_isLEGACY() {
        assertEquals(VersionBracket.LEGACY, VersionBracket.fromProtocol(47));
    }

    @Test
    void bracketMapping_protocol110_isCOMBAT() {
        assertEquals(VersionBracket.COMBAT, VersionBracket.fromProtocol(110));
    }

    @Test
    void bracketMapping_protocol404_isFLAT() {
        // 404 is the FLAT bracket minimum — protocol 393 falls in the gap
        // between COMBAT (107-340) and FLAT (404-498) and returns LATEST
        // as a fallback. Protocol 404 is the first valid FLAT protocol.
        assertEquals(VersionBracket.FLAT, VersionBracket.fromProtocol(404));
    }

    @Test
    void bracketMapping_protocol393_isFLAT() {
        PlayerProfile profile = new PlayerProfile(393, 393, null, false);
        assertEquals(VersionBracket.FLAT, profile.getClientBracket());
    }

    @Test
    void bracketMapping_protocol573_isWORLD() {
        assertEquals(VersionBracket.WORLD, VersionBracket.fromProtocol(573));
    }

    @Test
    void bracketMapping_protocol760_isMODERN() {
        assertEquals(VersionBracket.MODERN, VersionBracket.fromProtocol(760));
    }

    @Test
    void bracketMapping_protocol769_isLATEST() {
        assertEquals(VersionBracket.LATEST, VersionBracket.fromProtocol(769));
    }

    // --- 14. Input mode edge cases ---

    @Test
    void inputMode_null_isNotBedrock() {
        PlayerProfile profile = new PlayerProfile(769, 769, null, false);
        assertFalse(profile.isTouchDevice());
        assertFalse(profile.isController());
        assertFalse(profile.isBedrockKeyboard());
        assertEquals(1.0, profile.getBedrockToleranceMultiplier());
    }
}
