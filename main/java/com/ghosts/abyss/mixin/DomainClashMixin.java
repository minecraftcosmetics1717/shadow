package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.DomainExpansionOnEffectActiveTickProcedure"},
        remap = false
)
public class DomainClashMixin {

   // ThreadLocal used to pass the current entity iteration state safely across injection points
   private static final ThreadLocal<Entity> CURRENT_ITERATOR = ThreadLocal.withInitial(() -> null);

   /**
    * Captures the entity iterator variable during execution loop.
    */
   @ModifyVariable(
           method = {"execute"},
           at = @At("STORE"),
           name = {"entityiterator"},
           remap = false
   )
   private static Entity captureEntityIterator(Entity entityiterator) {
      CURRENT_ITERATOR.set(entityiterator);
      return entityiterator;
   }

   /**
    * Modifies domain power calculation for the casting entity by injecting custom refinement stats.
    */
   @ModifyVariable(
           method = {"execute"},
           at = @At(
                   value = "STORE",
                   ordinal = 1
           ),
           name = {"domainPower1"},
           remap = false
   )
   private static double injectDomainRefinementPower1(double original, LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity instanceof Player player) {
         double refinement = player.getPersistentData().getDouble("domain_refinement");
         return original + (refinement * 0.5D);
      }
      return original;
   }

   /**
    * Modifies domain power calculation for the target/opponent entity in the clash.
    */
   @ModifyVariable(
           method = {"execute"},
           at = @At(
                   value = "STORE",
                   ordinal = 1
           ),
           name = {"domainPower2"},
           remap = false
   )
   private static double injectDomainRefinementPower2(double original) {
      Entity iterator = CURRENT_ITERATOR.get();
      if (iterator instanceof Player player) {
         double refinement = player.getPersistentData().getDouble("domain_refinement");
         return original + (refinement * 0.5D);
      }
      return original;
   }
}