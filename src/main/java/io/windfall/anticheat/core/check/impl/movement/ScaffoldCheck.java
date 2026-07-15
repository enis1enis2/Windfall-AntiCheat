package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects scaffold (auto-bridge) hacks via three complementary detection vectors:
 * placement speed, tick-based tower detection, and rotation consistency.
 *
 * <p><b>Detection vector 1 — Placement speed:</b>
 * Tracks blocks placed per second in a sliding window and compares against
 * platform-specific thresholds (Java 12.0, Bedrock touch 8.0, controller 9.0, keyboard 10.0).
 * Sprinting players have a lower threshold (4.0 BPS) since sprinting reduces placement precision.
 *
 * <p><b>Detection vector 2 — Tick-based tower detection:</b>
 * Scaffold hacks placing blocks vertically (tower) place one block per game tick. Legitimate
 * tower building requires at least 6 ticks between placements (placing too fast is inhuman).
 * This vector tracks tick intervals and flags placements within {@value #TOWER_TICK_THRESHOLD}
 * ticks of each other.
 *
 * <p><b>Detection vector 3 — Rotation consistency:</b>
 * Scaffold bots rotate the player's view to face the block being placed, typically snapping
 * to exact yaw/pitch values at inhuman speeds. This vector detects rotation deltas that
 * exceed {@value #ROTATION_SNAP_THRESHOLD} degrees between consecutive placements.
 *
 * <p><b>Buffer logic:</b>
 * <ul>
 *   <li>Speed violations: +1.0 buffer, flag at &gt; 5.0</li>
 *   <li>Sprint violations: +0.5 buffer, flag at &gt; 3.0</li>
 *   <li>Tower violations: +1.5 buffer, flag at &gt; 4.0</li>
 *   <li>Rotation violations: +1.0 buffer, flag at &gt; 5.0</li>
 *   <li>Bedrock: +0.5 buffer, flag at &gt; 8.0 (higher threshold for latency)</li>
 * </ul>
 *
 * <p><b>Compatibility:</b> Uses {@link CompatFlag#RELAX_ON_MISMATCH} with 1.3x multiplier.
 * Tower detection uses tick-based timing (via {@link #onTick}), rotation uses
 * {@link System#currentTimeMillis()} for packet timestamps.
 *
 * @see MultiPlaceCheck — companion check for per-tick placement rate
 * @see InvalidPlaceCheck — companion check for occupied-block violations
 */
@CheckData(name = "Scaffold A", stableKey = "windfall.movement.scaffold", decay = 0.005, setbackVl = 30, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3, disableOnFolia = false)
public class ScaffoldCheck extends Check implements PacketCheck {

    // === Speed detection thresholds ===
    private static final double JAVA_MAX_BLOCK_PLACE_PER_SECOND = 12.0;
    private static final double BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC = 8.0;
    private static final double BEDROCK_KB_MAX_BLOCKS_PER_SEC = 10.0;
    private static final double BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC = 9.0;
    private static final double SPRINTING_BLOCKS_PER_SEC_THRESHOLD = 4.0;
    private static final long PLACE_WINDOW_MS = 1000;

    // === Tower detection thresholds ===
    /** Minimum ticks between block placements for legitimate tower building */
    private static final int TOWER_TICK_THRESHOLD = 6;
    /** Number of consecutive fast placements before flagging tower */
    private static final int TOWER_FAST_PLACEMENT_THRESHOLD = 3;

    // === Rotation consistency thresholds ===
    /** Maximum yaw/pitch delta (degrees) between consecutive placements for legit players */
    private static final float ROTATION_SNAP_THRESHOLD = 45.0f;
    /** Number of rotation snap violations before flagging */
    private static final int ROTATION_VIOLATION_THRESHOLD = 3;

    /**
     * Per-player mutable state for tracking scaffold indicators.
     */
    private static final class PlayerState {
        // === Speed tracking ===
        int blocksPlacedThisWindow;
        long windowStartTime;
        int lastSlot;
        double blocksPerSecondAccum;
        int samplesCollected;

        // === Tower detection (tick-based) ===
        /** Game tick of the last block placement */
        long lastPlacementTick;
        /** Count of consecutive placements within TOWER_TICK_THRESHOLD ticks */
        int consecutiveFastPlacements;

        // === Rotation consistency ===
        /** Yaw of the last block placement */
        float lastYaw;
        /** Pitch of the last block placement */
        float lastPitch;
        /** Timestamp of the last placement for rotation rate calculation */
        long lastPlacementTimeMs;
        /** Count of rotation snap violations in the current window */
        int rotationSnapViolations;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (isBlockPlacePacket(event)) {
            handleBlockPlace(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }

    /**
     * Per-tick callback for tower detection. Called from the tick loop to check
     * whether consecutive placements are happening faster than humanly possible.
     *
     * @param player the player to check
     * @param currentTick the current server tick count
     */
    public void onTick(WindfallPlayer player, long currentTick) {
        PlayerState state = getState(player);

        // If last placement was more than TOWER_TICK_THRESHOLD ticks ago, reset fast counter
        if (state.lastPlacementTick > 0 && currentTick - state.lastPlacementTick > TOWER_TICK_THRESHOLD) {
            state.consecutiveFastPlacements = 0;
        }
    }

    private boolean isBlockPlacePacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT;
    }

    /**
     * Handles a block placement event: runs all three detection vectors.
     *
     * @param player the player who placed a block
     */
    private void handleBlockPlace(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        // Vector 1: Speed-based detection (sliding window BPS)
        checkPlacementSpeed(player, state, now);

        // Vector 2: Tower detection (tick-based)
        checkTowerDetection(player, state);

        // Vector 3: Rotation consistency
        checkRotationConsistency(player, state, now);
    }

    /**
     * Vector 1: Measures blocks-per-second in a sliding window.
     * Flags if BPS exceeds platform-specific thresholds.
     */
    private void checkPlacementSpeed(WindfallPlayer player, PlayerState state, long now) {
        if (state.windowStartTime == 0 || now - state.windowStartTime > PLACE_WINDOW_MS) {
            if (state.blocksPlacedThisWindow > 0) {
                double bps = state.blocksPlacedThisWindow;
                state.blocksPerSecondAccum += bps;
                state.samplesCollected++;
            }
            state.blocksPlacedThisWindow = 0;
            state.windowStartTime = now;
        }

        state.blocksPlacedThisWindow++;
        double bps = state.blocksPlacedThisWindow / Math.max(1.0, (now - state.windowStartTime) / 1000.0);

        if (player.isBedrock()) {
            checkBedrockScaffold(player, bps);
        } else {
            checkJavaScaffold(player, bps);
        }
    }

    /**
     * Vector 2: Detects inhuman tower-building speed by measuring tick intervals
     * between consecutive block placements. Legitimate tower building requires
     * at least {@value #TOWER_TICK_THRESHOLD} ticks between placements.
     */
    private void checkTowerDetection(WindfallPlayer player, PlayerState state) {
        WindfallPlugin plugin = WindfallPlugin.getInstance();
        if (plugin == null) return;

        long currentTick = plugin.getCheckManager().getTickCounter();
        long tickDelta = currentTick - state.lastPlacementTick;

        if (state.lastPlacementTick > 0 && tickDelta <= TOWER_TICK_THRESHOLD) {
            state.consecutiveFastPlacements++;
            if (state.consecutiveFastPlacements >= TOWER_FAST_PLACEMENT_THRESHOLD) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.consecutiveFastPlacements = 0;
                }
            }
        } else {
            state.consecutiveFastPlacements = Math.max(0, state.consecutiveFastPlacements - 1);
        }

        state.lastPlacementTick = currentTick;
    }

    /**
     * Vector 3: Detects inhuman rotation snapping between consecutive placements.
     * Scaffold bots typically snap the player's view to face the block being placed,
     * producing large yaw/pitch deltas between consecutive placements.
     */
    private void checkRotationConsistency(WindfallPlayer player, PlayerState state, long now) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        if (state.lastPlacementTimeMs > 0) {
            float deltaYaw = Math.abs(yaw - state.lastYaw);
            // Normalize yaw delta to [0, 180]
            if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;
            float deltaPitch = Math.abs(pitch - state.lastPitch);

            if (deltaYaw > ROTATION_SNAP_THRESHOLD || deltaPitch > ROTATION_SNAP_THRESHOLD) {
                state.rotationSnapViolations++;
                if (state.rotationSnapViolations >= ROTATION_VIOLATION_THRESHOLD) {
                    increaseBuffer(player, 1.0);
                    if (getBuffer(player) > 5.0) {
                        flag(player);
                        resetBuffer(player);
                        state.rotationSnapViolations = 0;
                    }
                }
            } else {
                state.rotationSnapViolations = Math.max(0, state.rotationSnapViolations - 1);
            }
        }

        state.lastYaw = yaw;
        state.lastPitch = pitch;
        state.lastPlacementTimeMs = now;
    }

    /**
     * Checks Java-edition players for scaffold based on placement speed.
     */
    private void checkJavaScaffold(WindfallPlayer player, double bps) {
        if (bps > JAVA_MAX_BLOCK_PLACE_PER_SECOND) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (player.isSprinting() && bps > SPRINTING_BLOCKS_PER_SEC_THRESHOLD) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    /**
     * Checks Bedrock-edition players for scaffold based on input device thresholds.
     */
    private void checkBedrockScaffold(WindfallPlayer player, double bps) {
        BedrockInfo info = player.getBedrockInfo();
        if (info == null) return;

        double maxBps;
        if (info.isTouchDevice()) {
            maxBps = BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC;
        } else if (info.isController()) {
            maxBps = BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC;
        } else {
            maxBps = BEDROCK_KB_MAX_BLOCKS_PER_SEC;
        }

        if (bps > maxBps) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 8.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }
}
