package io.windfall.anticheat;

import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.github.retrooper.packetevents.PacketEvents;
import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.api.WindfallAPI;
import io.windfall.anticheat.api.WindfallProvider;
import io.windfall.anticheat.api.WindfallAPIImpl;
import io.windfall.anticheat.core.bedrock.GeyserManager;
import io.windfall.anticheat.core.bedrock.GeysersTracker;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.compat.WorldGuardCompat;
import io.windfall.anticheat.core.command.ChecklistGUI;
import io.windfall.anticheat.core.command.CommandManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.network.PacketListener;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.platform.FoliaCompat;
import io.windfall.anticheat.core.platform.PurpurCompat;
import io.windfall.anticheat.core.plugin.PluginDetector;
import io.windfall.anticheat.core.punishment.PunishmentEngine;
import io.windfall.anticheat.core.scheduler.PlatformScheduler;
import io.windfall.anticheat.core.severity.SeverityManager;
import io.windfall.anticheat.core.version.ServerFork;
import io.windfall.anticheat.core.version.VersionManager;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.compensation.LatencyCompensator;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point — owns all managers and enforces strict initialization order.
 *
 * <p>Startup lifecycle:
 * <ol>
 *   <li>{@link #onLoad()}: Sets static instance, initializes PacketEvents API</li>
 *   <li>{@link #onEnable()}: Creates managers in dependency order, registers listeners</li>
 *   <li>{@link #onDisable()}: Stops scheduler, terminates PacketEvents</li>
 * </ol>
 *
 * <p>Manager initialization order matters — later managers depend on earlier ones:
 * {@code Config → Version → Fork → Plugin → Scheduler → Platform → Player → Transaction
 * → Geyser → Severity → Punishment → Check → Command → Alert → GUI}
 *
 * <p>Player lifecycle:
 * <ul>
 *   <li>Added at LOGIN_SUCCESS via {@link PacketListener}</li>
 *   <li>Removed on PlayerQuitEvent via {@link PlayerQuitListener}</li>
 * </ul>
 *
 * @see CheckManager for anti-cheat check registration and processing
 * @see PacketListener for packet interception and player state updates
 */
// Single entry point — owns all managers, enforces strict init order
public final class WindfallPlugin extends JavaPlugin {

    private static WindfallPlugin instance;
    private PlatformScheduler scheduler;
    private PlayerManager playerManager;
    private CheckManager checkManager;
    private TransactionManager transactionManager;
    private PingPongManager pingPongManager;
    private LatencyCompensator latencyCompensator;
    private SimulationEngine simulationEngine;
    private VersionManager versionManager;
    private CommandManager commandManager;
    private WindfallConfig config;
    private AlertManager alertManager;
    private GeyserManager geyserManager;
    private GeysersTracker geysersTracker;
    private PunishmentEngine punishmentEngine;
    private SeverityManager severityManager;
    private ChecklistGUI checklistGUI;
    private ServerFork serverFork;
    private PluginDetector pluginDetector;
    private WorldGuardCompat worldGuardCompat;
    private FoliaCompat foliaCompat;
    private PurpurCompat purpurCompat;
    private volatile boolean running;

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();

        this.config = new WindfallConfig(this);
        this.versionManager = new VersionManager();
        this.serverFork = ServerFork.detect(getLogger());
        this.pluginDetector = new PluginDetector();
        this.pluginDetector.init(this);
        this.scheduler = new PlatformScheduler(this);
        this.foliaCompat = new FoliaCompat(serverFork.isFolia());
        this.foliaCompat.init(getLogger());
        this.purpurCompat = new PurpurCompat();
        this.purpurCompat.init(serverFork, getLogger());
        this.playerManager = new PlayerManager();
        this.transactionManager = new TransactionManager(this);
        this.pingPongManager = new PingPongManager(this);
        this.latencyCompensator = new LatencyCompensator();
        this.simulationEngine = new SimulationEngine(pingPongManager);
        this.geyserManager = GeyserManager.init(this);
        this.geysersTracker = new GeysersTracker();
        this.severityManager = SeverityManager.fromConfig(config);
        this.punishmentEngine = new PunishmentEngine(this);
        this.checkManager = new CheckManager(this);
        this.commandManager = new CommandManager(this);
        this.alertManager = new AlertManager(this);
        this.checklistGUI = new ChecklistGUI(this);

        // Initialize WorldGuard integration if available
        if (pluginDetector.isWorldGuardInstalled()) {
            this.worldGuardCompat = WorldGuardCompat.load();
            if (worldGuardCompat != null) {
                getLogger().info("[Windfall] WorldGuard integration loaded");
            }
        }

        // Register public API for other plugins
        WindfallProvider.register(new WindfallAPIImpl(this));

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(this));
        PacketEvents.getAPI().init();

        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);

        this.running = true;
        this.scheduler.startGlobalTick();

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        getLogger().info("Windfall v" + getDescription().getVersion() + " enabled in " + elapsed + "ms ("
            + versionManager.getServerVersion() + ", " + serverFork.getDisplayName() + ")");
    }

    @Override
    public void onDisable() {
        this.running = false;
        WindfallProvider.unregister();
        if (scheduler != null) scheduler.shutdown();
        PacketEvents.getAPI().terminate();
        getLogger().info("Windfall disabled.");
    }

    public static WindfallPlugin getInstance() { return instance; }
    public PlatformScheduler getScheduler() { return scheduler; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public CheckManager getCheckManager() { return checkManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public PingPongManager getPingPongManager() { return pingPongManager; }
    public LatencyCompensator getLatencyCompensator() { return latencyCompensator; }
    public SimulationEngine getSimulationEngine() { return simulationEngine; }
    public VersionManager getVersionManager() { return versionManager; }
    public WindfallConfig getWindfallConfig() { return config; }
    public AlertManager getAlertManager() { return alertManager; }
    public GeyserManager getGeyserManager() { return geyserManager; }
    public GeysersTracker getGeysersTracker() { return geysersTracker; }
    public PunishmentEngine getPunishmentEngine() { return punishmentEngine; }
    public SeverityManager getSeverityManager() { return severityManager; }
    public ChecklistGUI getChecklistGUI() { return checklistGUI; }
    public boolean isRunning() { return running; }
    public ServerFork getServerFork() { return serverFork; }
    public PluginDetector getPluginDetector() { return pluginDetector; }
    public WorldGuardCompat getWorldGuardCompat() { return worldGuardCompat; }
    public FoliaCompat getFoliaCompat() { return foliaCompat; }
    public PurpurCompat getPurpurCompat() { return purpurCompat; }

    private final class PlayerQuitListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            java.util.UUID uuid = event.getPlayer().getUniqueId();
            if (transactionManager != null) {
                transactionManager.onPlayerQuit(uuid);
            }
            if (pingPongManager != null) {
                pingPongManager.onPlayerQuit(uuid);
            }
            if (latencyCompensator != null) {
                latencyCompensator.onPlayerQuit(uuid);
            }
            if (punishmentEngine != null) {
                punishmentEngine.cleanup(uuid);
            }
            if (checkManager != null) {
                checkManager.removePlayer(uuid);
                // Save violation history and clean fingerprint on disconnect
                checkManager.getViolationPattern().savePlayerHistory(uuid);
                checkManager.getPacketFingerprint().removePlayer(uuid);
            }
            if (playerManager != null) {
                playerManager.remove(uuid);
            }
        }
    }
}
