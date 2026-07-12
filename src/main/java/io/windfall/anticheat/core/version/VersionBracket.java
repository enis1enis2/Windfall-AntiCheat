package io.windfall.anticheat.core.version;

/**
 * Groups Minecraft protocol versions into logical brackets for check compatibility.
 *
 * <p>Instead of checking exact protocol numbers everywhere, checks can use brackets
 * to determine which features/mechanics are available. Brackets are ordered by
 * protocol number and have descriptive names.
 *
 * <p>Bracket definitions:
 * <ul>
 *   <li><b>LEGACY</b> (4-47): 1.7-1.8 — stance packets, legacy combat, legacy materials</li>
 *   <li><b>COMBAT</b> (107-340): 1.9-1.12 — new combat, elytra, no stance field</li>
 *   <li><b>FLAT</b> (393-498): 1.13-1.14 — flattening, swimming, 1.5 sneak height</li>
 *   <li><b>WORLD</b> (573-758): 1.15-1.18.2 — extended world height, collision fixes</li>
 *   <li><b>MODERN</b> (759-766): 1.19-1.20.4 — chat signing, config phase, data components</li>
 *   <li><b>LATEST</b> (767+): 1.21+ — input packets, new combat, latest mechanics</li>
 * </ul>
 *
 * @see VersionManager for protocol version resolution
 * @see io.windfall.anticheat.core.check.CheckData#compatVersions() for check annotations
 */
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

    /**
     * Resolves a protocol version to its bracket.
     * Returns LEGACY for pre-1.7 and LATEST for post-1.21.4.
     */
    public static VersionBracket fromProtocol(int protocol) {
        for (VersionBracket bracket : values()) {
            if (protocol >= bracket.minProtocol && protocol <= bracket.maxProtocol) {
                return bracket;
            }
        }
        if (protocol < LEGACY.minProtocol) return LEGACY;
        return LATEST;
    }

    /**
     * Returns the ordinal distance between two brackets.
     * Useful for determining if two players are on "nearby" versions (same bracket = 0).
     */
    public static int getBracketDistance(int protocolA, int protocolB) {
        VersionBracket a = fromProtocol(protocolA);
        VersionBracket b = fromProtocol(protocolB);
        return Math.abs(a.ordinal() - b.ordinal());
    }

    /** Checks if a protocol version falls within this bracket's range */
    public boolean contains(int protocol) {
        return protocol >= minProtocol && protocol <= maxProtocol;
    }

    public int getMinProtocol() { return minProtocol; }
    public int getMaxProtocol() { return maxProtocol; }
    public String getVersionRange() { return versionRange; }
    public String getDescription() { return description; }
}
