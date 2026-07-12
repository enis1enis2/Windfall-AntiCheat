package io.windfall.anticheat.core.version;

public enum VersionBracket {

    LEGACY(4, 47, "1.7-1.8", "Stance packets, legacy combat, legacy materials"),
    COMBAT(107, 340, "1.9-1.12", "New combat, elytra, no stance field"),
    FLAT(393, 498, "1.13-1.14", "Flattening, swimming, 1.5 sneak"),
    WORLD(573, 758, "1.15-1.18.2", "Extended world height, collision fixes"),
    MODERN(759, 766, "1.19-1.20.4", "Chat signing, config phase, data components"),
    LATEST(767, 99999, "1.21+", "Input packets, new combat, latest mechanics");

    private final int minProtocol;
    private final int maxProtocol;
    private final String versionRange;
    private final String description;

    VersionBracket(int minProtocol, int maxProtocol, String versionRange, String description) {
        this.minProtocol = minProtocol;
        this.maxProtocol = maxProtocol;
        this.versionRange = versionRange;
        this.description = description;
    }

    public static VersionBracket fromProtocol(int protocol) {
        for (VersionBracket bracket : values()) {
            if (protocol >= bracket.minProtocol && protocol <= bracket.maxProtocol) {
                return bracket;
            }
        }
        if (protocol < LEGACY.minProtocol) return LEGACY;
        return LATEST;
    }

    public static int getBracketDistance(int protocolA, int protocolB) {
        VersionBracket a = fromProtocol(protocolA);
        VersionBracket b = fromProtocol(protocolB);
        return Math.abs(a.ordinal() - b.ordinal());
    }

    public boolean contains(int protocol) {
        return protocol >= minProtocol && protocol <= maxProtocol;
    }

    public int getMinProtocol() { return minProtocol; }
    public int getMaxProtocol() { return maxProtocol; }
    public String getVersionRange() { return versionRange; }
    public String getDescription() { return description; }
}
