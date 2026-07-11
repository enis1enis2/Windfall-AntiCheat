package io.windfall.anticheat.core.check.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.GroundSpoofCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class GroundSpoofCheckTest extends CheckTestBase {

    private GroundSpoofCheck createCheck() {
        return new GroundSpoofCheck();
    }

    @Test
    void constructor_readCheckData() {
        GroundSpoofCheck check = createCheck();
        assertEquals("Ground Spoof A", check.getName());
        assertEquals("windfall.movement.groundspoof", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        GroundSpoofCheck check = createCheck();
        Field field = GroundSpoofCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_falseGroundCountIsPerPlayer() throws Exception {
        GroundSpoofCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = GroundSpoofCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field falseGroundField = stateA.getClass().getDeclaredField("falseGroundCount");
        falseGroundField.setAccessible(true);
        falseGroundField.setInt(stateA, 10);

        assertEquals(0, falseGroundField.getInt(stateB));
    }

    @Test
    void perPlayerState_airTicksIsPerPlayer() throws Exception {
        GroundSpoofCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = GroundSpoofCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field airTicksField = stateA.getClass().getDeclaredField("airTicks");
        airTicksField.setAccessible(true);
        airTicksField.setInt(stateA, 50);

        assertEquals(0, airTicksField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        GroundSpoofCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = GroundSpoofCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = GroundSpoofCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        GroundSpoofCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 4.5);

        assertEquals(4.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
