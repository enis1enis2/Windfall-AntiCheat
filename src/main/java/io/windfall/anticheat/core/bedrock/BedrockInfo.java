package io.windfall.anticheat.core.bedrock;

import java.util.Objects;

public final class BedrockInfo {

    private final String deviceOs;
    private final String inputMode;
    private final String uiProfile;
    private final String clientVersion;
    private final String languageCode;

    public BedrockInfo(String deviceOs, String inputMode, String uiProfile, String clientVersion, String languageCode) {
        this.deviceOs = deviceOs;
        this.inputMode = inputMode;
        this.uiProfile = uiProfile;
        this.clientVersion = clientVersion;
        this.languageCode = languageCode;
    }

    public String deviceOs() { return deviceOs; }
    public String inputMode() { return inputMode; }
    public String uiProfile() { return uiProfile; }
    public String clientVersion() { return clientVersion; }
    public String languageCode() { return languageCode; }

    public boolean isTouchDevice() {
        return "TOUCH".equals(inputMode);
    }

    public boolean isMobileOs() {
        return "ANDROID".equals(deviceOs) || "IOS".equals(deviceOs);
    }

    public boolean isConsole() {
        return "XBOX".equals(deviceOs) || "PS4".equals(deviceOs) || "SWITCH".equals(deviceOs);
    }

    public boolean isBedrockKeyboard() {
        return "KEYBOARD_MOUSE".equals(inputMode);
    }

    public boolean isController() {
        return "CONTROLLER".equals(inputMode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BedrockInfo that = (BedrockInfo) o;
        return Objects.equals(deviceOs, that.deviceOs) && Objects.equals(inputMode, that.inputMode)
            && Objects.equals(uiProfile, that.uiProfile) && Objects.equals(clientVersion, that.clientVersion)
            && Objects.equals(languageCode, that.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceOs, inputMode, uiProfile, clientVersion, languageCode);
    }

    @Override
    public String toString() {
        return "BedrockInfo{" + deviceOs + ", " + inputMode + ", " + uiProfile + ", v" + clientVersion + "}";
    }
}
