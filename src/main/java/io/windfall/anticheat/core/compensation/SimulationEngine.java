package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-scenario movement simulation engine.
 *
 * <p>When a movement packet arrives, this engine simulates multiple possible world states
 * and checks if the player's movement is valid under ANY of them. This defeats the
 * "pulse toggle" bypass where cheats alternate between legitimate and cheating behavior
 * across ticks.
 *
 * <p>How it works:
 * <ol>
 *   <li>Query {@link LatencyCompensator} for all unconfirmed world changes</li>
 *   <li>For each combination of confirmed/unconfirmed changes, simulate the movement</li>
 *   <li>If the movement matches ANY scenario, it's considered legitimate</li>
 *   <li>If NO scenario matches, the movement is flagged as suspicious</li>
 * </ol>
 *
 * <p>Supported world change types and their physics effects:
 * <ul>
 *   <li>{@link WorldChange.Type#BLOCK_BREAK} — removes ground support, triggers gravity simulation</li>
 *   <li>{@link WorldChange.Type#BLOCK_PLACE} — adds solid ground, allows landing</li>
 *   <li>{@link WorldChange.Type#BLOCK_SHIFT} — piston push: old block becomes air, new block appears</li>
 *   <li>{@link WorldChange.Type#VELOCITY} — applies server-sent knockback/explosion/pearl velocity</li>
 *   <li>{@link WorldChange.Type#POTION_EFFECT} — modifies gravity and air drag multipliers</li>
 * </ul>
 *
 * <p>The engine caps scenarios at {@value MAX_SCENARIOS} to prevent combinatorial explosion.
 * In practice, most ticks have 0-2 unconfirmed changes, so 2^n is small.
 *
 * <p>Performance: simulations are lightweight (position + gravity + friction math).
 * The engine reuses simulation contexts to minimize allocation.
 *
 * @see PingPongManager for tick confirmation tracking
 * @see LatencyCompensator for deferred world changes
 */
public final class SimulationEngine {

    /** Maximum number of scenarios to simulate per tick (prevents combinatorial explosion) */
    private static final int MAX_SCENARIOS = 16;

    /** Maximum position deviation (blocks) for a scenario to match */
    private static final double MATCH_THRESHOLD = 0.1;

    private final PingPongManager pingPongManager;
    private final LatencyCompensator latencyCompensator;

    public SimulationEngine(PingPongManager pingPongManager, LatencyCompensator latencyCompensator) {
        this.pingPongManager = pingPongManager;
        this.latencyCompensator = latencyCompensator;
    }

    /**
     * Simulates multiple world state scenarios and returns the best match.
     *
     * <p>Each scenario represents a possible state of the world that the client might be
     * seeing. The client might have processed some state changes but not others, depending
     * on network latency.
     *
     * @param player the player whose movement is being validated
     * @param actualX the player's reported X position
     * @param actualY the player's reported Y position
     * @param actualZ the player's reported Z position
     * @return the simulation result with the best matching scenario
     */
    public SimulationResult simulate(WindfallPlayer player, double actualX, double actualY, double actualZ) {
        int confirmedTick = pingPongManager.getConfirmedTick(player);
        int currentTick = pingPongManager.getCurrentTick(player);

        // No unconfirmed changes — single scenario with default physics
        if (confirmedTick >= currentTick) {
            double deviation = calculateDeviation(player, actualX, actualY, actualZ);
            boolean matches = deviation <= MATCH_THRESHOLD;
            return new SimulationResult(0, deviation, 1, matches);
        }

        // Query unconfirmed world changes from the latency compensator
        List<WorldChange> unconfirmedChanges = latencyCompensator.getUnconfirmedChanges(
            player.getUuid(), confirmedTick, currentTick);

        // No unconfirmed changes tracked — single scenario with default physics
        if (unconfirmedChanges.isEmpty()) {
            double deviation = calculateDeviation(player, actualX, actualY, actualZ);
            boolean matches = deviation <= MATCH_THRESHOLD;
            return new SimulationResult(0, deviation, 1, matches);
        }

        int changeCount = Math.min(unconfirmedChanges.size(), log2Floor(MAX_SCENARIOS));
        int scenarioCount = Math.min(1 << changeCount, MAX_SCENARIOS);

        double bestDeviation = Double.MAX_VALUE;
        int bestScenario = -1;

        for (int i = 0; i < scenarioCount; i++) {
            double deviation = simulateScenario(player, actualX, actualY, actualZ,
                unconfirmedChanges, i, changeCount);
            if (deviation < bestDeviation) {
                bestDeviation = deviation;
                bestScenario = i;
            }
        }

        boolean matches = bestDeviation <= MATCH_THRESHOLD;
        return new SimulationResult(bestScenario, bestDeviation, scenarioCount, matches);
    }

    /**
     * Simulates a single scenario where specific unconfirmed changes are applied or not.
     *
     * <p>Each bit in the scenario mask corresponds to one unconfirmed change.
     * If the bit is set, the change is applied to the simulated world state;
     * otherwise, it is skipped (the client hasn't seen it yet).
     *
     * <p>Physics are applied per-change type:
     * <ul>
     *   <li>{@code BLOCK_BREAK}: if the block was under the player's feet, gravity applies</li>
     *   <li>{@code BLOCK_PLACE}: if the block was where the player is, landing is simulated</li>
     *   <li>{@code BLOCK_SHIFT}: old position becomes air, new position gets the block</li>
     *   <li>{@code VELOCITY}: velocity is applied as a one-time offset</li>
     *   <li>{@code POTION_EFFECT}: gravity and drag multipliers are adjusted</li>
     * </ul>
     *
     * @param player the player
     * @param actualX reported X position
     * @param actualY reported Y position
     * @param actualZ reported Z position
     * @param changes the unconfirmed changes to replay
     * @param scenarioMask bitmask of which unconfirmed changes are applied
     * @param changeCount number of changes to consider (up to log2 of MAX_SCENARIOS)
     * @return position deviation for this scenario
     */
    private double simulateScenario(WindfallPlayer player, double actualX, double actualY, double actualZ,
                                     List<WorldChange> changes, int scenarioMask, int changeCount) {
        double predictedX = player.getLastX();
        double predictedY = player.getLastY();
        double predictedZ = player.getLastZ();
        double deltaY = player.getDeltaY();
        boolean onGround = player.isOnGround();

        // Physics modifiers (accumulate across applied potion changes)
        double gravityMod = 1.0;
        double airDragMod = 1.0;

        // Velocity accumulator (applied after gravity/friction)
        double velX = 0, velY = 0, velZ = 0;

        // Apply each change in the scenario mask
        for (int bit = 0; bit < changeCount; bit++) {
            boolean changeApplied = (scenarioMask & (1 << bit)) != 0;
            if (!changeApplied) continue;

            WorldChange change = changes.get(bit);
            switch (change.getType()) {
                case BLOCK_BREAK: {
                    // Block removed — if it was under the player, they lose ground support
                    if (onGround && isBlockBelowPlayer(predictedX, predictedY, predictedZ,
                            change.getBlockX(), change.getBlockY(), change.getBlockZ())) {
                        onGround = false;
                    }
                    break;
                }
                case BLOCK_PLACE: {
                    // Block placed — if it's where the player's feet are, they land
                    if (!onGround && isBlockAtFeet(predictedX, predictedY, predictedZ,
                            change.getBlockX(), change.getBlockY(), change.getBlockZ())) {
                        onGround = true;
                        predictedY = change.getBlockY() + 1.0;
                        deltaY = 0;
                    }
                    break;
                }
                case BLOCK_SHIFT: {
                    // Piston push: old position becomes air, new position gets the block
                    if (onGround && isBlockBelowPlayer(predictedX, predictedY, predictedZ,
                            change.getOldX(), change.getOldY(), change.getOldZ())) {
                        onGround = false;
                    }
                    if (!onGround && isBlockAtFeet(predictedX, predictedY, predictedZ,
                            change.getBlockX(), change.getBlockY(), change.getBlockZ())) {
                        onGround = true;
                        predictedY = change.getBlockY() + 1.0;
                        deltaY = 0;
                    }
                    break;
                }
                case VELOCITY: {
                    // Server-sent velocity (knockback, explosion, ender pearl)
                    velX += change.getVelocityX();
                    velY += change.getVelocityY();
                    velZ += change.getVelocityZ();
                    break;
                }
                case POTION_EFFECT: {
                    // Potion effect modifiers accumulate (slow falling, levitation, etc.)
                    gravityMod *= change.getGravityMod();
                    airDragMod *= change.getAirDragMod();
                    break;
                }
            }
        }

        // Apply physics based on final state
        double gravity = PhysicsConstants.GRAVITY * gravityMod;
        double airDrag = PhysicsConstants.AIR_DRAG * airDragMod;

        if (!onGround) {
            // Airborne: apply gravity, then air drag
            predictedY += (deltaY - gravity) * airDrag;
        }

        // Apply accumulated velocity (after gravity/friction)
        predictedX += velX;
        predictedY += velY;
        predictedZ += velZ;

        return Math.sqrt(
            (actualX - predictedX) * (actualX - predictedX) +
            (actualY - predictedY) * (actualY - predictedY) +
            (actualZ - predictedZ) * (actualZ - predictedZ)
        );
    }

    /**
     * Calculates the deviation between predicted and actual position using default physics.
     * Used when no unconfirmed changes exist (single-scenario path).
     */
    private double calculateDeviation(WindfallPlayer player, double actualX, double actualY, double actualZ) {
        double predictedX = player.getLastX() + player.getDeltaX();
        double predictedY = player.getLastY() + player.getDeltaY();
        double predictedZ = player.getLastZ() + player.getDeltaZ();

        return Math.sqrt(
            (actualX - predictedX) * (actualX - predictedX) +
            (actualY - predictedY) * (actualY - predictedY) +
            (actualZ - predictedZ) * (actualZ - predictedZ)
        );
    }

    /**
     * Returns true if multi-scenario simulation is needed for this player.
     * Optimization: skip simulation when all ticks are confirmed.
     */
    public boolean needsSimulation(WindfallPlayer player) {
        int confirmedTick = pingPongManager.getConfirmedTick(player);
        int currentTick = pingPongManager.getCurrentTick(player);
        return confirmedTick < currentTick;
    }

    // === GEOMETRY HELPERS ===

    /**
     * Checks if a block at (bx,by,bz) is directly below the player's feet.
     * The block is "below" if it's at the player's Y-1 level and within horizontal reach.
     */
    private static boolean isBlockBelowPlayer(double px, double py, double pz,
                                               int bx, int by, int bz) {
        int playerBlockY = (int) Math.floor(py - 0.01); // Just below feet
        return by == playerBlockY &&
            Math.abs(px - (bx + 0.5)) < 1.0 &&
            Math.abs(pz - (bz + 0.5)) < 1.0;
    }

    /**
     * Checks if a block at (bx,by,bz) is at the player's feet level (same Y).
     * Used for BLOCK_PLACE landing detection.
     */
    private static boolean isBlockAtFeet(double px, double py, double pz,
                                          int bx, int by, int bz) {
        int playerBlockY = (int) Math.floor(py);
        return by == playerBlockY &&
            Math.abs(px - (bx + 0.5)) < 1.0 &&
            Math.abs(pz - (bz + 0.5)) < 1.0;
    }

    /**
     * Returns floor(log2(n)) — the number of bits needed to represent n scenarios.
     */
    private static int log2Floor(int n) {
        int bits = 0;
        while ((1 << bits) < n && bits < 30) bits++;
        return bits;
    }

    /**
     * Result of a multi-scenario simulation.
     */
    public static final class SimulationResult {
        /** Index of the best-matching scenario (-1 if none matched) */
        public final int bestScenario;
        /** Position deviation of the best scenario (blocks) */
        public final double bestDeviation;
        /** Total number of scenarios simulated */
        public final int scenarioCount;
        /** True if the best scenario matched within threshold */
        public final boolean matches;

        SimulationResult(int bestScenario, double bestDeviation, int scenarioCount, boolean matches) {
            this.bestScenario = bestScenario;
            this.bestDeviation = bestDeviation;
            this.scenarioCount = scenarioCount;
            this.matches = matches;
        }

        /** Returns true if multi-scenario simulation was actually needed (more than 1 scenario) */
        public boolean isMultiScenario() {
            return scenarioCount > 1;
        }
    }
}
