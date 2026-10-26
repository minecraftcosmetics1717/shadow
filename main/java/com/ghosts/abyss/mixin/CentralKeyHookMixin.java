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
public class CentralKeyHookMixin {

   @Inject(method = {"execute"}, at = {@At("HEAD")}, remap = false)
   private static void jreborn$onStartTechniqueKey(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity == null) return;

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability<Object>) capField.get(null);

         Object vars = entity.getCapability(cap, (Direction) null).orElse(null);
         if (vars == null) return;

         // Get Selected Technique Slot (0.0 to 21.0)
         Field selectField = vars.getClass().getDeclaredField("PlayerSelectCurseTechnique");
         selectField.setAccessible(true);
         double selected = (Double) selectField.get(vars);

         // Check if secondary technique is active
         Field secondField = vars.getClass().getDeclaredField("SecondTechnique");
         secondField.setAccessible(true);
         boolean secondActive = (Boolean) secondField.get(vars);

         // Get Active Character/Curse Technique ID (Primary or Secondary)
         Field techField = vars.getClass().getDeclaredField(secondActive ? "PlayerCurseTechnique2" : "PlayerCurseTechnique");
         techField.setAccessible(true);
         double techniqueId = (Double) techField.get(vars);

         int techIdInt = (int) techniqueId;

         switch (techIdInt) {
            case 20: // AoiTodo
               if (selected == 8.0) { // Rock
                  entity.getPersistentData().putBoolean("jreborn_todorock_auth", true);
               }
               break;

            case 6: // Megumi / Ten Shadows
               if (selected == 19.0) { // Shadow Dive
                  entity.getPersistentData().putBoolean("jreborn_shadowdive_auth", true);
               }
               break;

            case 8: // Dagon
               if (selected == 20.0) { // Domain / Horizon
                  entity.getPersistentData().putBoolean("abyss_dagon_domain_auth", true);
               }
               break;

            case 38: // Takako Uro
               if (selected == 9.0) { // Infinite Sky
                  entity.getPersistentData().putBoolean("jreborn_infsky_auth", true);
               }
               break;

            case 41: // Kaori Itadori
               if (selected == 11.0) { // Antigravity System: Self-Lapse
                  entity.getPersistentData().putBoolean("jreborn_selflapse_auth", true);
               }
               break;

            default:
               break;
         }
// After you resolve `selected` (PlayerSelectCurseTechnique):
         if (selected == 21.0) {
            entity.getPersistentData().putBoolean("abyss_cancel_domain_auth", true);
         }
      } catch (Exception ignored) {}
   }
}