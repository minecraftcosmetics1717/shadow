package com.ghosts.abyss.event;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "abyss")
public class HakariEvents {

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.player.hasEffect(JujutsucraftModMobEffects.JACKPOT.get())) {
         if (event.player.getEffect(JujutsucraftModMobEffects.JACKPOT.get()).getAmplifier() != 5) {
            event.player.removeEffect(JujutsucraftModMobEffects.JACKPOT.get());
            event.player.addEffect(new MobEffectInstance(JujutsucraftModMobEffects.JACKPOT.get(), 5020, 5));
            
            int baseAmp = 0;
            if (event.player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
               baseAmp = event.player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1;
            }

            baseAmp = Math.min(3, baseAmp);
            event.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5020, baseAmp));
         }

         event.player.getCapability(JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent((caps) -> {
            caps.PlayerCursePower = caps.PlayerCursePowerMAX;
         });
      }
   }
}