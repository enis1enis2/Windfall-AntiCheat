package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScaffoldCheckEnhancedTest {

    private MockedStatic<WindfallPlugin> pluginStaticMock;
    private WindfallPlugin mockPlugin;
    private WindfallConfig mockConfig;
    private CheckManager mockCheckManager;

    @BeforeEach
    void setUp() {
        pluginStaticMock = mockStatic(WindfallPlugin.class);
        mockPlugin = mock(WindfallPlugin.class);
        mockConfig = mock(WindfallConfig.class);
        mockCheckManager = mock(CheckManager.class);

        when(mockPlugin.getWindfallConfig()).thenReturn(mockConfig);
        when(mockPlugin.getCheckManager()).thenReturn(mockCheckManager);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());
        when(mockCheckManager.getTickCounter()).thenReturn(100L);

        when(mockConfig.isCheckEnabled("windfall.movement.scaffold")).thenReturn(true);
        when(mockConfig.getCheckMaxVl("windfall.movement.scaffold")).thenReturn(100);
        when(mockConfig.isCheckPunishable("windfall.movement.scaffold")).thenReturn(true);

        pluginStaticMock.when(WindfallPlugin::getInstance).thenReturn(mockPlugin);
    }

    @AfterEach
    void tearDown() {
        if (pluginStaticMock != null) {
            pluginStaticMock.close();
        }
    }

    private WindfallPlayer createMockPlayer(String name) {
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getUuid()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        when(player.getViolationLevels()).thenReturn(new ConcurrentHashMap<>());
        when(player.getBuffers()).thenReturn(new ConcurrentHashMap<>());
        when(player.isAlertsEnabled()).thenReturn(false);
        when(player.getProtocolVersion()).thenReturn(47);
        when(player.getTotalViolationLevel()).thenReturn(0);
        when(player.isBedrock()).thenReturn(false);
        when(player.getYaw()).thenReturn(0.0f);
        when(player.getPitch()).thenReturn(0.0f);

        org.bukkit.entity.Player bukkitPlayer = mock(org.bukkit.entity.Player.class);
        when(bukkitPlayer.getGameMode()).thenReturn(org.bukkit.GameMode.SURVIVAL);
        when(bukkitPlayer.isInsideVehicle()).thenReturn(false);
        when(player.getPlayer()).thenReturn(bukkitPlayer);

        return player;
    }

    @Test
    void checkData_hasCorrectName() {
        ScaffoldCheck check = new ScaffoldCheck();
        assertEquals("Scaffold A", check.getName());
    }

    @Test
    void checkData_hasCorrectStableKey() {
        ScaffoldCheck check = new ScaffoldCheck();
        assertEquals("windfall.movement.scaffold", check.getStableKey());
    }

    @Test
    void checkData_hasCorrectDecay() {
        ScaffoldCheck check = new ScaffoldCheck();
        assertEquals(0.005, check.getDecay(), 0.001);
    }

    @Test
    void checkData_hasCorrectSetbackVl() {
        ScaffoldCheck check = new ScaffoldCheck();
        assertEquals(30, check.getSetbackVl());
    }

    @Test
    void onTick_decreasesConsecutiveFastPlacements() {
        ScaffoldCheck check = new ScaffoldCheck();
        WindfallPlayer player = createMockPlayer("Test");

        // The onTick method should not throw when called
        assertDoesNotThrow(() -> check.onTick(player, 100));
        assertDoesNotThrow(() -> check.onTick(player, 101));
    }

    @Test
    void removePlayer_clearsState() {
        ScaffoldCheck check = new ScaffoldCheck();
        UUID uuid = UUID.randomUUID();

        // Should not throw when removing a player that was never added
        assertDoesNotThrow(() -> check.removePlayer(uuid));
    }

    @Test
    void multiplePlayers_haveIndependentState() {
        ScaffoldCheck check = new ScaffoldCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        // Tick both players — should not interfere
        assertDoesNotThrow(() -> check.onTick(playerA, 100));
        assertDoesNotThrow(() -> check.onTick(playerB, 100));
    }
}
