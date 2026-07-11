package io.windfall.anticheat.core.check.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.SpeedCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SpeedCheckTest extends CheckTestBase {

    private SpeedCheck createCheck() {
        return new SpeedCheck();
    }

    @Test
    void constructor_readCheckData() {
        SpeedCheck check = createCheck();
        assertEquals("Speed A", check.getName());
        assertEquals("windfall.movement.speed", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        SpeedCheck check = createCheck();
        Field field = SpeedCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_maxObservedSpeedIsPerPlayer() throws Exception {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = SpeedCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field maxSpeedField = stateA.getClass().getDeclaredField("maxObservedSpeed");
        maxSpeedField.setAccessible(true);
        maxSpeedField.setDouble(stateA, 0.5);

        assertEquals(0.0, maxSpeedField.getDouble(stateB));
    }

    @Test
    void getMaxObservedSpeed_returnsPerPlayerValue() throws Exception {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = SpeedCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Field maxSpeedField = stateA.getClass().getDeclaredField("maxObservedSpeed");
        maxSpeedField.setAccessible(true);
        maxSpeedField.setDouble(stateA, 0.75);

        assertEquals(0.75, check.getMaxObservedSpeed(playerA), 0.001);
        assertEquals(0.0, check.getMaxObservedSpeed(playerB), 0.001);
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = SpeedCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = SpeedCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 4.0);

        assertEquals(4.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
