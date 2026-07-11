package io.windfall.anticheat.core.version;

import org.bukkit.Bukkit;

public class VersionManager {

    private final String serverVersion;
    private final int protocolVersion;

    public VersionManager() {
        this.serverVersion = Bukkit.getBukkitVersion();
        this.protocolVersion = parseProtocolVersion(serverVersion);
    }

    private int parseProtocolVersion(String version) {
        // Bukkit version strings look like "1.20.4-R0.1-SNAPSHOT"; split for the MC version
        String mcVersion = version.split("-")[0];
        return mapVersionToProtocol(mcVersion);
    }

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
                // Unknown future versions get an estimated protocol number
                try {
                    String[] ver = mcVersion.split("\\.");
                    int major = Integer.parseInt(ver[0]);
                    int minor = Integer.parseInt(ver[1]);
                    return 700 + (major * 100) + minor;
                } catch (Exception e) {
                    return 999;
                }
        }
    }

    public String getServerVersion() {
        return serverVersion;
    }

    // Protocol 393 = 1.13, the "flattening" update that changed all internal IDs
    public boolean isLegacy() {
        return protocolVersion < 393;
    }

    // Protocol 763 = 1.20.1, new world height and movement code
    public boolean isModern() {
        return protocolVersion >= 763;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }
}
