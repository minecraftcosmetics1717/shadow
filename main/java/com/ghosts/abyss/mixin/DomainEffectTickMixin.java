package com.ghosts.abyss.mixin;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.DomainExpansionOnEffectActiveTickProcedure"},
        remap = false
)
public class DomainEffectTickMixin {

   // ThreadLocal stack to preserve original DomainExpansionRadius values across re-entrant method executions
   private static final ThreadLocal<Deque<Double>> RADIUS_STACK = ThreadLocal.withInitial(ArrayDeque::new);

   /**
    * Injected at the start of procedure execution.
    * Dynamically triples the domain radius if the caster has an open domain active.
    */
   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void onHead(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {

      if (entity != null
              && entity.getPersistentData().getBoolean("open_domain")
              && !(entity.level().dimension().equals(
              com.ghosts.abyss.domain.DomainDimension.ID)
              && entity.getPersistentData().getBoolean("abyss_domain_handled"))) {

         try {
            Class<?> cls = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables$MapVariables");
            Object mapVars = cls.getMethod("get", LevelAccessor.class).invoke(null, world);
            Field domainRadiusField = cls.getField("DomainExpansionRadius");

            double originalRadius = domainRadiusField.getDouble(mapVars);
            RADIUS_STACK.get().push(originalRadius);

            // Dynamically scale Domain Radius by 3x (e.g., for Sukuna/Kenjaku open barrier domains)
            domainRadiusField.setDouble(mapVars, originalRadius * 3.0D);
         } catch (Exception e) {
            RADIUS_STACK.get().push(-1.0D);
         }
      } else {
         RADIUS_STACK.get().push(-1.0D);
      }
   }

   /**
    * Injected at the end of procedure execution.
    * Restores the original DomainExpansionRadius to prevent permanent variable modification.
    */
   @Inject(
           method = {"execute"},
           at = {@At("RETURN")},
           remap = false
   )
   private static void onReturn(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      Deque<Double> stack = RADIUS_STACK.get();

      if (!stack.isEmpty()) {
         double savedRadius = stack.pop();

         // Revert DomainExpansionRadius if a valid positive radius was pushed
         if (savedRadius >= 0.0D) {
            try {
               Class<?> cls = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables$MapVariables");
               Object mapVars = cls.getMethod("get", LevelAccessor.class).invoke(null, world);
               Field domainRadiusField = cls.getField("DomainExpansionRadius");

               domainRadiusField.setDouble(mapVars, savedRadius);
            } catch (Exception ignored) {
            }
         }
      }
   }
}