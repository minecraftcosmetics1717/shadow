package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.DomainExpansionCreateBarrierProcedure"},
        remap = false
)
public class OpenDomainMixin {

   @Inject(
           method = {"execute"},
           at = {@At("RETURN")},
           remap = false
   )
   private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity != null) {
         // If entity has an open domain active, set barrier tick counter 'cnt2' to 1.0
         if (entity.getPersistentData().getBoolean("open_domain")) {
            entity.getPersistentData().putDouble("cnt2", 1.0D);
         }
      }
   }
}