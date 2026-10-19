package com.ghosts.abyss.mixin;

import java.lang.reflect.Field;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.common.capabilities.Capability;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure"},
        remap = false
)
public class GojoTeleportKeyHookMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void jslop$setTeleportAuth(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity == null) {
         return;
      }

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);

         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = entity.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return;
         }

         Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
         selectField.setAccessible(true);

         double select = (Double) selectField.get(vars);

         // Technique ID 9.0 corresponds to Gojo's Teleport skill selection
         if (select == 9.0D) {
            entity.getPersistentData().putBoolean("jslop_teleport_auth", true);
         }
      } catch (Exception ignored) {
      }
   }
}