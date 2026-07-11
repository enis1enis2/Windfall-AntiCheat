package io.windfall.anticheat;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.github.retrooper.packetevents.PacketEvents;
import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.core.bedrock.GeyserManager;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.command.ChecklistGUI;
import io.windfall.anticheat.core.command.CommandManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.network.PacketListener;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.punishment.PunishmentEngine;
import io.windfall.anticheat.core.scheduler.PlatformScheduler;
import io.windfall.anticheat.core.severity.SeverityManager;
import io.windfall.anticheat.core.version.VersionManager;
import io.windfall.anticheat.core.compensation.TransactionManager;
import org.bukkit.plugin.java.JavaPlugin;

// Single entry point — owns all managers, enforces strict init order
public final class WindfallPlugin extends JavaPlugin {

    private static WindfallPlugin instance;
    private PlatformScheduler scheduler;
    private PlayerManager playerManager;
    private CheckManager checkManager;
    private TransactionManager transactionManager;
    private VersionManager versionManager;
    private CommandManager commandManager;
    private WindfallConfig config;
    private AlertManager alertManager;
    private GeyserManager geyserManager;
    private PunishmentEngine punishmentEngine;
    private SeverityManager severityManager;
    private ChecklistGUI checklistGUI;
    private volatile boolean running;

    // PacketEvents must init in onLoad so it's ready before other plugins register listeners
    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    // Init order matters: config → version → scheduler → players → checks → commands → network
    @Override
    public void onEnable() {
        long start = System.nanoTime();

        this.config = new WindfallConfig(this);
        this.versionManager = new VersionManager();
        this.scheduler = new PlatformScheduler(this);
        this.playerManager = new PlayerManager();
        this.transactionManager = new TransactionManager(this);
        this.geyserManager = GeyserManager.init(this);
        this.severityManager = SeverityManager.fromConfig(config);
        this.punishmentEngine = new PunishmentEngine(this);
        this.checkManager = new CheckManager(this);
        this.commandManager = new CommandManager(this);
        this.alertManager = new AlertManager(this);
        this.checklistGUI = new ChecklistGUI(this);

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(this));
        PacketEvents.getAPI().init();

        this.running = true;
        this.scheduler.startGlobalTick();

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        getLogger().info("Windfall v" + getDescription().getVersion() + " enabled in " + elapsed + "ms (" + versionManager.getServerVersion() + ")");
    }

    @Override
    public void onDisable() {
        this.running = false;
        if (scheduler != null) scheduler.shutdown();
        PacketEvents.getAPI().terminate();
        getLogger().info("Windfall disabled.");
    }

    public static WindfallPlugin getInstance() { return instance; }
    public PlatformScheduler getScheduler() { return scheduler; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public CheckManager getCheckManager() { return checkManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public VersionManager getVersionManager() { return versionManager; }
    public WindfallConfig getWindfallConfig() { return config; }
    public AlertManager getAlertManager() { return alertManager; }
    public GeyserManager getGeyserManager() { return geyserManager; }
    public PunishmentEngine getPunishmentEngine() { return punishmentEngine; }
    public SeverityManager getSeverityManager() { return severityManager; }
    public ChecklistGUI getChecklistGUI() { return checklistGUI; }
    public boolean isRunning() { return running; }
}
