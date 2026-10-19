package com.ghosts.abyss.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.MOD,
        value = {Dist.CLIENT}
)
public class FourArmRenderer {
   @SubscribeEvent
   public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
      for (String skin : event.getSkins()) {
         LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);
         if (renderer instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new FourArmLayer(playerRenderer));
         }
      }
   }
}