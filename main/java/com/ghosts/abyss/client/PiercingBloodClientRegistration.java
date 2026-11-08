
package com.ghosts.abyss.client;

import com.ghosts.abyss.init.JujutsuSlopEntities;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PiercingBloodClientRegistration {

    private PiercingBloodClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                JujutsuSlopEntities.PIERCING_BLOOD.get(),
                PiercingBloodEntityRenderer::new
        );
    }
}
