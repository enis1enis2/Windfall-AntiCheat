package io.windfall.anticheat.core.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.Location;

public abstract class Check {

    protected final String name;
    protected final String stableKey;
    protected boolean enabled;
    protected boolean punishable;
    protected double decay;
    protected int maxVl;
    protected int setbackVl;
    protected int minVersion;
    protected int maxVersion;

    public Check() {
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

        WindfallConfig cfg = WindfallPlugin.getInstance().getWindfallConfig();
        this.enabled = cfg.isCheckEnabled(stableKey);
        this.maxVl = cfg.getCheckMaxVl(stableKey);
        this.punishable = cfg.isCheckPunishable(stableKey);
    }

    public abstract void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event);
    public abstract void onPacketSend(WindfallPlayer player, PacketSendEvent event);

    public void flag(WindfallPlayer player) {
        if (!enabled) return;

        WindfallPlugin plugin = WindfallPlugin.getInstance();
        int increment = plugin.getSeverityManager().getScaledVlIncrement(player);
        int vl = player.getViolationLevels().merge(stableKey, increment, Integer::sum);

        if (vl > maxVl) {
            player.getViolationLevels().put(stableKey, maxVl);
            vl = maxVl;
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

        if (vl >= setbackVl) {
            player.getViolationLevels().put(stableKey, 0);
            performSetback(player);
        }
    }

    public void flagWithSetback(WindfallPlayer player) {
        if (!enabled) return;

        WindfallPlugin plugin = WindfallPlugin.getInstance();
        int increment = plugin.getSeverityManager().getScaledVlIncrement(player);
        int vl = player.getViolationLevels().merge(stableKey, increment, Integer::sum);

        if (vl > maxVl) {
            player.getViolationLevels().put(stableKey, maxVl);
            vl = maxVl;
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

    public void reward(WindfallPlayer player) {
        int vl = player.getViolationLevels().getOrDefault(stableKey, 0);
        if (vl > 1) {
            player.getViolationLevels().put(stableKey, vl - 1);
        } else if (vl == 1) {
            player.getViolationLevels().put(stableKey, 0);
        }

        double buf = player.getBuffers().getOrDefault(stableKey, 0.0);
        if (buf > 0.0) {
            player.getBuffers().put(stableKey, Math.max(0.0, buf - decay));
        }
    }

    protected void performSetback(WindfallPlayer player) {
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
        player.getPlayer().teleport(loc);
    }

    public int getViolationLevel(WindfallPlayer player) {
        return player.getViolationLevels().getOrDefault(stableKey, 0);
    }

    public double getBuffer(WindfallPlayer player) {
        return player.getBuffers().getOrDefault(stableKey, 0.0);
    }

    public void increaseBuffer(WindfallPlayer player, double amount) {
        player.getBuffers().merge(stableKey, amount, Double::sum);
    }

    public void decreaseBuffer(WindfallPlayer player, double amount) {
        player.getBuffers().merge(stableKey, -amount, (a, b) -> Math.max(0.0, a + b));
    }

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
    public int getMaxVl() { return maxVl; }
    public int getSetbackVl() { return setbackVl; }
    public int getMinVersion() { return minVersion; }
    public int getMaxVersion() { return maxVersion; }
}
