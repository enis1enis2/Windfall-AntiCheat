package io.windfall.anticheat.core.network;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import io.windfall.anticheat.WindfallPlugin;
import io.windfall.anticheat.core.bedrock.BedrockInfo;
import io.windfall.anticheat.core.bedrock.GeyserManager;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.check.impl.combat.ReachCheck;
import io.windfall.anticheat.core.compensation.LatencyCompensator;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.compensation.WorldChange;
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
            } else if (type == PacketType.Play.Client.PONG
                    || type == PacketType.Play.Client.WINDOW_CONFIRMATION) {
                /* Transaction response. Feed both managers: TransactionManager measures RTT and
                 * detects fabricated IDs, PingPongManager advances the confirmed-tick window that
                 * lag compensation depends on. Without the latter the window never moves, so
                 * ping-pong confirmation and every latency estimate derived from it stay at zero. */
                short responseId = readTransactionResponseId(event, type);
                if (responseId >= 0) {
                    boolean matched = transactionManager.processTransaction(wp, responseId);
                    pingPongManager.processPingResponse(wp, responseId);
                    checkManager.onTransactionResponse(wp, matched);
                }
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

                    /* Record the knockback for lag compensation. Without this record the client
                     * may legitimately be airborne from a knockback it has not received yet, but
                     * the simulation engine has no VELOCITY scenario to test against. */
                    LatencyCompensator compensator = plugin.getLatencyCompensator();
                    if (compensator != null) {
                        compensator.recordChange(player.getUniqueId(),
                            pingPongManager.getCurrentTick(wp),
                            WorldChange.velocity(pingPongManager.getCurrentTick(wp), vel.x, vel.y, vel.z));
                    }
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

            /* Keep ReachCheck's entity cache fed. Without spawn/move/destroy tracking the cache
             * stays empty, every attack resolves to an unknown target, and no reach sample is
             * ever taken — the check silently never fires. */
            trackEntities(event, type);

            checkManager.onPacketSend(wp, event);

            // Feed block change packets to ActionData for movement check exemptions
            wp.getActionData().processSend(event);
        } catch (Exception e) {
            plugin.getLogger().warning("Exception in onPacketSend: " + e.getMessage());
        }
    }

    // LOGIN_SUCCESS is earliest safe point to create WindfallPlayer — before this, User data is incomplete
    /**
     * Updates {@link io.windfall.anticheat.core.check.impl.combat.ReachCheck}'s entity cache
     * from the server→client entity packets.
     *
     * <p>All four packet shapes are handled because they split differently across protocol
     * versions: living entities (mob spawns) have their own packet, players spawn separately,
     * plain entities use the generic spawn, and movement arrives as either an absolute teleport
     * or a relative position sync depending on the version. Missing any one of them leaves part
     * of the world untracked.
     *
     * @param event the outgoing packet event
     * @param type  the packet type
     */
    private void trackEntities(PacketSendEvent event, PacketTypeCommon type) {
        try {
            if (type == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
                WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);
                Vector3d pos = wrapper.getPosition();
                ReachCheck.trackSpawn(wrapper.getEntityId(), wrapper.getEntityType(), pos.x, pos.y, pos.z);
            } else if (type == PacketType.Play.Server.SPAWN_ENTITY) {
                WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
                Vector3d pos = wrapper.getPosition();
                ReachCheck.trackSpawn(wrapper.getEntityId(), wrapper.getEntityType(), pos.x, pos.y, pos.z);
            } else if (type == PacketType.Play.Server.SPAWN_PLAYER) {
                WrapperPlayServerSpawnPlayer wrapper = new WrapperPlayServerSpawnPlayer(event);
                Vector3d pos = wrapper.getPosition();
                ReachCheck.trackSpawn(wrapper.getEntityId(), EntityTypes.PLAYER, pos.x, pos.y, pos.z);
            } else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
                WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(event);
                applyEntityMove(wrapper.getEntityId(), wrapper.getPosition(), wrapper.getRelativeFlags());
            } else if (type == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
                WrapperPlayServerEntityPositionSync wrapper = new WrapperPlayServerEntityPositionSync(event);
                applyEntityMove(wrapper.getId(), wrapper.getValues().getPosition(), RelativeFlag.NONE);
            } else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
                WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(event);
                for (int entityId : wrapper.getEntityIds()) {
                    ReachCheck.trackRemove(entityId);
                }
            }
        } catch (Exception e) {
            // Entity tracking is best-effort: an undecodable packet must not break the send path
        }
    }

    /**
     * Stores an entity position, resolving relative teleports against its last known position.
     *
     * <p>A teleport packet may carry per-axis relative flags, in which case the coordinates are
     * deltas. Storing those as absolute positions would put the entity somewhere it was never at
     * and produce bogus reach distances.
     *
     * @param entityId the entity's network ID
     * @param pos      the coordinates as sent
     * @param flags    which axes are relative
     */
    private void applyEntityMove(int entityId, Vector3d pos, RelativeFlag flags) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;

        if (flags != null && (flags.has(RelativeFlag.X) || flags.has(RelativeFlag.Y)
                || flags.has(RelativeFlag.Z))) {
            double[] last = ReachCheck.getTrackedPosition(entityId);
            if (last != null) {
                x = flags.has(RelativeFlag.X) ? last[0] + x : x;
                y = flags.has(RelativeFlag.Y) ? last[1] + y : y;
                z = flags.has(RelativeFlag.Z) ? last[2] + z : z;
            }
        }

        ReachCheck.trackMove(entityId, x, y, z);
    }

    /**
     * Extracts the echoed transaction ID from a client transaction response.
     * Pong (1.17+) and WindowConfirmation (pre-1.17) carry it in different fields.
     *
     * @return the transaction ID, or -1 if the packet could not be decoded
     */
    private short readTransactionResponseId(PacketReceiveEvent event, PacketTypeCommon type) {
        try {
            if (type == PacketType.Play.Client.PONG) {
                return (short) new WrapperPlayClientPong(event).getId();
            }
            return new WrapperPlayClientWindowConfirmation(event).getActionId();
        } catch (Exception e) {
            return -1;
        }
    }

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
