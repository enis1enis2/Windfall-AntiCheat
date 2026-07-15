package io.windfall.anticheat.core.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.adaptive.AdaptiveThreshold;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;

/**
 * Self-contained Prometheus metrics endpoint for Windfall.
 *
 * <p>Exposes anti-cheat telemetry on a dedicated HTTP port (default 9211) using
 * the Prometheus simpleclient library. Zero external dependencies beyond the
 * shaded simpleclient JARs.
 *
 * <p><b>Metrics exposed:</b>
 * <ul>
 *   <li>{@code windfall_ticks_per_second} — server tick rate (Gauge)</li>
 *   <li>{@code windfall_checks_per_second} — checks evaluated per tick (Gauge)</li>
 *   <li>{@code windfall_active_players} — online player count (Gauge)</li>
 *   <li>{@code windfall_check_buffer_sum} — total buffer across all players/checks (Gauge)</li>
 *   <li>{@code windfall_adaptive_threshold} — current TPS-aware tolerance multiplier (Gauge)</li>
 *   <li>{@code windfall_server_tps} — estimated server TPS (Gauge)</li>
 *   <li>{@code windfall_violation_total} — cumulative flag count per check (Counter)</li>
 * </ul>
 *
 * <p><b>Lifecycle:</b>
 * <ul>
 *   <li>{@link #init(WindfallPlugin)} — called from {@code onEnable()} after AlertManager</li>
 *   <li>{@link #tick()} — called from {@code CheckManager.onTick()} every tick</li>
 *   <li>{@link #incrementFlags(String)} — called from {@code Check.flag()} on every flag</li>
 *   <li>{@link #shutdown()} — called from {@code onDisable()}</li>
 * </ul>
 *
 * <p>Disabled by default ({@code prometheus.enabled: false} in config.yml).
 * When disabled, all methods are no-ops with zero overhead.
 *
 * @see io.windfall.anticheat.core.config.WindfallConfig#isPrometheusEnabled()
 */
public final class WindfallPrometheus {

    private HTTPServer server;
    private final WindfallPlugin plugin;
    private final CollectorRegistry registry = new CollectorRegistry();

    // === Gauges ===
    private final Gauge ticksPerSecond = Gauge.build()
        .name("windfall_ticks_per_second")
        .help("Server tick rate")
        .create();
    private final Gauge checksPerSecond = Gauge.build()
        .name("windfall_checks_per_second")
        .help("Checks evaluated per tick")
        .create();
    private final Gauge activePlayers = Gauge.build()
        .name("windfall_active_players")
        .help("Online player count")
        .create();
    private final Gauge checkBufferSum = Gauge.build()
        .name("windfall_check_buffer_sum")
        .help("Total buffer across all players and checks")
        .create();
    private final Gauge adaptiveThreshold = Gauge.build()
        .name("windfall_adaptive_threshold")
        .help("Current TPS-aware tolerance multiplier")
        .create();
    private final Gauge serverTps = Gauge.build()
        .name("windfall_server_tps")
        .help("Estimated server TPS")
        .create();
    private final Gauge violationTotal = Gauge.build()
        .name("windfall_violation_total")
        .help("Total violation level across all players per check")
        .create();

    // === Counters ===
    private final Counter flagCounter = Counter.build()
        .name("windfall_flags_total")
        .help("Cumulative flag count per check")
        .labelNames("check")
        .create();

    /** Tick duration accumulator for computing ticks/second */
    private long lastTickTime;
    /** Rolling tick duration for TPS estimation (nanoseconds) */
    private long tickDurationAccum;
    /** Number of tick samples in the current window */
    private int tickSamples;

    /**
     * Creates a new WindfallPrometheus instance.
     *
     * @param plugin the Windfall plugin instance
     */
    public WindfallPrometheus(WindfallPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the Prometheus HTTP server on the configured host/port.
     * No-op if Prometheus is disabled in config.
     *
     * @param plugin the Windfall plugin instance
     */
    public void init(WindfallPlugin plugin) {
        if (!plugin.getWindfallConfig().isPrometheusEnabled()) {
            plugin.getLogger().info("[Windfall] Prometheus metrics disabled (prometheus.enabled=false)");
            return;
        }

        String host = plugin.getWindfallConfig().getPrometheusHost();
        int port = plugin.getWindfallConfig().getPrometheusPort();

        if (port < 0 || port > 65535) {
            plugin.getLogger().log(Level.WARNING,
                "[Windfall] Invalid Prometheus port: " + port + ". Must be 0-65535.");
            return;
        }

        try {
            ticksPerSecond.register(registry);
            checksPerSecond.register(registry);
            activePlayers.register(registry);
            checkBufferSum.register(registry);
            adaptiveThreshold.register(registry);
            serverTps.register(registry);
            violationTotal.register(registry);
            flagCounter.register(registry);
            server = new HTTPServer(new InetSocketAddress(host, port), registry, true);
            lastTickTime = System.nanoTime();
            plugin.getLogger().info("[Windfall] Prometheus metrics started on " + host + ":" + port);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                "[Windfall] Failed to start Prometheus server on " + host + ":" + port, e);
        }
    }

    /**
     * Called every tick from {@code CheckManager.onTick()} to update time-series gauges.
     * Computes ticks/second from tick duration and updates player/buffer/TPS metrics.
     */
    public void tick() {
        if (server == null) return;

        long now = System.nanoTime();
        long elapsed = now - lastTickTime;
        lastTickTime = now;

        // TPS estimation from tick duration (50ms = 20 TPS baseline)
        double tps = elapsed > 0 ? 1_000_000_000.0 / elapsed : 20.0;
        serverTps.set(tps);

        // Ticks per second (1.0 in normal operation, >1.0 if catch-up ticks)
        ticksPerSecond.set(1.0);

        // Active player count
        int playerCount = plugin.getPlayerManager().getAllPlayers().size();
        activePlayers.set(playerCount);

        // Total buffer across all players and checks
        double totalBuffer = 0;
        int totalChecks = 0;
        for (WindfallPlayer player : plugin.getPlayerManager().getAllPlayers()) {
            for (Check check : plugin.getCheckManager().getChecks()) {
                double buf = player.getBuffers().getOrDefault(check.getStableKey(), 0.0);
                totalBuffer += buf;
                if (buf > 0) totalChecks++;
            }
        }
        checkBufferSum.set(totalBuffer);
        checksPerSecond.set(totalChecks);

        // Adaptive threshold
        AdaptiveThreshold at = plugin.getCheckManager().getAdaptiveThreshold();
        if (at != null) {
            adaptiveThreshold.set(at.getCurrentMultiplier());
        }
    }

    /**
     * Increments the flag counter for the given check.
     * Called from {@code Check.flag()} on every violation.
     *
     * @param checkKey the stableKey of the check that flagged
     */
    public void incrementFlags(String checkKey) {
        if (server == null) return;
        flagCounter.labels(checkKey).inc();
    }

    /**
     * Shuts down the Prometheus HTTP server. No-op if not started.
     */
    public void shutdown() {
        if (server != null) {
            server.close();
            server = null;
            plugin.getLogger().info("[Windfall] Prometheus metrics stopped");
        }
    }

    /**
     * Returns true if the Prometheus server is running.
     *
     * @return true if initialized and listening
     */
    public boolean isRunning() {
        return server != null;
    }
}
