
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.SoulAwareness;
import com.ghosts.abyss.SoulAwarenessPoints;
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

    private static final String KEY_AVAILABLE =
            "abyss_soul_points_available";

    private static final String LAST_SYNCED =
            "abyss_soul_awareness_last_synced";

    private static final String LAST_POINTS_SYNCED =
            "abyss_soul_points_last_synced";

    private SoulAwarenessEvents() {
    }

    // Synchronize Soul Awareness and available points.
    public static void sync(ServerPlayer player) {

        int awareness = SoulAwareness.get(player);
        int availablePoints =
                SoulAwarenessPoints.getAvailable(player);

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSyncSoulAwareness(
                        awareness,
                        availablePoints
                )
        );

        CompoundTag data = player.getPersistentData();

        data.putInt(LAST_SYNCED, awareness);
        data.putInt(LAST_POINTS_SYNCED, availablePoints);
    }

    // Initialize and synchronize when a player joins.
    @SubscribeEvent
    public static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SoulAwareness.initialize(player);
        sync(player);
    }

    // Check for changes every second.
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

        int currentAwareness = SoulAwareness.get(player);
        int currentPoints =
                SoulAwarenessPoints.getAvailable(player);

        CompoundTag data = player.getPersistentData();

        boolean awarenessChanged =
                !data.contains(LAST_SYNCED)
                        || data.getInt(LAST_SYNCED)
                        != currentAwareness;

        boolean pointsChanged =
                !data.contains(LAST_POINTS_SYNCED)
                        || data.getInt(LAST_POINTS_SYNCED)
                        != currentPoints;

        if (awarenessChanged || pointsChanged) {
            sync(player);
        }
    }

    // Preserve Soul Awareness and upgrade points
    // when the player entity is recreated.
    @SubscribeEvent
    public static void onClone(
            PlayerEvent.Clone event
    ) {
        CompoundTag oldData =
                event.getOriginal().getPersistentData();

        CompoundTag newData =
                event.getEntity().getPersistentData();

        if (oldData.contains(KEY)) {
            newData.putInt(
                    KEY,
                    oldData.getInt(KEY)
            );

            newData.putBoolean(
                    INITIALIZED,
                    oldData.getBoolean(INITIALIZED)
            );
        }

        if (oldData.contains(KEY_AVAILABLE)) {
            newData.putInt(
                    KEY_AVAILABLE,
                    oldData.getInt(KEY_AVAILABLE)
            );
        }
    }

    // Synchronize after respawning.
    @SubscribeEvent
    public static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SoulAwareness.initialize(player);
        sync(player);
    }
}
