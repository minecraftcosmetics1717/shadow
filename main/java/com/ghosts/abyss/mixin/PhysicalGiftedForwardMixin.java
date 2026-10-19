package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.PhysicalGiftedProcedure"},
        remap = false
)
public abstract class PhysicalGiftedForwardMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void injectForwardVelocity(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity == null) {
         return;
      }

      // Checks for forward injection flag set by Heavenly Restriction / Physical Gifted dash inputs
      if (entity.getPersistentData().getBoolean("jslop_fwd_inject")) {
         entity.getPersistentData().putBoolean("jslop_fwd_inject", false);

         Vec3 currentDelta = entity.getDeltaMovement();

         // If the entity is standing still or moving below speed threshold, inject forward momentum based on current yaw
         if (Math.abs(currentDelta.x) + Math.abs(currentDelta.z) < 0.1D) {
            double yawRad = Math.toRadians(entity.getYRot());
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);

            entity.setDeltaMovement(forwardX * 0.2D, currentDelta.y, forwardZ * 0.2D);
         }
      }
   }
}