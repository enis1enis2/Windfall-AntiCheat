package io.windfall.anticheat.core.check;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.impl.combat.AimCheck;
import io.windfall.anticheat.core.check.impl.combat.AutoclickerCheck;
import io.windfall.anticheat.core.check.impl.combat.BacktrackCheck;
import io.windfall.anticheat.core.check.impl.combat.HitboxesCheck;
import io.windfall.anticheat.core.check.impl.combat.MultiInteractCheck;
import io.windfall.anticheat.core.check.impl.combat.SelfInteractCheck;
import io.windfall.anticheat.core.check.impl.combat.ReachCheck;
import io.windfall.anticheat.core.check.impl.combat.CriticalsCheck;
import io.windfall.anticheat.core.check.impl.combat.KillAuraCheck;
import io.windfall.anticheat.core.check.impl.combat.FastHealCheck;
import io.windfall.anticheat.core.check.impl.combat.SwordBlockCheck;
import io.windfall.anticheat.core.check.impl.combat.MacroCheck;
import io.windfall.anticheat.core.check.impl.movement.SpeedCheck;
import io.windfall.anticheat.core.check.impl.movement.FlightCheck;
import io.windfall.anticheat.core.check.impl.movement.VelocityCheck;
import io.windfall.anticheat.core.check.impl.movement.TimerCheck;
import io.windfall.anticheat.core.check.impl.movement.NoFallCheck;
import io.windfall.anticheat.core.check.impl.movement.StepCheck;
import io.windfall.anticheat.core.check.impl.movement.ScaffoldCheck;
import io.windfall.anticheat.core.check.impl.movement.ElytraCheck;
import io.windfall.anticheat.core.check.impl.movement.BaritoneCheck;
import io.windfall.anticheat.core.check.impl.movement.GroundSpoofCheck;
import io.windfall.anticheat.core.check.impl.movement.PhaseCheck;
import io.windfall.anticheat.core.check.impl.movement.SimulationCheck;
import io.windfall.anticheat.core.check.impl.movement.NoSlowCheck;
import io.windfall.anticheat.core.check.impl.movement.MotionCheck;
import io.windfall.anticheat.core.check.impl.movement.FastBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.FarBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.FarPlaceCheck;
import io.windfall.anticheat.core.check.impl.movement.InvalidBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.InvalidPlaceCheck;
import io.windfall.anticheat.core.check.impl.movement.NoSwingCheck;
import io.windfall.anticheat.core.check.impl.movement.RotationBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.AirLiquidBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.WrongBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.PositionBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.MultiBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.AirLiquidPlaceCheck;
import io.windfall.anticheat.core.check.impl.movement.RotationPlaceCheck;
import io.windfall.anticheat.core.check.impl.movement.PositionPlaceCheck;
import io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck;
import io.windfall.anticheat.core.check.impl.packet.BadPacketsCheck;
import io.windfall.anticheat.core.check.impl.packet.ChestStealerCheck;
import io.windfall.anticheat.core.check.impl.packet.CreativeCheck;
import io.windfall.anticheat.core.check.impl.packet.PacketOrderCheck;
import io.windfall.anticheat.core.check.impl.packet.ChatCheck;
import io.windfall.anticheat.core.check.impl.packet.CrashCheck;
import io.windfall.anticheat.core.check.impl.packet.SprintCheck;
import io.windfall.anticheat.core.check.impl.packet.ExploitCheck;
import io.windfall.anticheat.core.check.impl.packet.ClientBrandCheck;
import io.windfall.anticheat.core.check.impl.packet.VehicleCheck;
import io.windfall.anticheat.core.check.impl.inventory.InventoryCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {

    private final WindfallPlugin plugin;
    private final List<Check> checks = new ArrayList<>();
    private final Map<String, Check> checkByKey = new ConcurrentHashMap<>();
    // Captured once at startup — server protocol never changes mid-session
    private final int serverProtocol;

    public CheckManager(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.serverProtocol = plugin.getVersionManager().getProtocolVersion();
        registerChecks();
    }

    private void registerChecks() {
        List<Check> allChecks = new ArrayList<>();
        allChecks.add(new AimCheck());
        allChecks.add(new AutoclickerCheck());
        allChecks.add(new BacktrackCheck());
        allChecks.add(new HitboxesCheck());
        allChecks.add(new MultiInteractCheck());
        allChecks.add(new SelfInteractCheck());
        allChecks.add(new ReachCheck());
        allChecks.add(new CriticalsCheck());
        allChecks.add(new KillAuraCheck());
        allChecks.add(new FastHealCheck());
        allChecks.add(new SwordBlockCheck());
        allChecks.add(new MacroCheck());
        allChecks.add(new SpeedCheck());
        allChecks.add(new FlightCheck());
        allChecks.add(new VelocityCheck());
        allChecks.add(new TimerCheck());
        allChecks.add(new NoFallCheck());
        allChecks.add(new StepCheck());
        allChecks.add(new ScaffoldCheck());
        allChecks.add(new ElytraCheck());
        allChecks.add(new BaritoneCheck());
        allChecks.add(new GroundSpoofCheck());
        allChecks.add(new PhaseCheck());
        allChecks.add(new SimulationCheck());
        allChecks.add(new NoSlowCheck());
        allChecks.add(new MotionCheck());
        allChecks.add(new FastBreakCheck());
        allChecks.add(new FarBreakCheck());
        allChecks.add(new FarPlaceCheck());
        allChecks.add(new InvalidBreakCheck());
        allChecks.add(new InvalidPlaceCheck());
        allChecks.add(new NoSwingCheck());
        allChecks.add(new RotationBreakCheck());
        allChecks.add(new AirLiquidBreakCheck());
        allChecks.add(new WrongBreakCheck());
        allChecks.add(new PositionBreakCheck());
        allChecks.add(new MultiBreakCheck());
        allChecks.add(new AirLiquidPlaceCheck());
        allChecks.add(new RotationPlaceCheck());
        allChecks.add(new PositionPlaceCheck());
        allChecks.add(new MultiPlaceCheck());
        allChecks.add(new BadPacketsCheck());
        allChecks.add(new ChestStealerCheck());
        allChecks.add(new CreativeCheck());
        allChecks.add(new PacketOrderCheck());
        allChecks.add(new ChatCheck());
        allChecks.add(new CrashCheck());
        allChecks.add(new SprintCheck());
        allChecks.add(new ExploitCheck());
        allChecks.add(new ClientBrandCheck());
        allChecks.add(new VehicleCheck());
        allChecks.add(new InventoryCheck());

        // Version filter: checks outside server protocol range are never registered
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

    // Runs every 50ms via PlatformScheduler — reward before decay is intentional
    public void onTick() {
        io.windfall.anticheat.core.punishment.PunishmentEngine pe = plugin.getPunishmentEngine();
        for (WindfallPlayer player : plugin.getPlayerManager().getAllPlayers()) {
            if (!player.isValid()) continue;
            // Must run before checks so lastOnGround reflects previous tick
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
