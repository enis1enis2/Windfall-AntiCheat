package io.windfall.anticheat.api;

import java.util.UUID;

/**
 * Immutable snapshot of a player's anti-cheat data.
 *
 * <p>Returned by {@link WindfallAPI#getPlayerData(org.bukkit.entity.Player)}.
 * This is a read-only view — modifications do not affect internal state.
 *
 * @see WindfallAPI for obtaining player data
 */
public final class PlayerData {

    private final UUID uuid;
    private final int violationLevel;
    private final boolean exempt;
    private final boolean alertsEnabled;
    private final int protocolVersion;
    private final boolean bedrockClient;

    /**
     * Creates a new PlayerData snapshot.
     *
     * @param uuid           the player's UUID
     * @param violationLevel total violation level
     * @param exempt         whether the player is exempt
     * @param alertsEnabled  whether alerts are enabled for this player
     * @param protocolVersion the client's protocol version
     * @param bedrockClient  whether the player is on Bedrock
     */
    public PlayerData(UUID uuid, int violationLevel, boolean exempt,
                      boolean alertsEnabled, int protocolVersion, boolean bedrockClient) {
        this.uuid = uuid;
        this.violationLevel = violationLevel;
        this.exempt = exempt;
        this.alertsEnabled = alertsEnabled;
        this.protocolVersion = protocolVersion;
        this.bedrockClient = bedrockClient;
    }

    /** Returns the player's UUID */
    public UUID getUuid() { return uuid; }

    /** Returns the total violation level across all checks */
    public int getViolationLevel() { return violationLevel; }

    /** Returns true if the player is exempt from all checks */
    public boolean isExempt() { return exempt; }

    /** Returns true if alert notifications are enabled for this player */
    public boolean isAlertsEnabled() { return alertsEnabled; }

    /** Returns the client's protocol version number */
    public int getProtocolVersion() { return protocolVersion; }

    /** Returns true if the player is on Bedrock Edition (via Geyser) */
    public boolean isBedrockClient() { return bedrockClient; }

    /**
     * Returns a human-readable summary of this player's data.
     *
     * @return formatted string with key data points
     */
    @Override
    public String toString() {
        return "PlayerData{" +
            "uuid=" + uuid +
            ", vl=" + violationLevel +
            ", exempt=" + exempt +
            ", alerts=" + alertsEnabled +
            ", protocol=" + protocolVersion +
            ", bedrock=" + bedrockClient +
            '}';
    }
}
