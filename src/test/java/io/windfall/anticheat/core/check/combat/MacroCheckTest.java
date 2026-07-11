package io.windfall.anticheat.core.check.combat;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.MacroCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MacroCheckTest extends CheckTestBase {

    private MacroCheck createCheck() {
        return new MacroCheck();
    }

    @Test
    void constructor_readCheckData() {
        MacroCheck check = createCheck();
        assertEquals("Macro A", check.getName());
        assertEquals("windfall.combat.macro", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        MacroCheck check = createCheck();
        Field field = MacroCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_patternBufferIsPerPlayer() throws Exception {
        MacroCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = MacroCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field patternBufferField = stateA.getClass().getDeclaredField("patternBuffer");
        patternBufferField.setAccessible(true);
        StringBuilder bufA = (StringBuilder) patternBufferField.get(stateA);
        bufA.append("PPPPPPPPP");

        StringBuilder bufB = (StringBuilder) patternBufferField.get(stateB);
        assertEquals(0, bufB.length());
    }

    @Test
    void perPlayerState_movementPatternsIsPerPlayer() throws Exception {
        MacroCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = MacroCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field patternsField = stateA.getClass().getDeclaredField("movementPatterns");
        patternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> patternsA = (java.util.Map<String, Integer>) patternsField.get(stateA);
        patternsA.put("PPPPP", 10);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> patternsB = (java.util.Map<String, Integer>) patternsField.get(stateB);
        assertTrue(patternsB.isEmpty());
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        MacroCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = MacroCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = MacroCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        MacroCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 3.0);

        assertEquals(3.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
