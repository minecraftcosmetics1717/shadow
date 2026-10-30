
package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class SixEyesThermalLayerRegistration {

    private SixEyesThermalLayerRegistration() {}

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onAddLayers(
            EntityRenderersEvent.AddLayers event
    ) {
        int registered = 0;

        // Register for living entity renderers.
        for (EntityType<?> entityType :
                ForgeRegistries.ENTITY_TYPES.getValues()) {

            EntityRenderer renderer =
                    event.getRenderer((EntityType) entityType);

            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(
                        new SixEyesThermalLayer(livingRenderer)
                );
                registered++;
            }
        }

        // Player renderers are registered separately by skin.
        for (String skin : event.getSkins()) {

            LivingEntityRenderer renderer =
                    event.getSkin(skin);

            if (renderer != null) {
                renderer.addLayer(
                        new SixEyesThermalLayer(renderer)
                );
                registered++;
            }
        }

        System.out.println(
                "[Abyss] Registered Six Eyes thermal layers: "
                        + registered
        );
    }
}
