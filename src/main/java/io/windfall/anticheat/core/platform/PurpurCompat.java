package io.windfall.anticheat.core.platform;

import io.windfall.anticheat.core.version.ServerFork;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Purpur compatibility layer — reads custom knockback settings from purpur.yml.
 *
 * <p>Purpur allows server admins to customise knockback multipliers, which affects
 * the VelocityCheck false positive rate. This class reads the relevant values at
 * startup and provides adjustment methods for the check to apply.
 *
 * <p>If purpur.yml is missing or unreadable, all multipliers default to 1.0 (vanilla).
 *
 * @see io.windfall.anticheat.core.check.impl.movement.VelocityCheck for usage
 */
public final class PurpurCompat {

    private volatile boolean isPurpur;
    private volatile boolean customKnockbackEnabled;
    private volatile double attackKnockbackMultiplier = 1.0;
    private volatile double knockbackVerticalMultiplier = 1.0;

    /** Reads knockback settings from purpur.yml if running on Purpur */
    public void init(ServerFork fork, Logger logger) {
        this.isPurpur = fork.isPurpur();
        if (!isPurpur) return;

        try {
            File purpurConfig = new File("purpur.yml");
            if (!purpurConfig.exists()) {
                logger.warning("[Windfall] purpur.yml not found — using vanilla knockback values");
                return;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(purpurConfig);

            // Read knockback settings from purpur.yml
            double attackKB = config.getDouble("knockback.attack-knockback", 1.0);
            double verticalKB = config.getDouble("knockback.knockback-vertical", 1.0);

            if (attackKB != 1.0 || verticalKB != 1.0) {
                customKnockbackEnabled = true;
                attackKnockbackMultiplier = attackKB;
                knockbackVerticalMultiplier = verticalKB;
                logger.info("[Windfall] Purpur custom knockback detected: attack=" + attackKB + ", vertical=" + verticalKB);
            } else {
                logger.info("[Windfall] Purpur detected but using vanilla knockback values");
            }
        } catch (Exception e) {
            logger.info("[Windfall] Could not read purpur.yml — using vanilla knockback values");
        }
    }

    /** Adjusts horizontal knockback by Purpur's attack-knockback multiplier */
    public double adjustHorizontalKB(double vanillaKB) {
        if (!customKnockbackEnabled) return vanillaKB;
        return vanillaKB * attackKnockbackMultiplier;
    }

    /** Adjusts vertical knockback by Purpur's knockback-vertical multiplier */
    public double adjustVerticalKB(double vanillaKB) {
        if (!customKnockbackEnabled) return vanillaKB;
        return vanillaKB * knockbackVerticalMultiplier;
    }

    public boolean isPurpur() { return isPurpur; }
    public boolean isCustomKnockbackEnabled() { return customKnockbackEnabled; }
    public double getAttackKnockbackMultiplier() { return attackKnockbackMultiplier; }
    public double getKnockbackVerticalMultiplier() { return knockbackVerticalMultiplier; }
}
