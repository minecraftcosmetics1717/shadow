
package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;

import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class DomainPlayerSync {

    private static final int SYNC_DELAY = 10;
    private static final double SYNC_RANGE = 128.0;

    private DomainPlayerSync() {}

    public static void schedule(
            ServerPlayer player,
            ServerLevel destination
    ) {
        if (player == null || destination == null) return;

        MinecraftServer server = destination.getServer();
        if (server == null) return;

        UUID playerId = player.getUUID();

        server.tell(new TickTask(
                server.getTickCount() + SYNC_DELAY,
                () -> sync(playerId, destination)
        ));
    }

    private static void sync(
            UUID playerId,
            ServerLevel destination
    ) {
        MinecraftServer server = destination.getServer();
        if (server == null) return;

        ServerPlayer transferred =
                server.getPlayerList().getPlayer(playerId);

        if (transferred == null || !transferred.isAlive()) {
            return;
        }

        // Ignore old synchronization tasks if the player
        // has already left this dimension.
        if (transferred.serverLevel() != destination) {
            return;
        }

        int synchronizedPlayers = 0;
        double rangeSq = SYNC_RANGE * SYNC_RANGE;

        for (ServerPlayer observer : destination.players()) {
            if (observer == transferred) continue;

            if (observer.distanceToSqr(transferred)
                    > rangeSq) {
                continue;
            }

            // Correct the transferred player on the
            // observing player's client.
            sendPositionAndRotation(observer, transferred);

            // Correct the observer on the
            // transferred player's client.
            sendPositionAndRotation(transferred, observer);

            synchronizedPlayers++;
        }

        Abyss.LOGGER.info(
                "[Abyss Player Sync] Player={} Dim={} " +
                        "Position={} NearbyPlayers={}",
                transferred.getName().getString(),
                destination.dimension().location(),
                transferred.position(),
                synchronizedPlayers
        );
    }

    private static void sendPositionAndRotation(
            ServerPlayer receiver,
            ServerPlayer target
    ) {
        if (receiver == target) return;
        if (receiver.serverLevel() != target.serverLevel()) {
            return;
        }

        // Absolute position, body yaw and pitch.
        receiver.connection.send(
                new ClientboundTeleportEntityPacket(target)
        );

        // Head yaw is synchronized separately.
        byte headYaw = (byte) (
                target.getYHeadRot() * 256.0F / 360.0F
        );

        receiver.connection.send(
                new ClientboundRotateHeadPacket(
                        target,
                        headYaw
                )
        );
    }
}
