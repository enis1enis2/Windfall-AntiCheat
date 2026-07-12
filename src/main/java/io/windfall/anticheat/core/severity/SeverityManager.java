package io.windfall.anticheat.core.severity;

import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Scales VL increments based on a player's cumulative violation level.
 *
 * <p>Higher total VL → faster escalation → quicker punishments for repeat offenders.
 * This prevents cheaters from hovering just below punishment thresholds by
 * accumulating violations across multiple check types.
 *
 * <p>Severity tiers (configurable):
 * <ul>
 *   <li>LOW (default): 1.0x multiplier — base rate</li>
 *   <li>MODERATE: 1.3x — player has accumulated some violations</li>
 *   <li>HIGH: 1.6x — persistent cheater, escalating faster</li>
 *   <li>EXTREME: 2.0x — rapid escalation, approaching punishment thresholds</li>
 * </ul>
 *
 * <p>Bedrock players receive a discount multiplier (default 0.6x) applied after
 * severity scaling, because touch/controller input naturally produces more
 * borderline movement values.
 *
 * @see io.windfall.anticheat.core.check.Check#flag(WindfallPlayer) for VL increment usage
 * @see io.windfall.anticheat.core.config.WindfallConfig for tier thresholds
 */
// Scales VL increments based on player's cumulative violation level
// Higher VL → faster escalation → quicker punishments for repeat offenders
public class SeverityManager {

    private final boolean enabled;
    private final int moderateVl;       // Threshold for moderate severity
    private final int highVl;           // Threshold for high severity
    private final int extremeVl;        // Threshold for extreme severity
    private final double moderateMultiplier;  // e.g. 1.3x at moderate
    private final double highMultiplier;      // e.g. 1.6x at high
    private final double extremeMultiplier;   // e.g. 2.0x at extreme
    // Bedrock players get reduced multipliers because touch/controller input is less precise
    private final double bedrockDiscount;

    public SeverityManager(boolean enabled, int moderateVl, int highVl, int extremeVl,
                           double moderateMultiplier, double highMultiplier,
                           double extremeMultiplier, double bedrockDiscount) {
        this.enabled = enabled;
        this.moderateVl = moderateVl;
        this.highVl = highVl;
        this.extremeVl = extremeVl;
        this.moderateMultiplier = moderateMultiplier;
        this.highMultiplier = highMultiplier;
        this.extremeMultiplier = extremeMultiplier;
        this.bedrockDiscount = bedrockDiscount;
    }

    /** Creates a SeverityManager from config values */
    public static SeverityManager fromConfig(io.windfall.anticheat.core.config.WindfallConfig config) {
        return new SeverityManager(
            config.isSeverityEnabled(),
            config.getSeverityModerateVl(),
            config.getSeverityHighVl(),
            config.getSeverityExtremeVl(),
            config.getSeverityModerateMultiplier(),
            config.getSeverityHighMultiplier(),
            config.getSeverityExtremeMultiplier(),
            config.getSeverityBedrockDiscount()
        );
    }

    /**
     * Returns the multiplier applied to VL increments for this player.
     *
     * <p>Bedrock discount is applied after severity scaling — both multiply together.
     * For example, an EXTREME Bedrock player gets 2.0 × 0.6 = 1.2x effective multiplier.
     *
     * @param player the player to evaluate
     * @return the combined severity × bedrock multiplier (1.0 if disabled)
     */
    // Returns the multiplier applied to VL increments for this player
    // Bedrock discount is applied after severity scaling — both multiply together
    public double getSeverityMultiplier(WindfallPlayer player) {
        if (!enabled) return 1.0;

        int totalVl = player.getTotalViolationLevel();
        double multiplier;

        if (totalVl >= extremeVl) {
            multiplier = extremeMultiplier;
        } else if (totalVl >= highVl) {
            multiplier = highMultiplier;
        } else if (totalVl >= moderateVl) {
            multiplier = moderateMultiplier;
        } else {
            multiplier = 1.0;
        }

        if (player.isBedrock()) {
            multiplier *= bedrockDiscount;
        }

        return multiplier;
    }

    /**
     * Returns the rounded VL increment for this player (minimum 1).
     *
     * <p>Called by {@link io.windfall.anticheat.core.check.Check#flag(WindfallPlayer)}
     * to determine how many violation points to add.
     */
    public int getScaledVlIncrement(WindfallPlayer player) {
        if (!enabled) return 1;
        double multiplier = getSeverityMultiplier(player);
        return Math.max(1, (int) Math.round(multiplier));
    }

    /**
     * Returns a human-readable severity label for display in alerts and commands.
     *
     * @return "LOW", "MODERATE", "HIGH", "EXTREME", or "Disabled"
     */
    public String getSeverityLabel(WindfallPlayer player) {
        if (!enabled) return "Disabled";
        int totalVl = player.getTotalViolationLevel();
        if (totalVl >= extremeVl) return "EXTREME";
        if (totalVl >= highVl) return "HIGH";
        if (totalVl >= moderateVl) return "MODERATE";
        return "LOW";
    }

    public boolean isEnabled() { return enabled; }
    public int getModerateVl() { return moderateVl; }
    public int getHighVl() { return highVl; }
    public int getExtremeVl() { return extremeVl; }
    public double getModerateMultiplier() { return moderateMultiplier; }
    public double getHighMultiplier() { return highMultiplier; }
    public double getExtremeMultiplier() { return extremeMultiplier; }
    public double getBedrockDiscount() { return bedrockDiscount; }
}
