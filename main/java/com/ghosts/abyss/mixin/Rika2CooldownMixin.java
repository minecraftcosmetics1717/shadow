package com.ghosts.abyss.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(
   targets = {"net.mcreator.jujutsucraft.procedures.DieRikaProcedure"},
   remap = false
)
public class Rika2CooldownMixin {
   @ModifyConstant(
      method = {"execute"},
      constant = {@Constant(
   doubleValue = (double)3600.0F
)},
      remap = false
   )
   private static double jslop$extendRika2Cooldown(double original) {
      return (double)6000.0F;
   }
}
