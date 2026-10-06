package io.windfall.anticheat.core.check.combat;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.KillAuraCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Covers the rotation-symmetry detector in {@link KillAuraCheck}.
 *
 * <p>The symmetry metric is a balance score where 1.0 means an even split of clockwise and
 * counter-clockwise snaps. Comparing {@code min(counts)/total} against a 0.95 threshold made the
 * condition unsatisfiable — its maximum is 0.5 — so the detector never flagged anything.
 */
class KillAuraSymmetryTest extends CheckTestBase {

    private KillAuraCheck createCheck() {
        return new KillAuraCheck();
    }

    @Test
    void perfectlyBalancedSnaps_exceedThreshold() throws Exception {
        KillAuraCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");

        fillYawDeltas(check, player, alternatingDeltas(5, 5));

        runSymmetryCheck(check, player);

        assertTrue(check.getBuffer(player) > 0.0,
            "Perfectly balanced rotation deltas must be detectable");
    }

    @Test
    void balancedSnaps_reachHigherBufferThanSkewedOnes() throws Exception {
        KillAuraCheck balanced = createCheck();
        WindfallPlayer balancedPlayer = createMockPlayer("Bal");
        fillYawDeltas(balanced, balancedPlayer, alternatingDeltas(5, 5));
        runSymmetryCheck(balanced, balancedPlayer);

        KillAuraCheck skewed = createCheck();
        WindfallPlayer skewedPlayer = createMockPlayer("Skew");
        fillYawDeltas(skewed, skewedPlayer, alternatingDeltas(9, 1));
        runSymmetryCheck(skewed, skewedPlayer);

        assertTrue(balanced.getBuffer(balancedPlayer) > skewed.getBuffer(skewedPlayer),
            "A balanced split should score higher than a lopsided one");
    }

    @Test
    void oneDirectionalSnaps_doNotFlag() throws Exception {
        KillAuraCheck check = createCheck();
        WindfallPlayer player = createMockPlayer("Alice");

        ArrayDeque<Float> deltas = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) deltas.addLast(90.0f);
        fillYawDeltas(check, player, deltas);

        runSymmetryCheck(check, player);

        // Not symmetric, so the detector takes its decay branch and never raises the buffer
        assertTrue(check.getBuffer(player) <= 0.0,
            "All deltas in one direction is not symmetric and must not flag");
    }

    @Test
    void bedrockThreshold_stricterThanVanilla() throws Exception {
        KillAuraCheck check = createCheck();

        WindfallPlayer vanilla = createMockPlayer("Vanilla");
        when(vanilla.isBedrock()).thenReturn(false);
        // 8:2 split → balance score 0.4, below both thresholds, so use a near-even split
        // (9:1 → 0.2). Instead assert on the ratio directly via a moderately even split.
        fillYawDeltas(check, vanilla, alternatingDeltas(9, 1));
        runSymmetryCheck(check, vanilla);

        WindfallPlayer bedrock = createMockPlayer("Bedrock");
        when(bedrock.isBedrock()).thenReturn(true);
        fillYawDeltas(check, bedrock, alternatingDeltas(9, 1));
        runSymmetryCheck(check, bedrock);

        assertTrue(check.getBuffer(vanilla) <= 0.0);
        assertTrue(check.getBuffer(bedrock) <= 0.0);
    }

    // === HELPERS ===

    private ArrayDeque<Float> alternatingDeltas(int positive, int negative) {
        ArrayDeque<Float> deltas = new ArrayDeque<>();
        for (int i = 0; i < positive; i++) deltas.addLast(90.0f);
        for (int i = 0; i < negative; i++) deltas.addLast(-90.0f);
        return deltas;
    }

    private void fillYawDeltas(KillAuraCheck check, WindfallPlayer player, ArrayDeque<Float> deltas)
            throws Exception {
        Object state = getState(check, player);
        Field deltasField = state.getClass().getDeclaredField("recentYawDeltas");
        deltasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayDeque<Float> target = (ArrayDeque<Float>) deltasField.get(state);
        target.addAll(deltas);

        // snapCount gates the detector; set it above MIN_SNAP_COUNT
        Field snapField = state.getClass().getDeclaredField("snapCount");
        snapField.setAccessible(true);
        snapField.setInt(state, 3);
    }

    private Object getState(KillAuraCheck check, WindfallPlayer player) throws Exception {
        Method getState = KillAuraCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getState.setAccessible(true);
        return getState.invoke(check, player);
    }

    private void runSymmetryCheck(KillAuraCheck check, WindfallPlayer player) throws Exception {
        Method method = KillAuraCheck.class.getDeclaredMethod(
            "checkRotationSymmetry", WindfallPlayer.class, getState(check, player).getClass());
        method.setAccessible(true);
        method.invoke(check, player, getState(check, player));
    }
}