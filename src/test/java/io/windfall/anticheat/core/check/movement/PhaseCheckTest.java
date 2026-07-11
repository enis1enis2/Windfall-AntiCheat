package io.windfall.anticheat.core.check.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.PhaseCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PhaseCheckTest extends CheckTestBase {

    private PhaseCheck createCheck() {
        return new PhaseCheck();
    }

    @Test
    void constructor_readCheckData() {
        PhaseCheck check = createCheck();
        assertEquals("Phase A", check.getName());
        assertEquals("windfall.movement.phase", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        PhaseCheck check = createCheck();
        Field field = PhaseCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_clippingTicksIsPerPlayer() throws Exception {
        PhaseCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = PhaseCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field clippingField = stateA.getClass().getDeclaredField("clippingTicks");
        clippingField.setAccessible(true);
        clippingField.setInt(stateA, 10);

        assertEquals(0, clippingField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        PhaseCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = PhaseCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = PhaseCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        PhaseCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 2.0);

        assertEquals(2.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
