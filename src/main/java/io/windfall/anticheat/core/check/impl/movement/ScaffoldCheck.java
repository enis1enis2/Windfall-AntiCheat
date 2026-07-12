package io.windfall.anticheat.core.check.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.CompatFlag;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects scaffold (auto-bridge) hacks by measuring block placement speed over time.
 *
 * <p>Scaffold hacks automatically place blocks beneath the player as they move, typically at a
 * rate significantly higher than what a legitimate player can achieve. This check tracks blocks
 * placed per second using a sliding window and compares against platform-specific maximums.
 *
 * <p><b>Detection algorithm:</b>
 * <ol>
 *   <li>Count block placements within a {@value #PLACE_WINDOW_MS} ms sliding window.</li>
 *   <li>Compute blocks-per-second (BPS) from the count and elapsed time.</li>
 *   <li>Compare against platform-specific thresholds:
 *     <ul>
 *       <li>Java: {@value #JAVA_MAX_BLOCK_PLACE_PER_SECOND} BPS (12.0)</li>
 *       <li>Bedrock keyboard: {@value #BEDROCK_KB_MAX_BLOCKS_PER_SEC} BPS (10.0)</li>
 *       <li>Bedrock controller: {@value #BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC} BPS (9.0)</li>
 *       <li>Bedrock touch: {@value #BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC} BPS (8.0)</li>
 *     </ul>
 *   </li>
 *   <li>Java players also have a secondary check: sprinting + BPS &gt; 4.0 is suspicious
 *       (scaffold while sprinting requires faster-than-normal placement).</li>
 * </ol>
 *
 * <p><b>Buffer logic:</b> Java violations add 1.0 (flag at &gt; 5.0); sprinting adds 0.5 (flag at
 * &gt; 3.0); Bedrock adds 0.5 with a higher flag threshold of 8.0 to account for platform latency.
 *
 * <p><b>Compatibility:</b> Marked {@link CompatFlag#FOLIA_UNSAFE} because it uses
 * {@link System#currentTimeMillis()} rather than tick-based timing. Also uses
 * {@link CompatFlag#RELAX_ON_MISMATCH} with a {@code relaxMultiplier} of 1.3 to reduce false
 * positives on high-latency connections.
 *
 * @see MultiPlaceCheck — companion check for per-tick placement rate
 * @see InvalidPlaceCheck — companion check for occupied-block and self-intersection violations
 */
@CheckData(name = "Scaffold A", stableKey = "windfall.movement.scaffold", decay = 0.005, setbackVl = 30, compat = {CompatFlag.FOLIA_UNSAFE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3, disableOnFolia = false)
public class ScaffoldCheck extends Check implements PacketCheck {

    /** Maximum blocks per second a legitimate Java player can place. */
    private static final double JAVA_MAX_BLOCK_PLACE_PER_SECOND = 12.0;

    /** Maximum blocks per second for Bedrock touch-device players (slower input method). */
    private static final double BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC = 8.0;

    /** Maximum blocks per second for Bedrock keyboard players. */
    private static final double BEDROCK_KB_MAX_BLOCKS_PER_SEC = 10.0;

    /** Maximum blocks per second for Bedrock controller players. */
    private static final double BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC = 9.0;

    /**
     * Secondary threshold for Java players: placing blocks faster than this while sprinting
     * is suspicious because sprinting reduces placement precision.
     */
    private static final double SPRINTING_BLOCKS_PER_SEC_THRESHOLD = 4.0;

    /**
     * Size of the rolling window for accumulated BPS samples. Currently unused in the
     * threshold logic but retained for future averaging enhancements.
     */
    private static final int ROLLING_WINDOW_SIZE = 10;

    /** Duration of the sliding placement window in milliseconds (1 second). */
    private static final long PLACE_WINDOW_MS = 1000;

    /**
     * Per-player mutable state for tracking block placement rate.
     */
    private static final class PlayerState {
        /** Number of blocks placed within the current window. */
        int blocksPlacedThisWindow;
        /** Start timestamp (ms) of the current sliding window. */
        long windowStartTime;
        /** Last observed hotbar slot (for future slot-change heuristics). */
        int lastSlot;
        /** Accumulated blocks-per-second values for computing average BPS. */
        double blocksPerSecondAccum;
        /** Number of BPS samples collected in the current session. */
        int samplesCollected;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    /**
     * Retrieves or lazily initialises the per-player state for this check.
     *
     * @param player the player whose state to retrieve
     * @return the current {@link PlayerState} for the player
     */
    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    /**
     * Removes cached state for a disconnected player to prevent memory leaks.
     *
     * @param uuid the UUID of the player being removed
     */
    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    /**
     * Processes incoming packets for this check.
     *
     * <p>Delegates to {@link #handleBlockPlace} when a block placement packet is detected.
     *
     * @param player the player associated with the packet
     * @param event  the incoming packet event
     */
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
     * Checks whether the packet is a block placement packet.
     *
     * @param event the incoming packet event
     * @return {@code true} if the packet type is {@link PacketType.Play.Client#PLAYER_BLOCK_PLACEMENT}
     */
    private boolean isBlockPlacePacket(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        return type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT;
    }

    /**
     * Handles a block placement event: updates the sliding window, computes BPS, and
     * dispatches to the appropriate platform-specific check.
     *
     * <p>When the current window expires, the previous window's BPS is recorded as a sample,
     * the counter is reset, and a new window starts. Real-time BPS is computed as
     * {@code count / elapsed_seconds} and passed to the platform checker.
     *
     * @param player the player who placed a block
     */
    private void handleBlockPlace(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        /* Start a new window if this is the first placement or the window has expired */
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

        /* Real-time BPS = blocks placed / seconds elapsed in current window */
        double bps = state.blocksPlacedThisWindow / Math.max(1.0, (now - state.windowStartTime) / 1000.0);

        if (player.isBedrock()) {
            checkBedrockScaffold(player, bps);
        } else {
            checkJavaScaffold(player, bps);
        }
    }

    /**
     * Checks Java-edition players for scaffold behaviour based on placement speed.
     *
     * <p>Two heuristics:
     * <ul>
     *   <li>Absolute speed: BPS &gt; {@value #JAVA_MAX_BLOCK_PLACE_PER_SECOND} (hard cheat indicator).</li>
     *   <li>Sprint speed: BPS &gt; {@value #SPRINTING_BLOCKS_PER_SEC_THRESHOLD} while sprinting
     *       (soft cheat indicator — harder to place precisely while sprinting).</li>
     * </ul>
     *
     * @param player the Java-edition player to check
     * @param bps    current blocks-per-second rate
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
     * Checks Bedrock-edition players for scaffold behaviour based on input device.
     *
     * <p>Different input methods have different maximum placement speeds:
     * <ul>
     *   <li>Touch: slowest — {@value #BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC} BPS</li>
     *   <li>Controller: moderate — {@value #BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC} BPS</li>
     *   <li>Keyboard: fastest — {@value #BEDROCK_KB_MAX_BLOCKS_PER_SEC} BPS</li>
     * </ul>
     *
     * <p>Uses a higher buffer threshold ({@code 8.0}) than Java to account for Bedrock
     * platform latency variability.
     *
     * @param player the Bedrock-edition player to check
     * @param bps    current blocks-per-second rate
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
