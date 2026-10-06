package io.windfall.anticheat.core.check;

import io.windfall.anticheat.core.check.impl.movement.MultiBreakCheck;
import io.windfall.anticheat.core.check.impl.movement.NoSwingCheck;
import io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckBaseTest extends CheckTestBase {

    @Test
    void constructor_readCheckData_name() {
        NoSwingCheck check = new NoSwingCheck();
        assertEquals("No Swing A", check.getName());
    }

    @Test
    void constructor_readCheckData_stableKey() {
        NoSwingCheck check = new NoSwingCheck();
        assertEquals("windfall.movement.noswing", check.getStableKey());
    }

    @Test
    void constructor_readCheckData_setbackVl() {
        NoSwingCheck check = new NoSwingCheck();
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void constructor_readCheckData_maxVl_fromConfig() {
        when(mockConfig.getCheckMaxVl("windfall.movement.noswing")).thenReturn(50);
        NoSwingCheck check = new NoSwingCheck();
        assertEquals(50, check.getMaxVl());
    }

    @Test
    void constructor_readCheckData_enabled_fromConfig() {
        when(mockConfig.isCheckEnabled("windfall.movement.noswing")).thenReturn(false);
        NoSwingCheck check = new NoSwingCheck();
        assertFalse(check.isEnabled());
    }

    @Test
    void constructor_readCheckData_punishable_fromConfig() {
        when(mockConfig.isCheckPunishable("windfall.movement.noswing")).thenReturn(false);
        NoSwingCheck check = new NoSwingCheck();
        assertFalse(check.isPunishable());
    }

    @Test
    void setters_updateTunables() {
        NoSwingCheck check = new NoSwingCheck();

        check.setMaxVl(42);
        check.setSetbackVl(7);
        check.setDecay(0.5);

        assertEquals(42, check.getMaxVl());
        assertEquals(7, check.getSetbackVl());
        assertEquals(0.5, check.getDecay(), 1e-9);
    }

    @Test
    void reward_usesUpdatedDecay() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.setDecay(0.5);
        check.increaseBuffer(player, 1.0);
        check.reward(player);

        assertEquals(0.5, check.getBuffer(player), 1e-9);
    }

    @Test
    void increaseBuffer_addsToBuffer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.increaseBuffer(player, 2.5);
        assertEquals(2.5, check.getBuffer(player), 0.001);
    }

    @Test
    void increaseBuffer_accumulates() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.increaseBuffer(player, 1.0);
        check.increaseBuffer(player, 1.5);
        assertEquals(2.5, check.getBuffer(player), 0.001);
    }

    @Test
    void decreaseBuffer_subtractsFromBuffer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.increaseBuffer(player, 5.0);
        check.decreaseBuffer(player, 2.0);
        assertEquals(3.0, check.getBuffer(player), 0.001);
    }

    @Test
    void decreaseBuffer_floorsAtZero() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.increaseBuffer(player, 1.0);
        check.decreaseBuffer(player, 5.0);
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void resetBuffer_setsToZero() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.increaseBuffer(player, 10.0);
        check.resetBuffer(player);
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void getBuffer_returnsZeroForNewPlayer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void getViolationLevel_returnsZeroForNewPlayer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void flag_incrementsViolationLevel() {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.flag(player);

        assertEquals(1, check.getViolationLevel(player));
    }

    @Test
    void flag_capsAtMaxVl() {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");
        when(mockConfig.getCheckMaxVl("windfall.movement.multibreak")).thenReturn(3);

        MultiBreakCheck checkAtMax = new MultiBreakCheck();
        for (int i = 0; i < 10; i++) {
            checkAtMax.flag(player);
        }

        assertTrue(checkAtMax.getViolationLevel(player) <= 3);
    }

    @Test
    void flag_doesNothingWhenDisabled() {
        when(mockConfig.isCheckEnabled("windfall.movement.noswing")).thenReturn(false);
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.flag(player);

        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void flag_callsAlertManager() {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.flag(player);

        verify(mockSeverityManager).getScaledVlIncrement(player);
    }

    @Test
    void reward_doesNotDecrementViolationLevelWithinDecayInterval() {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.flag(player);
        check.flag(player);
        assertEquals(2, check.getViolationLevel(player));

        // Decrementing VL every tick drained punishment tiers within a second of one flag
        check.reward(player);
        assertEquals(2, check.getViolationLevel(player),
            "VL must hold during the decay grace period");
    }

    @Test
    void reward_decrementsViolationLevelAfterDecayInterval() throws Exception {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.flag(player);
        check.flag(player);
        assertEquals(2, check.getViolationLevel(player));

        expireVlDecayWindow(check);
        check.reward(player);

        assertEquals(1, check.getViolationLevel(player));
    }

    @Test
    void reward_decrementsAtMostOncePerInterval() throws Exception {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.flag(player);
        check.flag(player);
        check.flag(player);
        assertEquals(3, check.getViolationLevel(player));

        expireVlDecayWindow(check);
        check.reward(player);
        check.reward(player);
        check.reward(player);

        assertEquals(2, check.getViolationLevel(player),
            "Repeated reward calls inside one interval must not stack decrements");
    }

    /** Backdates the last VL decay so the next reward call is outside the grace window. */
    private void expireVlDecayWindow(MultiBreakCheck check) throws Exception {
        java.lang.reflect.Field field = Check.class.getDeclaredField("lastVlDecayMs");
        field.setAccessible(true);
        field.setLong(check, System.currentTimeMillis() - Check.VL_DECAY_INTERVAL_MS - 1L);
    }

    @Test
    void reward_floorsViolationLevelAtZero() {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.reward(player);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void reward_decaysBuffer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");

        check.increaseBuffer(player, 1.0);
        check.reward(player);
        // decay = 0.02 for NoSwingCheck
        assertEquals(0.98, check.getBuffer(player), 0.001);
    }

    @Test
    void separatePlayers_haveIndependentBuffers() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 5.0);

        assertEquals(5.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }

    @Test
    void separatePlayers_haveIndependentViolationLevels() {
        MultiBreakCheck check = new MultiBreakCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.flag(playerA);

        assertEquals(1, check.getViolationLevel(playerA));
        assertEquals(0, check.getViolationLevel(playerB));
    }
}
