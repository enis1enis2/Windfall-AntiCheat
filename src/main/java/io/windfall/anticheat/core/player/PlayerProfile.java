package io.windfall.anticheat.core.player;

import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.version.VersionBracket;

/**
 * Immutable snapshot of a player's version and platform profile.
 *
 * <p>Computed once at login from {@link BedrockInfo} and ViaVersion data.
 * Used by checks to apply version-specific tolerances (e.g., Bedrock touch
 * input gets wider aim thresholds, legacy clients get relaxed speed limits).
 *
 * <p>The {@link #getCombinedToleranceMultiplier()} method returns the product
 * of Bedrock input mode tolerance and version-gap tolerance — checks multiply
 * their thresholds by this value to avoid false positives.
 *
 * @see VersionBracket#fromProtocol(int) for version classification
 * @see VersionBracket#getBracketDistance(int, int) for version gap calculation
 */
public final class PlayerProfile {

    private final int clientProtocol;
    private final VersionBracket clientBracket;
    private final boolean isBedrock;
    private final String bedrockInputMode;
    private final String bedrockDeviceOs;
    private final boolean isViaVersionClient;
    private final int versionGap;

    /**
     * Constructs a player profile from login-time data.
     *
     * @param serverProtocol the server's protocol version
     * @param clientProtocol the client's protocol version (may differ if ViaVersion is present)
     * @param bedrockInfo    Bedrock player info from Geyser, or null for Java clients
     * @param isViaVersion   true if the client is connecting through ViaVersion
     */
    public PlayerProfile(int serverProtocol, int clientProtocol, BedrockInfo bedrockInfo, boolean isViaVersion) {
        this.clientProtocol = clientProtocol;
        this.clientBracket = VersionBracket.fromProtocol(clientProtocol);
        this.isBedrock = bedrockInfo != null;
        this.bedrockInputMode = bedrockInfo != null ? bedrockInfo.inputMode() : null;
        this.bedrockDeviceOs = bedrockInfo != null ? bedrockInfo.deviceOs() : null;
        this.isViaVersionClient = isViaVersion;
        this.versionGap = VersionBracket.getBracketDistance(serverProtocol, clientProtocol);
    }

    /**
     * Returns true if the client's version bracket differs from the server's.
     * Used to activate ViaVersion-sensitive check relaxation.
     */
    public boolean isVersionMismatch(int serverProtocol) {
        return VersionBracket.fromProtocol(serverProtocol) != clientBracket;
    }

    /**
     * Returns a tolerance multiplier based on Bedrock input mode.
     *
     * <p>Touch screens have the least precision (1.15x), controllers are moderate
     * (1.10x), and keyboard-mouse Bedrock gets the smallest boost (1.05x).
     * Java clients always return 1.0.
     */
    public double getBedrockToleranceMultiplier() {
        if (!isBedrock) return 1.0;
        if ("TOUCH".equals(bedrockInputMode)) return 1.15;
        if ("CONTROLLER".equals(bedrockInputMode)) return 1.10;
        return 1.05;
    }

    /**
     * Returns a tolerance multiplier based on the version gap between client and server.
     *
     * <p>Each version bracket of distance adds 5% tolerance. For example,
     * a 1.8 client on a 1.21 server has a gap of ~4 brackets → 1.20x multiplier.
     * Same-version clients return 1.0.
     */
    public double getVersionGapMultiplier() {
        // Wider version gap = wider tolerance needed for physics prediction
        if (versionGap <= 0) return 1.0;
        return 1.0 + (versionGap * 0.05);
    }

    /**
     * Returns the combined tolerance multiplier (Bedrock × version gap).
     * Checks should multiply their detection thresholds by this value.
     */
    public double getCombinedToleranceMultiplier() {
        return getBedrockToleranceMultiplier() * getVersionGapMultiplier();
    }

    /** Returns true if the Bedrock player is using a touch device */
    public boolean isTouchDevice() {
        return "TOUCH".equals(bedrockInputMode);
    }

    /** Returns true if the Bedrock player is using a controller */
    public boolean isController() {
        return "CONTROLLER".equals(bedrockInputMode);
    }

    /** Returns true if the Bedrock player is using keyboard+mouse (cross-play input) */
    public boolean isBedrockKeyboard() {
        return isBedrock && "KEYBOARD_MOUSE".equals(bedrockInputMode);
    }

    public int getClientProtocol() { return clientProtocol; }
    public VersionBracket getClientBracket() { return clientBracket; }
    public boolean isBedrock() { return isBedrock; }
    public String getBedrockInputMode() { return bedrockInputMode; }
    public String getBedrockDeviceOs() { return bedrockDeviceOs; }
    public boolean isViaVersionClient() { return isViaVersionClient; }
    public int getVersionGap() { return versionGap; }
}
