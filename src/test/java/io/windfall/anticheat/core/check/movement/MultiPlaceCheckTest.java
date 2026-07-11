package io.windfall.anticheat.core.check.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MultiPlaceCheckTest extends CheckTestBase {

    private MultiPlaceCheck createCheck() {
        return new MultiPlaceCheck();
    }

    @Test
    void constructor_readCheckData() {
        MultiPlaceCheck check = createCheck();
        assertEquals("Multi Place", check.getName());
        assertEquals("windfall.movement.multiplace", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        MultiPlaceCheck check = createCheck();
        Field field = MultiPlaceCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_placeCountIsPerPlayer() throws Exception {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = MultiPlaceCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field placesField = stateA.getClass().getDeclaredField("placesThisTick");
        placesField.setAccessible(true);
        placesField.setInt(stateA, 10);

        assertEquals(0, placesField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = MultiPlaceCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = MultiPlaceCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 4.0);

        assertEquals(4.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
