package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimulationEngineTest {

    @Mock private PingPongManager mockPingPongManager;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimulationEngine(mockPingPongManager);
    }

    @Test
    void needsSimulation_returnsFalseWhenAllTicksConfirmed() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        assertFalse(engine.needsSimulation(player));
    }

    @Test
    void needsSimulation_returnsTrueWhenUnconfirmedTicks() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(5);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        assertTrue(engine.needsSimulation(player));
    }

    @Test
    void simulate_allConfirmed_returnsSingleScenario() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.1, -0.2, 0.05);

        SimulationEngine.SimulationResult result = engine.simulate(player, 100.1, 63.8, 200.05);
        assertEquals(1, result.scenarioCount);
        assertFalse(result.isMultiScenario());
    }

    @Test
    void simulate_unconfirmedTicks_createsMultipleScenarios() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(5);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(7);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.1, -0.2, 0.05);

        SimulationEngine.SimulationResult result = engine.simulate(player, 100.1, 63.8, 200.05);
        // 2 unconfirmed changes → 2^2 = 4 scenarios
        assertEquals(4, result.scenarioCount);
        assertTrue(result.isMultiScenario());
    }

    @Test
    void simulate_capsAt16Scenarios() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(0);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.1, -0.2, 0.05);

        SimulationEngine.SimulationResult result = engine.simulate(player, 100.1, 63.8, 200.05);
        assertTrue(result.scenarioCount <= 16, "Should cap at 16 scenarios");
    }

    @Test
    void simulate_returnsBestMatchingScenario() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        // Perfect match: predicted = last + delta
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.5, 0.0, 0.3);

        SimulationEngine.SimulationResult result = engine.simulate(player, 100.5, 64.0, 200.3);
        assertTrue(result.matches, "Perfect match should return matches=true");
        assertEquals(0, result.bestScenario);
    }

    @Test
    void simulate_deviationBeyondThreshold_returnsNoMatch() {
        WindfallPlayer player = createPlayer();
        // Use unconfirmed ticks to trigger multi-scenario path with threshold check
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(5);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(7);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.0, 0.0, 0.0);

        // Actual position is 5 blocks away from predicted (100,64,200)
        SimulationEngine.SimulationResult result = engine.simulate(player, 105.0, 64.0, 200.0);
        assertFalse(result.matches, "Large deviation should not match");
    }

    @Test
    void simulate_resultContainsDeviationDistance() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.0, 0.0, 0.0);

        SimulationEngine.SimulationResult result = engine.simulate(player, 103.0, 64.0, 200.0);
        assertEquals(3.0, result.bestDeviation, 0.01, "Deviation should be ~3 blocks");
    }

    @Test
    void simulateResult_multiScenario_trueWhenCountGt1() {
        SimulationEngine.SimulationResult single = new SimulationEngine.SimulationResult(0, 0.01, 1, true);
        SimulationEngine.SimulationResult multi = new SimulationEngine.SimulationResult(0, 0.01, 4, true);
        assertFalse(single.isMultiScenario());
        assertTrue(multi.isMultiScenario());
    }

    private WindfallPlayer createPlayer() {
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getUuid()).thenReturn(java.util.UUID.randomUUID());
        return player;
    }

    private void setupPlayerPosition(WindfallPlayer player, double lastX, double lastY, double lastZ,
                                      double deltaX, double deltaY, double deltaZ) {
        when(player.getLastX()).thenReturn(lastX);
        when(player.getLastY()).thenReturn(lastY);
        when(player.getLastZ()).thenReturn(lastZ);
        when(player.getDeltaX()).thenReturn(deltaX);
        when(player.getDeltaY()).thenReturn(deltaY);
        when(player.getDeltaZ()).thenReturn(deltaZ);
        when(player.isOnGround()).thenReturn(false);
    }
}
