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
public class RikaPartialKeyHookMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void jslop$setRikaPartialAuth(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
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

         Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
         secondField.setAccessible(true);
         boolean secondActive = (Boolean) secondField.get(vars);

         Field ctField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
         ctField.setAccessible(true);
         double curseTechnique = (Double) ctField.get(vars);

         // Technique ID 5.0 = Yuta Okkotsu / Rika
         if (curseTechnique != 5.0D) {
            return;
         }

         Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
         selectField.setAccessible(true);
         double selectedTechnique = (Double) selectField.get(vars);

         // Skill selection index 11.0 = Rika: Partial
         if (selectedTechnique == 11.0D) {
            entity.getPersistentData().putBoolean("jslop_rika_partial_auth", true);
         }
      } catch (Exception ignored) {
      }
   }
}