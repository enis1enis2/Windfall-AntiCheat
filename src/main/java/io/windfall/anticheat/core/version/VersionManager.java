package io.windfall.anticheat.core.version;

import org.bukkit.Bukkit;

/**
 * Maps the running server's Bukkit version string to a protocol version number.
 *
 * <p>Parses {@link org.bukkit.Bukkit#getBukkitVersion()} (e.g., "1.20.4-R0.1-SNAPSHOT"
 * or "26.1.2-R0.1-SNAPSHOT") and returns the equivalent protocol version number.
 * Protocol versions are used throughout the check system for version-dependent physics.
 *
 * <p>Supports both legacy 1.x versioning (1.7-1.21.4) and Mojang's new year-based
 * versioning (26.x, 27.x, ...) introduced in 2026.
 *
 * @see VersionBracket for grouped version ranges
 * @see io.windfall.anticheat.core.physics.VersionPhysics for protocol-based branching
 */
public class VersionManager {

    private final String serverVersion;
    private final int protocolVersion;

    /** Parses the protocol version from Bukkit's version string at startup */
    public VersionManager() {
        this.serverVersion = Bukkit.getBukkitVersion();
        this.protocolVersion = parseProtocolVersion(serverVersion);
    }

    private int parseProtocolVersion(String version) {
        // Bukkit version strings look like "1.20.4-R0.1-SNAPSHOT"; split for the MC version
        String mcVersion = version.split("-")[0];
        return mapVersionToProtocol(mcVersion);
    }

    /**
     * Maps a Minecraft version string to its protocol version number.
     * Handles legacy 1.x, year-based 26.x+, and unmapped future versions.
     */
    private int mapVersionToProtocol(String mcVersion) {
        switch (mcVersion) {
            case "1.7.5": return 4;
            case "1.7.10": return 5;
            case "1.8":
            case "1.8.8": return 47;
            case "1.9":
            case "1.9.4": return 110;
            case "1.10":
            case "1.10.2": return 210;
            case "1.11":
            case "1.11.2": return 316;
            case "1.12":
            case "1.12.2": return 340;
            case "1.13":
            case "1.13.2": return 404;
            case "1.14":
            case "1.14.4": return 498;
            case "1.15":
            case "1.15.2": return 578;
            case "1.16.1": return 736;
            case "1.16.4":
            case "1.16.5": return 754;
            case "1.17":
            case "1.17.1": return 756;
            case "1.18":
            case "1.18.2": return 758;
            case "1.19":
            case "1.19.2": return 760;
            case "1.19.3": return 761;
            case "1.19.4": return 762;
            case "1.20":
            case "1.20.1": return 763;
            case "1.20.2": return 764;
            case "1.20.3":
            case "1.20.4": return 765;
            case "1.20.5":
            case "1.20.6": return 766;
            case "1.21":
            case "1.21.1": return 767;
            case "1.21.2":
            case "1.21.3": return 768;
            case "1.21.4": return 769;
            default:
                // Handle new year-based versioning (26.1.x, 26.2.x, etc.)
                // Mojang switched from 1.x to year-based in 2026
                try {
                    String[] ver = mcVersion.split("\\.");
                    int major = Integer.parseInt(ver[0]);
                    int minor = Integer.parseInt(ver[1]);
                    // Year-based versions (>=26): map to protocol above legacy ceiling
                    if (major >= 26) {
                        return 800 + (major - 26) * 10 + minor;
                    }
                    // Legacy 1.x fallback for unmapped versions
                    return 700 + (major * 100) + minor;
                } catch (Exception e) {
                    return 999;
                }
        }
    }

    /** Returns the raw Bukkit version string (e.g., "1.20.4-R0.1-SNAPSHOT") */
    public String getServerVersion() {
        return serverVersion;
    }

    /** Whether this is a pre-flattening server (before 1.13, protocol <393) */
    public boolean isLegacy() {
        return protocolVersion < 393;
    }

    /** Whether this is a modern server (1.20.1+, protocol >=763) */
    public boolean isModern() {
        return protocolVersion >= 763;
    }

    /** Returns the numeric protocol version for the running server */
    public int getProtocolVersion() {
        return protocolVersion;
    }
}
