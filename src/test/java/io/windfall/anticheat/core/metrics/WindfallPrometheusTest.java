package io.windfall.anticheat.core.metrics;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.config.WindfallConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WindfallPrometheusTest {

    private WindfallPlugin mockPlugin;
    private WindfallConfig mockConfig;
    private WindfallPrometheus prometheus;

    @BeforeEach
    void setUp() {
        mockPlugin = mock(WindfallPlugin.class);
        mockConfig = mock(WindfallConfig.class);
        when(mockPlugin.getWindfallConfig()).thenReturn(mockConfig);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());
        prometheus = new WindfallPrometheus(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        if (prometheus != null) {
            prometheus.shutdown();
        }
    }

    @Test
    void disabledByDefault_doesNotStart() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(false);
        prometheus.init(mockPlugin);
        assertFalse(prometheus.isRunning());
    }

    @Test
    void enabled_startsServer() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(true);
        when(mockConfig.getPrometheusHost()).thenReturn("127.0.0.1");
        when(mockConfig.getPrometheusPort()).thenReturn(0); // random port

        prometheus.init(mockPlugin);
        assertTrue(prometheus.isRunning());
    }

    @Test
    void shutdown_stopsServer() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(true);
        when(mockConfig.getPrometheusHost()).thenReturn("127.0.0.1");
        when(mockConfig.getPrometheusPort()).thenReturn(0);

        prometheus.init(mockPlugin);
        assertTrue(prometheus.isRunning());

        prometheus.shutdown();
        assertFalse(prometheus.isRunning());
    }

    @Test
    void tick_whenDisabled_doesNotThrow() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(false);
        prometheus.init(mockPlugin);

        // Should be no-op when disabled
        assertDoesNotThrow(() -> prometheus.tick());
    }

    @Test
    void incrementFlags_whenDisabled_doesNotThrow() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(false);
        prometheus.init(mockPlugin);

        assertDoesNotThrow(() -> prometheus.incrementFlags("windfall.movement.speed"));
    }

    @Test
    void shutdown_whenNotStarted_doesNotThrow() {
        assertDoesNotThrow(() -> prometheus.shutdown());
    }

    @Test
    void multipleShutdowns_areIdempotent() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(true);
        when(mockConfig.getPrometheusHost()).thenReturn("127.0.0.1");
        when(mockConfig.getPrometheusPort()).thenReturn(0);

        prometheus.init(mockPlugin);
        prometheus.shutdown();
        assertDoesNotThrow(() -> prometheus.shutdown());
    }

    @Test
    void startWithInvalidPort_logsWarning() {
        when(mockConfig.isPrometheusEnabled()).thenReturn(true);
        when(mockConfig.getPrometheusHost()).thenReturn("127.0.0.1");
        when(mockConfig.getPrometheusPort()).thenReturn(-1); // invalid port

        // Should not throw, just log warning
        assertDoesNotThrow(() -> prometheus.init(mockPlugin));
        assertFalse(prometheus.isRunning());
    }
}
