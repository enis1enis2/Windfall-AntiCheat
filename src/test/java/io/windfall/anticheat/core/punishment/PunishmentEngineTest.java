package io.windfall.anticheat.core.punishment;

import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Covers punishment VL aggregation in {@link PunishmentEngine}.
 *
 * <p>Tier evaluation used the raw violation-level sum, which included checks configured as
 * {@code punishable: false}. A player could be banned purely from alert-only check VL.
 */
class PunishmentEngineTest {

    private static final String PUNISHABLE_CHECK = "windfall.movement.speed";
    private static final String ALERT_ONLY_CHECK = "windfall.combat.killaura";

    private MockedStatic<WindfallPlugin> pluginStaticMock;
    private WindfallPlugin mockPlugin;
    private WindfallConfig mockConfig;

    private final ConcurrentHashMap<String, Integer> violationLevels = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        pluginStaticMock = Mockito.mockStatic(WindfallPlugin.class);

        mockPlugin = Mockito.mock(WindfallPlugin.class);
        mockConfig = Mockito.mock(WindfallConfig.class);

        when(mockPlugin.getWindfallConfig()).thenReturn(mockConfig);
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());

        when(mockConfig.isPunishmentsEnabled()).thenReturn(true);
        when(mockConfig.getPunishmentWarnVl()).thenReturn(10);
        when(mockConfig.getPunishmentKickVl()).thenReturn(20);
        when(mockConfig.getPunishmentTempbanVl()).thenReturn(30);
        when(mockConfig.getPunishmentPermbanVl()).thenReturn(40);
        when(mockConfig.getPunishmentTempbanDuration()).thenReturn("1d");
        when(mockConfig.getPunishmentWarnMessage()).thenReturn("warn");
        when(mockConfig.getPunishmentKickMessage()).thenReturn("kick");
        when(mockConfig.getPunishmentTempbanReason()).thenReturn("temp");
        when(mockConfig.getPunishmentPermbanReason()).thenReturn("perm");

        when(mockConfig.isCheckPunishable(PUNISHABLE_CHECK)).thenReturn(true);
        when(mockConfig.isCheckPunishable(anyString()))
            .thenAnswer(i -> i.getArgument(0, String.class).equals(PUNISHABLE_CHECK));

        pluginStaticMock.when(WindfallPlugin::getInstance).thenReturn(mockPlugin);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        pluginStaticMock.close();
    }

    @Test
    void punishableVl_excludesNonPunishableChecks() {
        violationLevels.put(PUNISHABLE_CHECK, 5);
        violationLevels.put(ALERT_ONLY_CHECK, 100);

        PunishmentEngine engine = new PunishmentEngine(mockPlugin);

        assertEquals(5, punishableVl(engine, player()), "Only punishable check VL may count");
    }

    @Test
    void punishableVl_matchesRawTotal_whenAllChecksPunishable() {
        violationLevels.put(PUNISHABLE_CHECK, 5);
        violationLevels.put(ALERT_ONLY_CHECK, 3);
        // Mark the second check as punishable for this scenario
        when(mockConfig.isCheckPunishable(ALERT_ONLY_CHECK)).thenReturn(true);

        PunishmentEngine engine = new PunishmentEngine(mockPlugin);

        assertEquals(8, punishableVl(engine, player()));
    }

    @Test
    void alertOnlyVl_aloneNeverTriggersPunishment() {
        // 100 VL, well past permban, but from a non-punishable check
        violationLevels.put(ALERT_ONLY_CHECK, 100);

        PunishmentEngine engine = new PunishmentEngine(mockPlugin);

        assertTrue(punishableVl(engine, player()) < engine.getPermbanVl(),
            "Alert-only VL must not reach the permban tier");
    }

    @Test
    void punishableVl_isZeroWithNoViolations() {
        PunishmentEngine engine = new PunishmentEngine(mockPlugin);
        assertEquals(0, punishableVl(engine, player()));
    }

    private int punishableVl(PunishmentEngine engine, WindfallPlayer player) {
        java.lang.reflect.Method method = java.util.Arrays.stream(PunishmentEngine.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("getPunishableVl"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("getPunishableVl not found"));
        method.setAccessible(true);
        try {
            return (Integer) method.invoke(engine, player);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private WindfallPlayer player() {
        WindfallPlayer player = Mockito.mock(WindfallPlayer.class);
        when(player.getUuid()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Alice");
        when(player.getViolationLevels()).thenReturn(violationLevels);
        return player;
    }
}