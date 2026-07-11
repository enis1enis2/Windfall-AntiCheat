package io.windfall.anticheat.core.severity;

import io.windfall.anticheat.core.player.WindfallPlayer;

public class SeverityManager {

    private final boolean enabled;
    private final int moderateVl;
    private final int highVl;
    private final int extremeVl;
    private final double moderateMultiplier;
    private final double highMultiplier;
    private final double extremeMultiplier;
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

    public int getScaledVlIncrement(WindfallPlayer player) {
        if (!enabled) return 1;
        double multiplier = getSeverityMultiplier(player);
        return Math.max(1, (int) Math.round(multiplier));
    }

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
