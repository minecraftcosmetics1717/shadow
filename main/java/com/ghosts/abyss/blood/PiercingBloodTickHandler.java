
package com.ghosts.abyss.blood;

import com.ghosts.abyss.Abyss;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PiercingBloodTickHandler {

    private PiercingBloodTickHandler() {}

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

        AbyssPiercingBlood.tick(player);
    }
}
