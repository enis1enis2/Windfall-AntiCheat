package io.windfall.anticheat.core.check.type;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.player.WindfallPlayer;

/**
 * Mixin-style interface for checks that only need one packet direction.
 *
 * <p>Implement only the method(s) you need — the other defaults to a no-op.
 * This avoids boilerplate when a check only listens to client-to-server packets
 * (e.g., {@code onPacketReceive}) or server-to-client packets (e.g., {@code onPacketSend}).
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyCheck extends Check implements PacketCheck {
 *     @Override
 *     public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
 *         // Only handle incoming packets
 *     }
 * }
 * }</pre>
 */
public interface PacketCheck {

    /** Called when a client-to-server packet is received. Override to inspect incoming packets. */
    default void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {}

    /** Called when a server-to-client packet is sent. Override to inspect outgoing packets. */
    default void onPacketSend(WindfallPlayer player, PacketSendEvent event) {}
}
