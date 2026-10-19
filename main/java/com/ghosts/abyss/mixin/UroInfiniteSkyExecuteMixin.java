package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueTakakoUroProcedure"},
   remap = false
)
public class UroInfiniteSkyExecuteMixin {

   @Inject(
      method = {"execute"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private static void jreborn$uroInfiniteSkyExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (!(entity instanceof ServerPlayer player)) {
         return;
      }

      boolean authorized = player.getPersistentData().getBoolean("jreborn_infsky_auth");
      player.getPersistentData().putBoolean("jreborn_infsky_auth", false);

      if (!authorized) {
         return;
      }

      player.getPersistentData().putDouble("skill", 0.0);
      MobEffect cursedTechEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cursed_technique"));
      if (cursedTechEffect != null) {
         player.removeEffect(cursedTechEffect);
      }

      MobEffect infinityEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "infinity_effect"));
      if (infinityEffect != null) {
         // 60 seconds = 1200 Ticks
         player.addEffect(new MobEffectInstance(infinityEffect, 1200, 0, false, true));
         Abyss.LOGGER.info("[JReborn] Applied Infinite Sky (Infinity Effect) to {}", player.getScoreboardName());
      } else {
         Abyss.LOGGER.error("[JReborn] CRITICAL: Couldn't find 'jujutsucraft:infinity_effect' registered!");
      }

      ci.cancel();
   }
}