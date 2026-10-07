package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class OmniSprintCheckTest extends CheckTestBase {

    private OmniSprintCheck createCheck() {
        return new OmniSprintCheck();
    }

    @Test
    void constructor_readCheckData() {
        OmniSprintCheck check = createCheck();
        assertEquals("Sprint B", check.getName());
        assertEquals("windfall.movement.sprint", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        OmniSprintCheck check = createCheck();
        Field field = OmniSprintCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_independentModel() throws Exception {
        OmniSprintCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice", UUID.randomUUID());
        WindfallPlayer playerB = createMockPlayer("Bob", UUID.randomUUID());

        java.lang.reflect.Method getStateMethod =
            OmniSprintCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        assertNotSame(stateA, stateB);

        Field modelField = stateA.getClass().getDeclaredField("model");
        modelField.setAccessible(true);
        Object modelA = modelField.get(stateA);
        Object modelB = modelField.get(stateB);
        assertNotSame(modelA, modelB);
    }

    @Test
    void removePlayer_clearsOnlyThatPlayersState() throws Exception {
        OmniSprintCheck check = createCheck();
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        WindfallPlayer playerA = createMockPlayer("Alice", uuidA);
        WindfallPlayer playerB = createMockPlayer("Bob", uuidB);

        java.lang.reflect.Method getStateMethod =
            OmniSprintCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        check.removePlayer(uuidA);

        Field stateMapField = OmniSprintCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(1, stateMap.size());
        assertTrue(stateMap.containsKey(uuidB));
    }
}