package io.windfall.anticheat.core.check;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.punishment.PunishmentEngine;
import io.windfall.anticheat.core.severity.SeverityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class CheckTestBase {

    protected MockedStatic<WindfallPlugin> pluginStaticMock;

    @Mock protected WindfallPlugin mockPlugin;
    @Mock protected WindfallConfig mockConfig;
    @Mock protected SeverityManager mockSeverityManager;
    @Mock protected AlertManager mockAlertManager;
    @Mock protected PunishmentEngine mockPunishmentEngine;

    @BeforeEach
    void setUpCheckBase() {
        pluginStaticMock = mockStatic(WindfallPlugin.class);

        when(mockPlugin.getWindfallConfig()).thenReturn(mockConfig);
        when(mockPlugin.getSeverityManager()).thenReturn(mockSeverityManager);
        when(mockPlugin.getAlertManager()).thenReturn(mockAlertManager);
        when(mockPlugin.getPunishmentEngine()).thenReturn(mockPunishmentEngine);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());

        when(mockConfig.isCheckEnabled(anyString())).thenReturn(true);
        when(mockConfig.getCheckMaxVl(anyString())).thenReturn(100);
        when(mockConfig.isCheckPunishable(anyString())).thenReturn(true);
        when(mockConfig.isVerboseEnabled()).thenReturn(false);

        when(mockSeverityManager.getScaledVlIncrement(any(WindfallPlayer.class))).thenReturn(1);

        pluginStaticMock.when(WindfallPlugin::getInstance).thenReturn(mockPlugin);
    }

    @AfterEach
    void tearDownCheckBase() {
        if (pluginStaticMock != null) {
            pluginStaticMock.close();
        }
    }

    protected WindfallPlayer createMockPlayer(String name) {
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getUuid()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        when(player.getViolationLevels()).thenReturn(new ConcurrentHashMap<>());
        when(player.getBuffers()).thenReturn(new ConcurrentHashMap<>());
        when(player.isAlertsEnabled()).thenReturn(false);
        when(player.getProtocolVersion()).thenReturn(47);
        when(player.getTotalViolationLevel()).thenReturn(0);
        when(player.isBedrock()).thenReturn(false);

        org.bukkit.entity.Player bukkitPlayer = mock(org.bukkit.entity.Player.class);
        when(bukkitPlayer.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
        when(bukkitPlayer.isInsideVehicle()).thenReturn(false);
        when(player.getPlayer()).thenReturn(bukkitPlayer);

        return player;
    }

    protected WindfallPlayer createMockPlayer(String name, UUID uuid) {
        WindfallPlayer player = createMockPlayer(name);
        when(player.getUuid()).thenReturn(uuid);
        return player;
    }
}
