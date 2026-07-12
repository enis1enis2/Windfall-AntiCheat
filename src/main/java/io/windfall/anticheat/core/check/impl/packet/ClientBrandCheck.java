package io.windfall.anticheat.core.check.impl.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckData;
import io.windfall.anticheat.core.check.type.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Detects known hacked clients by inspecting the channel brand plugin message.
 *
 * <p>When a client connects, it sends a plugin message on the {@code minecraft:brand}
 * (1.13+) or {@code MC|Brand} (pre-1.13) channel containing its client brand string.
 * Many cheat clients include identifiable strings in their brand (e.g., "wurst", "impact").
 *
 * <p>The brand string is normalized by converting to lowercase, stripping all non-alphanumeric
 * characters, then checking against a hardcoded list of known cheat client brand substrings.
 * A match triggers an immediate flag with no buffer (zero-tolerance).
 *
 * <p>Known cheat brands include: wurst, impact, moon, liquidbounce, koks, aristois, vape,
 * rusherhack, ghostly, exhibiton, phobos, fdp, salhack, rise, astolfo, blexware, hanabi,
 * cabbage, dotgod, sodium (when fake), meteor, inertia, xplus, tenacity, nextgen, crescent.
 *
 * <p>Setback at VL 10, decay disabled (0.0) — brand detection is a one-shot flag.
 *
 * @see Check for base violation system
 */
@CheckData(name = "Client Brand A", stableKey = "windfall.packet.brand", decay = 0.0, setbackVl = 10)
public class ClientBrandCheck extends Check implements PacketCheck {

    /**
     * Hardcoded list of known cheat client brand substrings (lowercase, alphanumeric only).
     * These are matched against the normalized client brand using {@code String.contains()}.
     */
    private static final List<String> SUSPICIOUS_BRANDS = Arrays.asList(
        "wurst", "impact", "moon", "liquidbounce", "koks", "aristois",
        "vape", "rusherhack", "ghostly", "exhibiton", "phobos",
        "fdp", "salhack", "rise", "astolfo", "blexware", "hanabi",
        "cabbage", "dotgod", "sodium", "meteor", "inertia",
        "xplus", "tenacity", "nextgen", "crescent"
    );

    /**
     * Processes incoming plugin messages to detect suspicious client brands.
     * Only processes packets on the {@code minecraft:brand} or {@code MC|Brand} channel.
     * The brand string is normalized (lowercase, alphanumeric-only) before matching.
     *
     * @param player the player who sent the plugin message
     * @param event  the received packet event
     */
    @Override
    public void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
        String channel = wrapper.getChannelName();

        if (!channel.equals("minecraft:brand") && !channel.equals("MC|Brand")) return;

        byte[] data = wrapper.getData();
        if (data == null || data.length == 0) return;

        /* Normalize brand: lowercase and strip all non-alphanumeric characters to prevent evasion
         * via unicode characters, spaces, or special characters in the brand string */
        String brand = new String(data, StandardCharsets.UTF_8).toLowerCase()
            .replaceAll("[^a-z0-9]", "");

        for (String suspicious : SUSPICIOUS_BRANDS) {
            if (brand.contains(suspicious)) {
                flag(player);
                var logger = io.windfall.anticheat.WindfallPlugin.getInstance().getLogger();
                logger.warning("[Client Brand A] " + player.getName()
                    + " detected suspicious brand: " + brand);
                return;
            }
        }
    }

    /** {@inheritDoc} No outgoing packet processing needed for brand checks */
    @Override
    public void onPacketSend(WindfallPlayer player, PacketSendEvent event) {
    }
}
