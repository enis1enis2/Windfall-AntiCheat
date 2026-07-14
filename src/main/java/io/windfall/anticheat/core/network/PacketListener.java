package io.windfall.anticheat.core.network;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.bedrock.GeyserManager;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.bukkit.entity.Player;

/**
 * Central packet interceptor — updates player state and dispatches to all checks.
 *
 * <p>This listener sits between PacketEvents and the check system. It processes
 * raw packets to update {@link WindfallPlayer} state (position, rotation, velocity,
 * ground, sprint, abilities) before dispatching to {@link CheckManager}.
 *
 * <p>Incoming packets (client → server):
 * <ul>
 *   <li>Position/Rotation/Flying: update player coordinates, ground state, deltas</li>
 *   <li>KeepAlive: process transaction ping measurement</li>
 *   <li>InteractEntity: record attack timestamp</li>
 *   <li>First position after respawn: clear respawned flag</li>
 * </ul>
 *
 * <p>Outgoing packets (server → client):
 * <ul>
 *   <li>LOGIN_SUCCESS: create WindfallPlayer (earliest safe point for User data)</li>
 *   <li>EntityVelocity: capture server-sent knockback velocity</li>
 *   <li>PlayerPositionAndLook: record teleport destination for setbacks</li>
 *   <li>RESPAWN: reset player state, set respawned flag for ViaVersion desync protection</li>
 *   <li>Ping: send transaction for ping measurement</li>
 *   <li>PlayerAbilities: update flight state</li>
 * </ul>
 *
 * @see CheckManager for check dispatch
 * @see TransactionManager for ping measurement system
 */
public class PacketListener extends PacketListenerAbstract {

    private final WindfallPlugin plugin;
    private final PlayerManager playerManager;
    private final CheckManager checkManager;
    private final TransactionManager transactionManager;
    private final PingPongManager pingPongManager;

    public PacketListener(WindfallPlugin plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.checkManager = plugin.getCheckManager();
        this.transactionManager = plugin.getTransactionManager();
        this.pingPongManager = plugin.getPingPongManager();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;

        WindfallPlayer wp = playerManager.get(player.getUniqueId());
        PacketTypeCommon type = event.getPacketType();

        try {
            if (wp == null || !wp.isValid()) return;

            // Clear ViaVersion respawn flag on first position packet after respawn
            if (wp.isRespawned()) {
                if (type == PacketType.Play.Client.PLAYER_POSITION
                        || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    wp.setRespawned(false);
                }
            }

            if (type == PacketType.Play.Client.PLAYER_POSITION) {
                WrapperPlayClientPlayerPosition wrapper = new WrapperPlayClientPlayerPosition(event);
                Vector3d pos = wrapper.getPosition();
                wp.setPosition(pos.x, pos.y, pos.z);
                wp.setOnGround(wrapper.isOnGround());
                wp.setMovedSinceTick(true);
            } else if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
                Vector3d pos = wrapper.getPosition();
                wp.setPosition(pos.x, pos.y, pos.z);
                wp.setYaw(wrapper.getYaw());
                wp.setPitch(wrapper.getPitch());
                wp.setOnGround(wrapper.isOnGround());
                wp.setMovedSinceTick(true);
            } else if (type == PacketType.Play.Client.PLAYER_ROTATION) {
                WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
                wp.setYaw(wrapper.getYaw());
                wp.setPitch(wrapper.getPitch());
                wp.setOnGround(wrapper.isOnGround());
            } else if (type == PacketType.Play.Client.PLAYER_FLYING) {
                WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
                wp.setOnGround(wrapper.isOnGround());
            } else if (type == PacketType.Play.Client.KEEP_ALIVE) {
                WrapperPlayClientKeepAlive wrapper = new WrapperPlayClientKeepAlive(event);
            long id = wrapper.getId();
            // KeepAlive IDs are longs, mask to 16 bits for transaction system short IDs
            transactionManager.processTransaction(wp, (short) (id & 0xFFFF));
            } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
                wp.setLastAttackTime(System.currentTimeMillis());
            }

            checkManager.onPacketReceive(wp, event);

            // Feed block action packets to ActionData for movement check exemptions
            wp.getActionData().processReceive(event);
        } catch (Exception e) {
            plugin.getLogger().warning("Exception in onPacketReceive: " + e.getMessage());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;

        PacketTypeCommon type = event.getPacketType();

        try {
            if (type == PacketType.Login.Server.LOGIN_SUCCESS) {
                handleLogin(player, event);
                return;
            }

            WindfallPlayer wp = playerManager.get(player.getUniqueId());
            if (wp == null || !wp.isValid()) return;

            if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
                WrapperPlayServerEntityVelocity wrapper = new WrapperPlayServerEntityVelocity(event);
                // Only capture velocity for THIS player, not other entities
                if (wrapper.getEntityId() == player.getEntityId()) {
                    Vector3d vel = wrapper.getVelocity();
                    wp.setServerVelocityX(vel.x);
                    wp.setServerVelocityY(vel.y);
                    wp.setServerVelocityZ(vel.z);
                    wp.setVelocityReceived(true);
                }
            } else if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
                WrapperPlayServerPlayerPositionAndLook wrapper = new WrapperPlayServerPlayerPositionAndLook(event);
                wp.setTeleportPosition(wrapper.getX(), wrapper.getY(), wrapper.getZ());
            // On respawn, reset to origin — server teleports player next tick
            } else if (type == PacketType.Play.Server.RESPAWN) {
                wp.setPosition(0, 0, 0);
                wp.setOnGround(true);
                wp.setSprinting(false);
                wp.setSneaking(false);
                wp.setRespawned(true);
            } else if (type == PacketType.Play.Server.PING) {
                transactionManager.sendTransaction(wp);
            } else if (type == PacketType.Play.Server.PLAYER_ABILITIES) {
                WrapperPlayServerPlayerAbilities wrapper = new WrapperPlayServerPlayerAbilities(event);
                wp.setFlying(wrapper.isFlying());
                wp.setAllowFlight(wrapper.isFlightAllowed());
            }

            checkManager.onPacketSend(wp, event);

            // Feed block change packets to ActionData for movement check exemptions
            wp.getActionData().processSend(event);
        } catch (Exception e) {
            plugin.getLogger().warning("Exception in onPacketSend: " + e.getMessage());
        }
    }

    // LOGIN_SUCCESS is earliest safe point to create WindfallPlayer — before this, User data is incomplete
    private void handleLogin(Player player, PacketSendEvent event) {
        if (playerManager.get(player.getUniqueId()) != null) return;

        User user = event.getUser();
        if (user == null) return;

        WindfallPlayer wp = new WindfallPlayer(player, user);
        playerManager.add(wp);

        ClientVersion version = user.getClientVersion();
        if (version != null) {
            wp.setClientVersion(version);
            wp.setProtocolVersion(version.getProtocolVersion());
        }

        GeyserManager geyserManager = plugin.getGeyserManager();
        // Check Geyser at login only — Bedrock status doesn't change mid-session
        if (geyserManager != null && geyserManager.isGeyserPresent()) {
            BedrockInfo bedrockInfo = geyserManager.getBedrockInfo(player.getUniqueId());
            if (bedrockInfo != null) {
                wp.setBedrockInfo(bedrockInfo);
            }
        }
    }
}
