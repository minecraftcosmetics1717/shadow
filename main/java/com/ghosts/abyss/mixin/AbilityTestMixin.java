package com.ghosts.abyss.mixin;

import com.ghosts.abyss.AbilityScanHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"net.mcreator.jujutsucraft.procedures.ChangeTechniqueTest2Procedure"},
   remap = false
)
public class AbilityTestMixin {
   @Inject(
           method = "execute",
           at = @At("RETURN"),
           cancellable = true,
           remap = false
   )
   private static void forcePassOnPermissiveScan(LevelAccessor world, double x, double y, double z, Entity entity, double playerCT, double playerSelect, CallbackInfoReturnable<Boolean> cir) {
      if (AbilityScanHandler.isPermissive()) {
         cir.setReturnValue(false);
      }

   }
}
