package io.windfall.anticheat.core.check.inventory;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.inventory.InventoryCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class InventoryCheckTest extends CheckTestBase {

    private InventoryCheck createCheck() {
        return new InventoryCheck();
    }

    @Test
    void constructor_readCheckData() {
        InventoryCheck check = createCheck();
        assertEquals("Inventory A", check.getName());
        assertEquals("windfall.inventory.inventory", check.getStableKey());
        assertEquals(15, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        InventoryCheck check = createCheck();
        Field field = InventoryCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_clickCountIsPerPlayer() throws Exception {
        InventoryCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = InventoryCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field clickCountField = stateA.getClass().getDeclaredField("clickCount");
        clickCountField.setAccessible(true);
        clickCountField.setInt(stateA, 10);

        assertEquals(0, clickCountField.getInt(stateB));
    }

    @Test
    void perPlayerState_secondClickCountIsPerPlayer() throws Exception {
        InventoryCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = InventoryCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field clicksThisSecondField = stateA.getClass().getDeclaredField("clicksThisSecond");
        clicksThisSecondField.setAccessible(true);
        clicksThisSecondField.setInt(stateA, 25);

        assertEquals(0, clicksThisSecondField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        InventoryCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = InventoryCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = InventoryCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        InventoryCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 1.5);

        assertEquals(1.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
