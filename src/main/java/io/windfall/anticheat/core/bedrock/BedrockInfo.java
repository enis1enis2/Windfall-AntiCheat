package io.windfall.anticheat.core.bedrock;

import java.util.Objects;

/**
 * Immutable snapshot of a Bedrock player's device information from Floodgate.
 *
 * <p>Retrieved via {@link GeyserManager#getBedrockInfo(UUID)} and used by anti-cheat checks
 * to apply platform-specific thresholds. For example, touch input has higher aim-snap tolerance
 * than keyboard/mouse input, and mobile devices may have higher movement latency.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@link #deviceOs} &mdash; operating system (ANDROID, IOS, XBOX, PS4, SWITCH, WINDOWS)</li>
 *   <li>{@link #inputMode} &mdash; input method (KEYBOARD_MOUSE, TOUCH, CONTROLLER)</li>
 *   <li>{@link #uiProfile} &mdash; UI layout (TOUCH, CLASSIC, POCKET, PLUS)</li>
 *   <li>{@link #clientVersion} &mdash; Bedrock client version string (e.g., "1.21.50")</li>
 *   <li>{@link #languageCode} &mdash; client language (e.g., "en_US")</li>
 * </ul>
 *
 * <p>All fields may be "UNKNOWN" if the Floodgate API is not available or the player
 * has restricted their information sharing settings.
 *
 * @see GeyserManager for how this info is retrieved via reflection
 * @see GeysersTracker for Bedrock player movement adjustments
 */
// Immutable snapshot of a Bedrock player's device info from Floodgate
// Used by checks to apply platform-specific thresholds (touch has higher aim-snap tolerance)
public final class BedrockInfo {

    private final String deviceOs;      // ANDROID, IOS, XBOX, PS4, SWITCH, WINDOWS
    private final String inputMode;     // KEYBOARD_MOUSE, TOUCH, CONTROLLER
    private final String uiProfile;     // TOUCH, CLASSIC, POCKET, PLUS
    private final String clientVersion; // e.g. "1.21.50"
    private final String languageCode;  // e.g. "en_US"

    /**
     * Creates an immutable Bedrock device info snapshot.
     *
     * @param deviceOs      the operating system (ANDROID, IOS, XBOX, PS4, SWITCH, WINDOWS, UNKNOWN)
     * @param inputMode     the input method (KEYBOARD_MOUSE, TOUCH, CONTROLLER, UNKNOWN)
     * @param uiProfile     the UI layout profile (TOUCH, CLASSIC, POCKET, PLUS, UNKNOWN)
     * @param clientVersion the Bedrock client version string (e.g., "1.21.50")
     * @param languageCode  the client language code (e.g., "en_US")
     */
    public BedrockInfo(String deviceOs, String inputMode, String uiProfile, String clientVersion, String languageCode) {
        this.deviceOs = deviceOs;
        this.inputMode = inputMode;
        this.uiProfile = uiProfile;
        this.clientVersion = clientVersion;
        this.languageCode = languageCode;
    }

    /** Returns the device operating system (ANDROID, IOS, XBOX, PS4, SWITCH, WINDOWS) */
    public String deviceOs() { return deviceOs; }
    /** Returns the input mode (KEYBOARD_MOUSE, TOUCH, CONTROLLER) */
    public String inputMode() { return inputMode; }
    /** Returns the UI profile (TOUCH, CLASSIC, POCKET, PLUS) */
    public String uiProfile() { return uiProfile; }
    /** Returns the Bedrock client version string */
    public String clientVersion() { return clientVersion; }
    /** Returns the client language code */
    public String languageCode() { return languageCode; }

    /**
     * Checks if the player is using touch input (mobile phone/tablet).
     * Touch players have different aim characteristics and movement patterns.
     *
     * @return true if inputMode is TOUCH
     */
    public boolean isTouchDevice() {
        return "TOUCH".equals(inputMode);
    }

    /**
     * Checks if the player is on a mobile OS (Android or iOS).
     *
     * @return true if deviceOs is ANDROID or IOS
     */
    public boolean isMobileOs() {
        return "ANDROID".equals(deviceOs) || "IOS".equals(deviceOs);
    }

    /**
     * Checks if the player is on a gaming console (Xbox, PlayStation, or Nintendo Switch).
     * Console players use controller input with aim assist, requiring different thresholds.
     *
     * @return true if deviceOs is XBOX, PS4, or SWITCH
     */
    public boolean isConsole() {
        return "XBOX".equals(deviceOs) || "PS4".equals(deviceOs) || "SWITCH".equals(deviceOs);
    }

    /**
     * Checks if the player uses keyboard and mouse on Bedrock.
     * These players have similar input characteristics to Java Edition players.
     *
     * @return true if inputMode is KEYBOARD_MOUSE
     */
    public boolean isBedrockKeyboard() {
        return "KEYBOARD_MOUSE".equals(inputMode);
    }

    /**
     * Checks if the player uses a controller input device.
     * Controller input has built-in aim smoothing and different acceleration curves.
     *
     * @return true if inputMode is CONTROLLER
     */
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
