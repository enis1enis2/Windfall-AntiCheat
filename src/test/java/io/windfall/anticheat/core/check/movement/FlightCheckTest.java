package io.windfall.anticheat.core.check.movement;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.FlightCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class FlightCheckTest extends CheckTestBase {

    private FlightCheck createCheck() {
        return new FlightCheck();
    }

    @Test
    void constructor_readCheckData() {
        FlightCheck check = createCheck();
        assertEquals("Fly A", check.getName());
        assertEquals("windfall.movement.fly", check.getStableKey());
        assertEquals(15, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        FlightCheck check = createCheck();
        Field field = FlightCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_expectedDeltaYIsPerPlayer() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = FlightCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field deltaYField = stateA.getClass().getDeclaredField("expectedDeltaY");
        deltaYField.setAccessible(true);
        deltaYField.setDouble(stateA, -0.08);

        assertEquals(0.0, deltaYField.getDouble(stateB));
    }

    @Test
    void perPlayerState_hoverTicksIsPerPlayer() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = FlightCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field hoverTicksField = stateA.getClass().getDeclaredField("hoverTicks");
        hoverTicksField.setAccessible(true);
        hoverTicksField.setInt(stateA, 25);

        assertEquals(0, hoverTicksField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = FlightCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        Field stateMapField = FlightCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
    }

    @Test
    void buffers_arePerPlayer() {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 3.5);

        assertEquals(3.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }

    // === no-fall sub-check ===

    /**
     * The no-fall sub-check used to sit behind an {@code !currentOnGround} guard and was only
     * invoked after an on-ground early return, so its {@code flagWithSetback} branch could never
     * run. These tests drive the sub-check directly to pin the reachable behaviour.
     */
    @Test
    void noFall_spoofedGroundAfterLongFall_isDetected() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");
        Object state = state(check, player);

        // Fall starts high and descends past the distance threshold while still reporting airborne
        runNoFall(check, player, state, false, -0.9, 80.0, 79.1);
        runNoFall(check, player, state, false, -0.9, 79.1, 74.0);
        // ... then the client starts claiming on-ground mid-descent, which must reach the
        // setback path rather than being treated as a normal landing
        runNoFall(check, player, state, true, -0.9, 74.0, 73.1);

        assertTrue(check.getViolationLevel(player) > 0,
            "Claiming on-ground mid-descent must register a violation");
    }

    @Test
    void noFall_landingAfterLongFall_isDetected() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");
        Object state = state(check, player);

        runNoFall(check, player, state, false, -0.9, 80.0, 79.1);
        runNoFall(check, player, state, false, -0.9, 79.1, 74.0);
        runNoFall(check, player, state, true, -0.2, 74.0, 74.0);

        assertTrue(check.getViolationLevel(player) > 0,
            "Landing after a long fast fall must register a violation");
    }

    @Test
    void noFall_shortDrop_isNotFlagged() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");
        Object state = state(check, player);

        // Only 2 blocks total, below NO_FALL_DISTANCE
        runNoFall(check, player, state, false, -0.9, 65.0, 64.1);
        runNoFall(check, player, state, false, -0.9, 64.1, 63.0);
        runNoFall(check, player, state, true, -0.1, 63.0, 63.0);

        assertEquals(0, check.getViolationLevel(player),
            "A 2 block drop is below the no-fall distance threshold");
    }

    @Test
    void noFall_shortAscent_isNotTrackedAsFalling() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");
        Object state = state(check, player);

        // Jumping upward must not seed a fall origin
        runNoFall(check, player, state, false, 0.42, 64.0, 64.42);

        Field falling = field(state, "falling");
        assertEquals(false, falling.getBoolean(state));
    }

    @Test
    void noFall_fallStateClearedOnLegitimateLanding() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");
        Object state = state(check, player);

        runNoFall(check, player, state, false, -0.9, 80.0, 79.1);
        assertEquals(true, field(state, "falling").getBoolean(state));

        runNoFall(check, player, state, true, -0.05, 79.1, 79.0);

        assertEquals(false, field(state, "falling").getBoolean(state));
    }

    // === helpers ===

    private Object state(FlightCheck check, WindfallPlayer player) throws Exception {
        java.lang.reflect.Method getState =
            FlightCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getState.setAccessible(true);
        return getState.invoke(check, player);
    }

    private Field field(Object state, String name) throws Exception {
        Field f = state.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private void runNoFall(FlightCheck check, WindfallPlayer player, Object state, boolean onGround,
                           double deltaY, double lastY, double currentY) throws Exception {
        java.lang.reflect.Method method = FlightCheck.class.getDeclaredMethod(
            "handleNoFall", WindfallPlayer.class, state.getClass(), boolean.class,
            double.class, double.class, double.class);
        method.setAccessible(true);
        method.invoke(check, player, state, onGround, deltaY, lastY, currentY);
    }
}
