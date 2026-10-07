package io.windfall.anticheat.core.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.compat.trust.TrustFactorModel;
import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.metrics.WindfallPrometheus;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Location;

/**
 * Base class for all Windfall anti-cheat checks.
 *
 * <p>Every check must:
 * <ol>
 *   <li>Extend this class and implement {@link PacketCheck}</li>
 *   <li>Declare a {@link CheckData} annotation (read at construction via reflection)</li>
 *   <li>Override {@link #onPacketReceive} or {@link #onPacketSend} with detection logic</li>
 * </ol>
 *
 * <p>Check lifecycle:
 * <ul>
 *   <li>Construction: reads {@link CheckData}, loads config overrides (enabled, maxVl, punishable)</li>
 *   <li>Per-packet: {@code onPacketReceive/onPacketSend} called for every matching packet</li>
 *   <li>Per-tick: {@link #reward(WindfallPlayer)} decays buffer and VL for all online players</li>
 *   <li>On quit: {@link #removePlayer(java.util.UUID)} clears per-player state maps</li>
 * </ul>
 *
 * <p>Violation system:
 * <ul>
 *   <li>{@link #increaseBuffer} / {@link #decreaseBuffer}: accumulate detection confidence</li>
 *   <li>{@link #flag(WindfallPlayer)}: increment VL, send alerts, evaluate punishment</li>
 *   <li>{@link #flagWithSetback(WindfallPlayer)}: flag + immediate teleport setback</li>
 *   <li>{@link #performSetback(WindfallPlayer)}: teleport to last safe position</li>
 * </ul>
 *
 * @see CheckData for annotation-based configuration
 * @see CompatFlag for version/platform compatibility flags
 * @see io.windfall.anticheat.core.check.type.PacketCheck for mixin interface
 */
public abstract class Check {

    protected final String name;
    // stableKey maps 1:1 to config.yml — never rename without updating all config files
    protected final String stableKey;
    protected volatile boolean enabled;
    protected volatile boolean punishable;
    /** Buffer decay rate per tick — higher = faster confidence decay */
    protected volatile double decay;
    /** Maximum violation level — VL is capped at this value */
    protected volatile int maxVl;
    /** VL threshold that triggers a setback teleport */
    protected volatile int setbackVl;
    /** Minimum server protocol version to enable this check */
    protected volatile int minVersion;
    /** Maximum server protocol version to enable this check */
    protected volatile int maxVersion;
    protected final CompatFlag[] compatFlags;
    /** Buffer multiplier when RELAX_ON_MISMATCH is active — 1.0 = no relaxation */
    protected final double relaxMultiplier;
    protected final boolean disableOnFolia;
    protected final boolean disableOnPurpur;

    /**
     * Constructs a check by reading its {@link CheckData} annotation via reflection.
     *
     * <p>Requires a static plugin instance ({@link WindfallPlugin#getInstance()}) to
     * access the config. Missing {@link CheckData} causes an immediate startup crash.
     */
    public Check() {
        // Every check class must declare @CheckData or the server crashes at startup
        CheckData data = getClass().getAnnotation(CheckData.class);
        if (data == null) {
            throw new IllegalStateException(
                "Check " + getClass().getSimpleName() + " is missing @CheckData annotation");
        }
        this.name = data.name();
        this.stableKey = data.stableKey();
        this.decay = data.decay();
        this.setbackVl = data.setbackVl();
        this.minVersion = data.minVersion();
        this.maxVersion = data.maxVersion();
        this.compatFlags = data.compat();
        this.relaxMultiplier = data.relaxMultiplier();
        this.disableOnFolia = data.disableOnFolia();
        this.disableOnPurpur = data.disableOnPurpur();

        WindfallConfig cfg = WindfallPlugin.getInstance().getWindfallConfig();
        this.enabled = cfg.isCheckEnabled(stableKey);
        this.maxVl = cfg.getCheckMaxVl(stableKey);
        this.punishable = cfg.isCheckPunishable(stableKey);
        // decay and setbackVl have to be applied here as well: this constructor is what the
        // check runs with from the moment it is registered, so leaving them to
        // CheckManager#reloadChecks meant an operator's explicit config.yml values were
        // silently ignored until the first /windfall reload. Null means "not written in
        // config.yml" — keep the @CheckData defaults in that case.
        Integer explicitSetbackVl = cfg.getExplicitCheckSetbackVl(stableKey);
        if (explicitSetbackVl != null) {
            this.setbackVl = explicitSetbackVl;
        }
        Double explicitDecay = cfg.getExplicitCheckDecay(stableKey);
        if (explicitDecay != null) {
            this.decay = explicitDecay;
        }
    }

