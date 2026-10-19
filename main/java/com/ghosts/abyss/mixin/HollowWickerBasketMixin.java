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
        targets = {"net.mcreator.jujutsucraft.procedures.EffectCharactorProcedure"},
        remap = false
)
public class HollowWickerBasketMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false,
           cancellable = true
   )
   private static void onEffectCharactor(LevelAccessor world, Entity entity, Entity entityiterator, CallbackInfo ci) {
      // Cancels domain/character status effect execution if target has active Hollow Wicker Basket (hwb_active)
      if (entityiterator != null && entityiterator.getPersistentData().getBoolean("hwb_active")) {
         ci.cancel();
      }
   }
}