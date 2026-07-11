package io.windfall.anticheat.core.check.type;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.windfall.anticheat.core.player.WindfallPlayer;

// Mixin-style interface — implement only the packet direction your check needs
public interface PacketCheck {

    default void onPacketReceive(WindfallPlayer player, PacketReceiveEvent event) {}

    default void onPacketSend(WindfallPlayer player, PacketSendEvent event) {}
}
