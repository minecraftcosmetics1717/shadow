
package com.ghosts.abyss.domain;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class DomainPlayerSync {

    private static final String RESET_KEY =
            "abyss_tracking_reset_pending";

    private static final int RESET_DELAY = 10;

    private DomainPlayerSync() {}

    public static void schedule(
            ServerPlayer player,
            ServerLevel destination
    ) {
        if (player == null || destination == null) {
            return;
        }

        MinecraftServer server = destination.getServer();
        if (server == null) return;

        UUID playerId = player.getUUID();

        server.tell(new TickTask(
                server.getTickCount() + RESET_DELAY,
                () -> {
                    ServerPlayer current =
                            server.getPlayerList().getPlayer(playerId);

                    if (current == null || !current.isAlive()) {
                        return;
                    }

                    // Ignore an outdated reset if the player
                    // has already changed dimensions again.
                    if (current.serverLevel() != destination) {
                        return;
                    }

                    // Minecraft's ServerEntity tracker will
                    // process this during sendChanges().
                    current.getPersistentData().putBoolean(
                            RESET_KEY,
                            true
                    );
                }
        ));
    }
}
