package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;

import java.lang.reflect.Field;

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
        targets = {"net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure"},
        remap = false
)
public class TodoPebbleDisplayMixin {
   private static final double SKILL_COST = 50.0D;

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jreborn$todoPebbleDisplay(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
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

         double playerSkill = (Double) ctField.get(vars);
         // Technique ID 20.0 = Aoi
         if (playerSkill != 20.0D) {
            return;
         }

         Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
         selectField.setAccessible(true);
         double selectNumber = (Double) selectField.get(vars);

         boolean isShiftKeyDown = entity.isShiftKeyDown();
         boolean noChange = false;

         try {
            Field ncField = vars.getClass().getDeclaredField("noChangeTechnique");
            ncField.setAccessible(true);
            noChange = (Boolean) ncField.get(vars);
         } catch (NoSuchFieldException ignored) {
         }

         boolean commitPebble;
         if (!isShiftKeyDown && selectNumber == 7.0D && !noChange) {
            commitPebble = true;
         } else if (isShiftKeyDown && selectNumber == 15.0D && !noChange) {
            commitPebble = true;
         } else if (selectNumber == 8.0D && noChange) {
            commitPebble = true;
         } else {
            commitPebble = false;
         }

         if (!commitPebble) {
            return;
         }

         Class<?> decideClass = Class.forName("net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure");
         decideClass.getMethod("execute", Entity.class, Boolean.TYPE, Boolean.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, String.class)
                 .invoke(null, entity, false, false, SKILL_COST, 20.0D, 8.0D, "Pebble");

         ci.cancel();
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JReborn] TodoPebbleDisplayMixin failed: {}", e.getMessage());
      }
   }
}