package com.ghosts.abyss.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CanSeeSukunaSlashProcedure"},
        remap = false
)
public class ShrineSlashSuppressMixin {

   @Inject(
           method = {"execute"},
           at = {@At("RETURN")},
           cancellable = true,
           remap = false
   )
   private static void suppressNativeSlashes(LevelAccessor world, Entity slashSource, Entity viewer, CallbackInfoReturnable<Boolean> cir) {
      if (slashSource instanceof LivingEntity livingEntity) {
         MobEffect domainExpansionEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:domain_expansion"));

         // Suppress rendering/visibility of native Malevolent Shrine slashes if the caster is inside a Domain Expansion
         if (domainExpansionEffect != null && livingEntity.hasEffect(domainExpansionEffect)) {
            cir.setReturnValue(false);
         }
      }
   }
}