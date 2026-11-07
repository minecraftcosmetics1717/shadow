
package com.ghosts.abyss.event;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.SoulAwareness;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SoulAwarenessEvents {

    private static final String KEY =
            "abyss_soul_awareness";

    private static final String INITIALIZED =
            "abyss_soul_awareness_initialized";

    private SoulAwarenessEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Update starting bonuses once per second.
        if (player.tickCount % 20 == 0) {
            SoulAwareness.initialize(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(
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
    }
}
