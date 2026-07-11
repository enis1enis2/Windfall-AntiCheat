package io.windfall.anticheat.core.player;

import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.version.VersionBracket;

public final class PlayerProfile {

    private final int clientProtocol;
    private final VersionBracket clientBracket;
    private final boolean isBedrock;
    private final String bedrockInputMode;
    private final String bedrockDeviceOs;
    private final boolean isViaVersionClient;
    private final int versionGap;

    public PlayerProfile(int serverProtocol, int clientProtocol, BedrockInfo bedrockInfo, boolean isViaVersion) {
        this.clientProtocol = clientProtocol;
        this.clientBracket = VersionBracket.fromProtocol(clientProtocol);
        this.isBedrock = bedrockInfo != null;
        this.bedrockInputMode = bedrockInfo != null ? bedrockInfo.inputMode() : null;
        this.bedrockDeviceOs = bedrockInfo != null ? bedrockInfo.deviceOs() : null;
        this.isViaVersionClient = isViaVersion;
        this.versionGap = VersionBracket.getBracketDistance(serverProtocol, clientProtocol);
    }

    public boolean isVersionMismatch(int serverProtocol) {
        return VersionBracket.fromProtocol(serverProtocol) != clientBracket;
    }

    public double getBedrockToleranceMultiplier() {
        if (!isBedrock) return 1.0;
        if ("TOUCH".equals(bedrockInputMode)) return 1.15;
        if ("CONTROLLER".equals(bedrockInputMode)) return 1.10;
        return 1.05;
    }

    public double getVersionGapMultiplier() {
        // Wider version gap = wider tolerance needed for physics prediction
        if (versionGap <= 0) return 1.0;
        return 1.0 + (versionGap * 0.05);
    }

    public double getCombinedToleranceMultiplier() {
        return getBedrockToleranceMultiplier() * getVersionGapMultiplier();
    }

    public boolean isTouchDevice() {
        return "TOUCH".equals(bedrockInputMode);
    }

    public boolean isController() {
        return "CONTROLLER".equals(bedrockInputMode);
    }

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
