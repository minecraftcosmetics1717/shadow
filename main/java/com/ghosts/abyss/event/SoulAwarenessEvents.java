
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.SoulAwareness;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncSoulAwareness;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SoulAwarenessEvents {

    private static final String KEY =
            "abyss_soul_awareness";

    private static final String INITIALIZED =
            "abyss_soul_awareness_initialized";

    private static final String LAST_SYNCED =
            "abyss_soul_awareness_last_synced";

    private SoulAwarenessEvents() {}

    public static void sync(ServerPlayer player) {
        int awareness = SoulAwareness.get(player);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncSoulAwareness(awareness)
        );

        player.getPersistentData().putInt(
                LAST_SYNCED, awareness
        );
    }

    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulAwareness.initialize(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        SoulAwareness.initialize(player);

        int current = SoulAwareness.get(player);
        CompoundTag data = player.getPersistentData();

        if (!data.contains(LAST_SYNCED)
                || data.getInt(LAST_SYNCED) != current) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(
            PlayerEvent.Clone event
    ) {
        CompoundTag oldData =
                event.getOriginal().getPersistentData();

        CompoundTag newData =
                event.getEntity().getPersistentData();

        if (oldData.contains(KEY)) {
            newData.putInt(KEY, oldData.getInt(KEY));
            newData.putBoolean(
                    INITIALIZED,
                    oldData.getBoolean(INITIALIZED)
            );
        }
    }

    @SubscribeEvent
    public static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulAwareness.initialize(player);
            sync(player);
        }
    }
}
