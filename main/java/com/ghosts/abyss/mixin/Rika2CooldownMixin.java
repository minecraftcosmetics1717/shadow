
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.technique.TechniqueCooldownManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.DieRikaProcedure",
        remap = false
)
public class Rika2CooldownMixin {

   @ModifyConstant(
           method = "execute",
           constant = @Constant(doubleValue = 3600.0D),
           remap = false
   )
   private static double jslop$extendRika2Cooldown(
           double original
   ) {
      return 6000.0D;
   }

   @Inject(
           method = "execute",
           at = @At("HEAD"),
           remap = false
   )
   private static void abyss$beginRikaCooldown(
           LevelAccessor world,
           Entity entity,
           CallbackInfo ci
   ) {
      TechniqueCooldownManager.beginRikaDespawn(entity);
   }

   @Inject(
           method = "execute",
           at = @At("RETURN"),
           remap = false
   )
   private static void abyss$endRikaCooldown(
           LevelAccessor world,
           Entity entity,
           CallbackInfo ci
   ) {
      TechniqueCooldownManager.endRikaDespawn();
   }
}
