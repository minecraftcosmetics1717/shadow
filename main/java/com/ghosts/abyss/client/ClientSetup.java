package com.ghosts.abyss.client;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.client.renderer.RockRenderer;
import com.ghosts.abyss.init.JujutsuSlopEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

   @SubscribeEvent
   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(JujutsuSlopEntities.ROCK.get(), RockRenderer::new);
   }
}