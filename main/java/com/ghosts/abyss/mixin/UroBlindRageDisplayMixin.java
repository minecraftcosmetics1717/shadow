package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retain Infinite Sky's slot, but replace the name with Blind Rage. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressedProcedure",
        remap = false)
public class UroBlindRageDisplayMixin {
   private static final double SKILL_COST = 1000.0D;

   @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
   private static void abyss$blindRageDisplay(
           LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity == null) return;
      var vars = entity.getCapability(
              JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
      if (vars == null) return;

      double ct = vars.SecondTechnique
              ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
      if ((int) Math.round(ct) != 38) return;

      double selected = vars.PlayerSelectCurseTechnique;
      boolean shift = entity.isShiftKeyDown();
      boolean commit = (!shift && selected == 2.0D && !vars.noChangeTechnique)
              || (shift && selected == 10.0D && !vars.noChangeTechnique)
              || (selected == 9.0D && vars.noChangeTechnique);
      if (!commit) return;

      // Physical attack = true so JC and Abyss display the combat icon/timer.
      TechniqueDecideProcedure.execute(entity, false, true,
              SKILL_COST, 38.0D, 9.0D, "Blind Rage");
      ci.cancel();
   }
}
