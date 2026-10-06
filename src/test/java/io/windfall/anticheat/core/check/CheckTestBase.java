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

import static org.mockito.ArgumentMatchers.anyInt;
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
        // Setback teleports go through FoliaCompat; on a non-Folia server it falls back to a
        // sync teleport, which is a no-op on the mocked player. A real instance avoids mocking
        // a final class whose constructor requires nothing.
        when(mockPlugin.getFoliaCompat()).thenReturn(new io.windfall.anticheat.core.platform.FoliaCompat(false));

        when(mockConfig.isCheckEnabled(anyString())).thenReturn(true);
        when(mockConfig.getCheckMaxVl(anyString())).thenReturn(100);
        when(mockConfig.isCheckPunishable(anyString())).thenReturn(true);
        when(mockConfig.isVerboseEnabled()).thenReturn(false);
        // Mockito answers Integer returns with 0 rather than null, but these two methods mean
        // "absent" via null — a 0 here would override every @CheckData decay/setbackVl default
        // and make the annotation unreachable in tests.
        when(mockConfig.getExplicitCheckSetbackVl(anyString())).thenReturn(null);
        when(mockConfig.getExplicitCheckDecay(anyString())).thenReturn(null);

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
        // Checks that probe the world (e.g. FlightCheck's ground check) treat an offline or
        // unreachable world as "unknown" and refuse to flag, so tests need a reachable one.
        when(bukkitPlayer.isOnline()).thenReturn(true);
        org.bukkit.World world = mock(org.bukkit.World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        org.bukkit.block.Block air = mock(org.bukkit.block.Block.class);
        when(air.getType()).thenReturn(org.bukkit.Material.AIR);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(air);
        when(bukkitPlayer.getWorld()).thenReturn(world);
        when(player.getPlayer()).thenReturn(bukkitPlayer);

        return player;
    }

    protected WindfallPlayer createMockPlayer(String name, UUID uuid) {
        WindfallPlayer player = createMockPlayer(name);
        when(player.getUuid()).thenReturn(uuid);
        return player;
    }
}
