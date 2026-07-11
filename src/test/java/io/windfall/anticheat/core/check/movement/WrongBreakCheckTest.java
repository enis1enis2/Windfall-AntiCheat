package io.windfall.anticheat.core.check.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.WrongBreakCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class WrongBreakCheckTest extends CheckTestBase {

    private WrongBreakCheck createCheck() {
        return new WrongBreakCheck();
    }

    @Test
    void constructor_readCheckData() {
        WrongBreakCheck check = createCheck();
        assertEquals("Wrong Break", check.getName());
        assertEquals("windfall.movement.wrongbreak", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        WrongBreakCheck check = createCheck();
        Field field = WrongBreakCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_breakPositionIsPerPlayer() throws Exception {
        WrongBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = WrongBreakCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field hasLastBreakField = stateA.getClass().getDeclaredField("hasLastBreak");
        hasLastBreakField.setAccessible(true);
        hasLastBreakField.setBoolean(stateA, true);

        Field lastBreakXField = stateA.getClass().getDeclaredField("lastBreakX");
        lastBreakXField.setAccessible(true);
        lastBreakXField.setDouble(stateA, 100.0);

        assertFalse(hasLastBreakField.getBoolean(stateB));
        assertEquals(0.0, lastBreakXField.getDouble(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        WrongBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = WrongBreakCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = WrongBreakCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        WrongBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 2.0);

        assertEquals(2.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
