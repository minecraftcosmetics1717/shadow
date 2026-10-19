package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueOnPotionActiveTickProcedure"},
        remap = false
)
public class CsmChargeReleaseMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void clearPressZForOwnedSpirits(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity == null) {
         return;
      }

      // Check if the entity is tagged as a Cursed Spirit
      if (entity.getPersistentData().getBoolean("CursedSpirit")) {
         double rawSkill = entity.getPersistentData().getDouble("skill");
         int skill = (int) Math.round(rawSkill);

         if (skill > 0) {
            if (skill % 100 != 21) {
               entity.getPersistentData().putBoolean("PRESS_Z", false);
            }
         }
      }
   }
}