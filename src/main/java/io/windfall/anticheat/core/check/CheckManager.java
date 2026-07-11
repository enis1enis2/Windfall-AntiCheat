package io.windfall.anticheat.core.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.impl.combat.AimCheck;
import io.windfall.anticheat.core.check.impl.combat.ReachCheck;
import io.windfall.anticheat.core.check.impl.combat.CriticalsCheck;
import io.windfall.anticheat.core.check.impl.combat.KillAuraCheck;
import io.windfall.anticheat.core.check.impl.combat.FastHealCheck;
import io.windfall.anticheat.core.check.impl.combat.SwordBlockCheck;
import io.windfall.anticheat.core.check.impl.movement.SpeedCheck;
import io.windfall.anticheat.core.check.impl.movement.FlightCheck;
import io.windfall.anticheat.core.check.impl.movement.VelocityCheck;
import io.windfall.anticheat.core.check.impl.movement.TimerCheck;
import io.windfall.anticheat.core.check.impl.movement.NoFallCheck;
import io.windfall.anticheat.core.check.impl.movement.StepCheck;
import io.windfall.anticheat.core.check.impl.movement.ScaffoldCheck;
import io.windfall.anticheat.core.check.impl.movement.ElytraCheck;
import io.windfall.anticheat.core.check.impl.packet.BadPacketsCheck;
import io.windfall.anticheat.core.check.impl.packet.ChestStealerCheck;
import io.windfall.anticheat.core.check.impl.packet.CreativeCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {

    private final WindfallPlugin plugin;
    private final List<Check> checks = new ArrayList<>();
    private final Map<String, Check> checkByKey = new ConcurrentHashMap<>();
    private final int serverProtocol;

    public CheckManager(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.serverProtocol = plugin.getVersionManager().getProtocolVersion();
        registerChecks();
    }

    private void registerChecks() {
        List<Check> allChecks = new ArrayList<>();
        allChecks.add(new AimCheck());
        allChecks.add(new ReachCheck());
        allChecks.add(new CriticalsCheck());
        allChecks.add(new KillAuraCheck());
        allChecks.add(new FastHealCheck());
        allChecks.add(new SwordBlockCheck());
        allChecks.add(new SpeedCheck());
        allChecks.add(new FlightCheck());
        allChecks.add(new VelocityCheck());
        allChecks.add(new TimerCheck());
        allChecks.add(new NoFallCheck());
        allChecks.add(new StepCheck());
        allChecks.add(new ScaffoldCheck());
        allChecks.add(new ElytraCheck());
        allChecks.add(new BadPacketsCheck());
        allChecks.add(new ChestStealerCheck());
        allChecks.add(new CreativeCheck());

        for (Check check : allChecks) {
            if (serverProtocol >= check.getMinVersion() && serverProtocol <= check.getMaxVersion()) {
                checks.add(check);
                checkByKey.put(check.getStableKey(), check);
            } else {
                plugin.getLogger().info("[Windfall] Skipping " + check.getName()
                    + " (requires " + check.getMinVersion() + "-" + check.getMaxVersion()
                    + ", server=" + serverProtocol + ")");
            }
        }

        plugin.getLogger().info("[Windfall] Registered " + checks.size() + "/" + allChecks.size()
            + " checks for protocol " + serverProtocol);
    }

    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        for (Check check : checks) {
            if (!check.isEnabled()) continue;
            try {
                check.onPacketReceive(player, event);
            } catch (Exception e) {
                plugin.getLogger().severe("Check " + check.getName() + " error: " + e.getMessage());
            }
        }
    }

    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
        for (Check check : checks) {
            if (!check.isEnabled()) continue;
            try {
                check.onPacketSend(player, event);
            } catch (Exception e) {
                plugin.getLogger().severe("Check " + check.getName() + " error: " + e.getMessage());
            }
        }
    }

    public void onTick() {
        io.windfall.anticheat.core.punishment.PunishmentEngine pe = plugin.getPunishmentEngine();
        for (WindfallPlayer player : plugin.getPlayerManager().getAllPlayers()) {
            if (!player.isValid()) continue;
            player.resetTickState();
            for (Check check : checks) {
                if (!check.isEnabled()) continue;
                check.reward(player);
            }
            if (pe != null) {
                pe.decayTierIfNeeded(player);
            }
        }
    }

    public void reloadChecks() {
        plugin.getWindfallConfig().reload();
        for (Check check : checks) {
            check.setEnabled(plugin.getWindfallConfig().isCheckEnabled(check.getStableKey()));
            check.setPunishable(plugin.getWindfallConfig().isCheckPunishable(check.getStableKey()));
        }
    }

    public Check getCheckByStableKey(String key) {
        return checkByKey.get(key);
    }

    public List<Check> getChecks() {
        return checks;
    }
}