    /**
     * Called when a client-to-server packet is received.
     * Override in checks that inspect incoming packets (attacks, movement, inventory).
     */
    public abstract void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event);

    /**
     * Called when a server-to-client packet is sent.
     * Override in checks that inspect outgoing packets (respawn, position teleport, abilities).
     */
    public abstract void onPacketSend(WindfallPlayer player, PacketSendEvent event);

    /**
     * Removes per-player state for the given UUID.
     * Override in checks that maintain {@code ConcurrentHashMap<UUID, PlayerState>} maps
     * to prevent memory leaks when players disconnect.
     */
    public void removePlayer(java.util.UUID uuid) {
        // Override in checks that maintain per-player state maps
    }

    /**
     * Increments the violation level and dispatches alerts/punishments.
     *
     * <p>Flow:
     * <ol>
     *   <li>Compute scaled VL increment via {@link SeverityManager}</li>
     *   <li>Merge into player's VL map, cap at maxVl</li>
     *   <li>Dispatch alert to staff (if enabled and cooldown expired)</li>
     *   <li>Evaluate punishment tier via {@link PunishmentEngine}</li>
     *   <li>If VL ≥ setbackVl: reset VL and teleport to last safe position</li>
     * </ol>
     *
     * @param player the flagged player
     */
    public void flag(WindfallPlayer player) {
        if (!enabled) return;

        decreaseTrust(player);

        WindfallPlugin plugin = WindfallPlugin.getInstance();
        int increment = plugin.getSeverityManager().getScaledVlIncrement(player);
        int vl = player.getViolationLevels().merge(stableKey, increment, Integer::sum);

        // Cap at maxVl so VL cannot exceed threshold even under rapid flagging
        if (vl > maxVl) {
            player.getViolationLevels().put(stableKey, maxVl);
            vl = maxVl;
        }

        // A fresh violation restarts the decay grace period
        lastVlDecayMs = System.currentTimeMillis();

        // Record violation for repeat-offender pattern analysis
        if (plugin.getCheckManager() != null) {
            plugin.getCheckManager().getViolationPattern()
                .recordViolation(player.getUuid(), stableKey, vl, plugin.getCheckManager().getTickCounter());

            // Increment Prometheus flag counter
            WindfallPrometheus prometheus = plugin.getCheckManager().getPrometheus();
            if (prometheus != null) {
                prometheus.incrementFlags(stableKey);
            }
        }

        AlertManager alertManager = plugin.getAlertManager();
        if (alertManager != null && player.isAlertsEnabled() && vl > 0) {
            alertManager.sendAlert(player, this, "VL=" + vl);
        } else if (plugin.getWindfallConfig().isVerboseEnabled()) {
            plugin.getLogger().warning("[" + name + "] " + player.getName() + " VL=" + vl);
        }

        if (punishable && plugin.getPunishmentEngine() != null) {
            plugin.getPunishmentEngine().evaluate(player);
        }

        // Reset before setback to prevent instant re-punishment on next flag
        if (vl >= setbackVl) {
            player.getViolationLevels().put(stableKey, 0);
            performSetback(player);
        }
    }

    /**
     * Flags and immediately performs a setback, regardless of VL threshold.
     * Used for critical violations that require instant correction (e.g., inventory exploit).
     *
     * @param player the flagged player
     */
    public void flagWithSetback(WindfallPlayer player) {
        if (!enabled) return;

        decreaseTrust(player);

        WindfallPlugin plugin = WindfallPlugin.getInstance();
        int increment = plugin.getSeverityManager().getScaledVlIncrement(player);
        int vl = player.getViolationLevels().merge(stableKey, increment, Integer::sum);

        if (vl > maxVl) {
            player.getViolationLevels().put(stableKey, maxVl);
            vl = maxVl;
        }

        /* Setback violations are critical and need the same repeat-offender and metrics
         * bookkeeping as flag(), which otherwise only saw plain flags. Without this, a check
         * that only ever calls flagWithSetback produced no violation history and no Prometheus
         * flag counts, so staff dashboards silently under-reported it. */
        lastVlDecayMs = System.currentTimeMillis();

        if (plugin.getCheckManager() != null) {
            plugin.getCheckManager().getViolationPattern()
                .recordViolation(player.getUuid(), stableKey, vl, plugin.getCheckManager().getTickCounter());

            WindfallPrometheus prometheus = plugin.getCheckManager().getPrometheus();
            if (prometheus != null) {
                prometheus.incrementFlags(stableKey);
            }
        }

        AlertManager alertManager = plugin.getAlertManager();
        if (alertManager != null && player.isAlertsEnabled()) {
            alertManager.sendAlert(player, this, "VL=" + vl + " (SETBACK)");
        } else if (plugin.getWindfallConfig().isVerboseEnabled()) {
            plugin.getLogger().warning("[" + name + "] " + player.getName() + " VL=" + vl + " (SETBACK)");
        }

        if (punishable && plugin.getPunishmentEngine() != null) {
            plugin.getPunishmentEngine().evaluate(player);
        }

        performSetback(player);

        if (vl >= setbackVl) {
            player.getViolationLevels().put(stableKey, 0);
        }
    }

    /**
     * Decreases the player's VL and buffer for this check.
     * Called once per tick for all online players — provides recovery for clean play.
     *
     * <p>Buffer decays every tick, but VL only decays once per {@value #VL_DECAY_INTERVAL_MS}ms.
     * Decrementing VL on every tick let punishment tiers evaporate within a second of a single
     * flag: with several checks registered, a player at the permban threshold lost all of it
     * between two alerts.
     *
     * @param player the player to reward
     */
    public void reward(WindfallPlayer player) {
        increaseTrust(player);

        double buf = player.getBuffers().getOrDefault(stableKey, 0.0);
        if (buf > 0.0) {
            player.getBuffers().put(stableKey, Math.max(0.0, buf - decay));
        }

        int vl = player.getViolationLevels().getOrDefault(stableKey, 0);
        if (vl <= 0) return;

        long now = System.currentTimeMillis();
        long last = lastVlDecayMs;
        if (now - last < VL_DECAY_INTERVAL_MS) return;
        // Compare-and-set so concurrent reward calls cannot stack extra decrements
        if (!compareAndSetLastVlDecay(last, now)) return;

        player.getViolationLevels().put(stableKey, vl - 1);
    }

    /** Interval between VL decrements during clean play, in milliseconds. */
    static final long VL_DECAY_INTERVAL_MS = 1000L;

    private volatile long lastVlDecayMs = 0L;

    private boolean compareAndSetLastVlDecay(long expected, long update) {
        synchronized (this) {
            if (lastVlDecayMs != expected) return false;
            lastVlDecayMs = update;
            return true;
        }
    }

    /**
     * Teleports the player to their last known safe position.
     * Prefers the last teleport position; falls back to last ground position if no teleport sent yet.
     * The (0,0,0) sentinel indicates no teleport has been received.
     */
    // Prefers last teleport position; falls back to last ground if no teleport sent yet (0,0,0 sentinel)
    protected void performSetback(WindfallPlayer player) {
        // Skip setback during respawn desync — position is (0,0,0) until first post-respawn position packet
        if (player.isRespawned()) return;
        if (player.getPlayer() == null || !player.getPlayer().isOnline()) return;
        double tx = player.getTeleportX();
        double ty = player.getTeleportY();
        double tz = player.getTeleportZ();
        if (tx == 0.0 && ty == 0.0 && tz == 0.0) {
            tx = player.getGroundX();
            ty = player.getGroundY();
            tz = player.getGroundZ();
        }
        Location loc = new Location(
            player.getPlayer().getWorld(), tx, ty, tz,
            player.getYaw(), player.getPitch());
        WindfallPlugin.getInstance().getFoliaCompat().teleportAsync(player.getPlayer(), loc);
    }

    /** Returns this check's violation level for the given player */
    public int getViolationLevel(WindfallPlayer player) {
        return player.getViolationLevels().getOrDefault(stableKey, 0);
    }

    /** Returns this check's buffer (detection confidence) for the given player */
    public double getBuffer(WindfallPlayer player) {
        return player.getBuffers().getOrDefault(stableKey, 0.0);
    }

    /** Adds to this check's buffer — higher values indicate stronger detection confidence */
    public void increaseBuffer(WindfallPlayer player, double amount) {
        player.getBuffers().merge(stableKey, amount, Double::sum);
    }

    /** Subtracts from this check's buffer, floored at 0.0 — buffers must never go negative */
    // Floor at 0.0 — buffers represent confidence and must never go negative
    public void decreaseBuffer(WindfallPlayer player, double amount) {
        player.getBuffers().merge(stableKey, -amount, (a, b) -> Math.max(0.0, a + b));
    }

    /** Resets this check's buffer to 0.0 — used after flagging to prevent double-flagging */
    public void resetBuffer(WindfallPlayer player) {
        player.getBuffers().put(stableKey, 0.0);
    }

    public String getName() { return name; }
    public String getStableKey() { return stableKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isPunishable() { return punishable; }
    public void setPunishable(boolean punishable) { this.punishable = punishable; }
    public double getDecay() { return decay; }
    public void setDecay(double decay) { this.decay = decay; }
    public int getMaxVl() { return maxVl; }
    public void setMaxVl(int maxVl) { this.maxVl = maxVl; }
    public int getSetbackVl() { return setbackVl; }
    public void setSetbackVl(int setbackVl) { this.setbackVl = setbackVl; }
    public int getMinVersion() { return minVersion; }
    public int getMaxVersion() { return maxVersion; }
    public CompatFlag[] getCompatFlags() { return compatFlags; }
    public double getRelaxMultiplier() { return relaxMultiplier; }
    public boolean isDisableOnFolia() { return disableOnFolia; }
    public boolean isDisableOnPurpur() { return disableOnPurpur; }

    /** Returns true if this check has the given compatibility flag */
    public boolean hasCompatFlag(CompatFlag flag) {
        for (CompatFlag f : compatFlags) {
            if (f == flag) return true;
        }
        return false;
    }

    /**
     * Utility: flags if value exceeds threshold, with proportional buffer increase.
     *
     * <p>Buffer increases proportionally to how far the value exceeds the threshold
     * (e.g., 10% over = +0.1 buffer). Flags when buffer exceeds 2.0, then resets.
     * Decreases buffer by 0.1 when below threshold for gradual recovery.
     *
     * @param player    the player to check
     * @param value     the measured value (e.g., speed, distance)
     * @param threshold the maximum allowed value
     */
    public void flagIfAboveThreshold(WindfallPlayer player, double value, double threshold) {
        if (value > threshold) {
            increaseBuffer(player, (value - threshold) / threshold);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    /**
     * Scales a detection threshold by the player's current trust rank.
     *
     * <p>Flagged players ({@link TrustFactorModel.TrustRank#SUPER_UNTRUSTWORTHY}/
     * {@link TrustFactorModel.TrustRank#UNTRUSTWORTHY}) face tighter thresholds so their suspicion
     * compounds; trusted players ({@link TrustFactorModel.TrustRank#TRUSTED}/
     * {@link TrustFactorModel.TrustRank#LEGIT}) earn a wider margin before being flagged again.
     *
     * @param player the player being evaluated
     * @param base   the default (SUSPICIOUS/NORMAL) threshold
     * @return the trust-adjusted threshold
     */
    protected double trustAdjustedThreshold(WindfallPlayer player, double base) {
        TrustFactorModel trust = player != null ? player.getTrustFactor() : null;
        if (trust == null) return base;
        switch (trust.getRank()) {
            case SUPER_UNTRUSTWORTHY: return base * 0.5D;
            case UNTRUSTWORTHY: return base * 0.75D;
            case TRUSTED: return base * 1.1D;
            case LEGIT: return base * 1.3D;
            default: return base;
        }
    }

    /** Lowers trust on a violation so repeat offenders get progressively tighter thresholds. */
    private void decreaseTrust(WindfallPlayer player) {
        TrustFactorModel trust = player != null ? player.getTrustFactor() : null;
        if (trust != null) {
            trust.decreaseTrust();
        }
    }

    /** Raises trust during clean play — a tick with no violation restores some standing. */
    private void increaseTrust(WindfallPlayer player) {
        TrustFactorModel trust = player != null ? player.getTrustFactor() : null;
        if (trust != null) {
            trust.increaseTrust();
        }
    }
}
