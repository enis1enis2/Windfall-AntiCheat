package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CrystalCheckTest extends CheckTestBase {

    private CrystalCheck createCheck() {
        return new CrystalCheck();
    }

    @Test
    void constructor_readCheckData() {
        CrystalCheck check = createCheck();
        assertEquals("Crystal A", check.getName());
        assertEquals("windfall.combat.crystal", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        CrystalCheck check = createCheck();
        Field field = CrystalCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_independentWindow() throws Exception {
        CrystalCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice", UUID.randomUUID());
        WindfallPlayer playerB = createMockPlayer("Bob", UUID.randomUUID());

        java.lang.reflect.Method getStateMethod =
            CrystalCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        assertNotSame(stateA, stateB);

        Field windowField = stateA.getClass().getDeclaredField("window");
        windowField.setAccessible(true);
        Object windowA = windowField.get(stateA);
        Object windowB = windowField.get(stateB);
        assertNotSame(windowA, windowB);
    }

    @Test
    void removePlayer_clearsOnlyThatPlayersState() throws Exception {
        CrystalCheck check = createCheck();
        UUID uuidA = UUID.randomUUID();
        UUID uuidB = UUID.randomUUID();
        WindfallPlayer playerA = createMockPlayer("Alice", uuidA);
        WindfallPlayer playerB = createMockPlayer("Bob", uuidB);

        java.lang.reflect.Method getStateMethod =
            CrystalCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        check.removePlayer(uuidA);

        Field stateMapField = CrystalCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(1, stateMap.size());
        assertTrue(stateMap.containsKey(uuidB));
        assertFalse(stateMap.containsKey(uuidA));
    }

    @Test
    void trustFactor_null_doesNotThrowInFlagPaths() {
        // createMockPlayer's getTrustFactor() is unstubbed (null). Drive the trust seams that
        // Check now calls during flag/flagWithSetback/reward — a missing trust model must not
        // break any of them for players that registered before the model existed.
        CrystalCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");

        check.flag(player);
        check.flagWithSetback(player);
        check.reward(player);
    }
}