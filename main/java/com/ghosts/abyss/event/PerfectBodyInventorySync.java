
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncPerfectBodyInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = Abyss.MOD_ID)
public final class PerfectBodyInventorySync {

    private static final Map<UUID, Boolean> LAST_STATES =
            new HashMap<>();

    private PerfectBodyInventorySync() {}

    public static void sync(ServerPlayer player) {
        boolean perfectBody = player.getPersistentData()
                .getBoolean("perfect_body");

        LAST_STATES.put(player.getUUID(), perfectBody);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncPerfectBodyInventory(
                        player.getUUID(),
                        perfectBody
                )
        );
    }

    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        LAST_STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        boolean current = player.getPersistentData()
                .getBoolean("perfect_body");

        Boolean previous = LAST_STATES.get(player.getUUID());

        if (previous == null || previous != current) {
            sync(player);
        }
    }
}
