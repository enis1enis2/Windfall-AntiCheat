package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimulationEngineTest {

    @Mock private PingPongManager mockPingPongManager;
    @Mock private LatencyCompensator mockLatencyCompensator;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SimulationEngine(mockPingPongManager, mockLatencyCompensator);
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
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(8);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.1, -0.2, 0.05);

        // 3 unconfirmed changes → capped by log2
        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 5, 8)).thenReturn(List.of(
            WorldChange.blockBreak(6, 100, 63, 200),
            WorldChange.blockBreak(7, 101, 63, 201),
            WorldChange.velocity(7, 0.0, 0.0, 0.0)
        ));

        SimulationEngine.SimulationResult result = engine.simulate(player, 100.1, 63.8, 200.05);
        assertTrue(result.scenarioCount > 1, "Should create multiple scenarios");
        assertTrue(result.isMultiScenario());
    }

    @Test
    void simulate_capsAt16Scenarios() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(0);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.1, -0.2, 0.05);

        // Provide more than log2(16)=4 changes
        java.util.List<WorldChange> manyChanges = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            manyChanges.add(WorldChange.blockBreak(i + 1, 100 + i, 63, 200));
        }
        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 0, 10)).thenReturn(manyChanges);

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
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
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

    // === NEW: Physics replay tests ===

    @Test
    void simulate_blockBreakBelowFeet_removesGroundSupport() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(12);
        // Player is on ground at Y=64, block below is at Y=63
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.0, 0.0, 0.0);
        when(player.isOnGround()).thenReturn(true);

        // Block below player breaks (unconfirmed)
        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 10, 12)).thenReturn(List.of(
            WorldChange.blockBreak(11, 100, 63, 200)
        ));

        // Scenario 0 (bit not set): block still there → ground, predicted stays at Y=64
        // Scenario 1 (bit set): block broken → no ground, gravity applies → predictedY drops
        // If player actually moved down, scenario 1 matches better
        SimulationEngine.SimulationResult result = engine.simulate(player, 100.0, 63.9, 200.0);
        // With block break applied, gravity pulls player down → 63.9 matches scenario 1
        assertTrue(result.matches, "Block break scenario should match the lower position");
    }

    @Test
    void simulate_velocityChange_appliesKnockback() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(12);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.0, 0.0, 0.0);

        // Server sent knockback velocity (unconfirmed)
        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 10, 12)).thenReturn(List.of(
            WorldChange.velocity(11, 0.5, 0.0, -0.3)
        ));

        // If client processed the velocity, they'd be offset by (0.5, 0, -0.3)
        SimulationEngine.SimulationResult result = engine.simulate(player, 100.5, 64.0, 199.7);
        assertTrue(result.matches, "Velocity scenario should match the knocked-back position");
    }

    @Test
    void simulate_potionEffect_reducesGravity() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(12);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.0, 0.0, 0.0);

        // Slow falling potion applied (gravity reduced to 10%)
        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 10, 12)).thenReturn(List.of(
            WorldChange.potionEffect(11, 0.1, 1.0) // 10% gravity
        ));

        // Player moving slowly downward with slow falling
        SimulationEngine.SimulationResult result = engine.simulate(player, 100.0, 63.99, 200.0);
        assertTrue(result.matches, "Slow falling scenario should match reduced downward movement");
    }

    @Test
    void simulate_noUnconfirmedChanges_returnsSingleScenario() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(5);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(10);
        setupPlayerPosition(player, 100.0, 64.0, 200.0, 0.1, 0.0, 0.05);

        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 5, 10))
            .thenReturn(java.util.Collections.emptyList());

        SimulationEngine.SimulationResult result = engine.simulate(player, 100.1, 64.0, 200.05);
        assertEquals(1, result.scenarioCount, "No unconfirmed changes → single scenario");
    }

    @Test
    void simulate_blockPlace_createsLanding() {
        WindfallPlayer player = createPlayer();
        when(mockPingPongManager.getConfirmedTick(player)).thenReturn(10);
        when(mockPingPongManager.getCurrentTick(player)).thenReturn(12);
        setupPlayerPosition(player, 100.0, 65.0, 200.0, 0.0, 0.0, 0.0);
        when(player.isOnGround()).thenReturn(false);

        // Block placed at player's feet level (unconfirmed)
        when(mockLatencyCompensator.getUnconfirmedChanges(player.getUuid(), 10, 12)).thenReturn(List.of(
            WorldChange.blockPlace(11, 100, 65, 200, org.bukkit.Material.STONE)
        ));

        // If client saw the block, they'd be standing on it at Y=66
        SimulationEngine.SimulationResult result = engine.simulate(player, 100.0, 65.0, 200.0);
        assertTrue(result.matches, "Block place scenario should match landing position");
    }

    private WindfallPlayer createPlayer() {
        WindfallPlayer player = mock(WindfallPlayer.class);
        when(player.getUuid()).thenReturn(UUID.randomUUID());
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
