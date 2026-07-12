package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class VelocityCheckTest extends CheckTestBase {

    private VelocityCheck createCheck() {
        return new VelocityCheck();
    }

    @Test
    void constructor_readCheckData() {
        VelocityCheck check = createCheck();
        assertEquals("Velocity A", check.getName());
        assertEquals("windfall.movement.velocity", check.getStableKey());
        assertEquals(30, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        VelocityCheck check = createCheck();
        Field field = VelocityCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_independentState() throws Exception {
        VelocityCheck check = createCheck();
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        WindfallPlayer playerA = createMockPlayer("Alice", uuidA);
        WindfallPlayer playerB = createMockPlayer("Bob", uuidB);

        java.lang.reflect.Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        assertNotSame(stateA, stateB);

        Field velocityActiveField = stateA.getClass().getDeclaredField("velocityActive");
        velocityActiveField.setAccessible(true);
        velocityActiveField.setBoolean(stateA, true);

        assertFalse(velocityActiveField.getBoolean(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        VelocityCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = VelocityCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void removePlayer_clearsStateForOnePlayer() throws Exception {
        VelocityCheck check = createCheck();
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        WindfallPlayer playerA = createMockPlayer("Alice", uuidA);
        WindfallPlayer playerB = createMockPlayer("Bob", uuidB);

        java.lang.reflect.Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = VelocityCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());

        check.removePlayer(uuidA);

        assertEquals(1, stateMap.size());
        assertFalse(stateMap.containsKey(uuidA));
        assertTrue(stateMap.containsKey(uuidB));
    }

    @Test
    void removePlayer_doesNotAffectOtherPlayers() throws Exception {
        VelocityCheck check = createCheck();
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        WindfallPlayer playerA = createMockPlayer("Alice", uuidA);
        WindfallPlayer playerB = createMockPlayer("Bob", uuidB);

        java.lang.reflect.Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        check.removePlayer(uuidA);

        Field velocityActiveField = stateB.getClass().getDeclaredField("velocityActive");
        velocityActiveField.setAccessible(true);
        assertFalse(velocityActiveField.getBoolean(stateB));

        Object stateBAfter = getStateMethod.invoke(check, playerB);
        assertSame(stateB, stateBAfter);
    }

    @Test
    void buffers_arePerPlayer() {
        VelocityCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 2.5);

        assertEquals(2.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
