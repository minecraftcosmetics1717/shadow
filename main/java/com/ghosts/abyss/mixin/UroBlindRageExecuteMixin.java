package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Slot 3809 is handled by BlindRageHandler through held mouse input.
 * Do not execute Infinite Sky's old Infinity effect or JC's default fallback.
 */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueTakakoUroProcedure",
        remap = false)
public class UroBlindRageExecuteMixin {
   @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
   private static void abyss$blindRageExecute(
           LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity == null || (int) Math.round(
              entity.getPersistentData().getDouble("skill")) != 3809) return;
      ci.cancel();
      if (!entity.level().isClientSide && entity instanceof LivingEntity living) {
         living.removeEffect(JujutsucraftModMobEffects.CURSED_TECHNIQUE.get());
      }
   }
}
