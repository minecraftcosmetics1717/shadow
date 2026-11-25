package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Add Mahito's Soul Grasp to JC's unused slot 11; preserve all other skills. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyChangeTechniqueOnKeyPressed2Procedure",
        remap = false)
public final class MahitoSoulGraspDisplayMixin {
    private static final double CE_COST = 50.0D; // Matches current Blind Rage

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$soulGraspDisplay(
            LevelAccessor world, double x, double y, double z,
            Entity entity, double playerCT, double playerSelect, CallbackInfo ci) {
        if (entity == null || playerCT != 15.0D) return;
        boolean selectingSoulGrasp = playerSelect == 11.0D
                || (playerSelect == 14.0D && entity.isShiftKeyDown());
        if (!selectingSoulGrasp) return;
        TechniqueDecideProcedure.execute(entity, false, true,
                CE_COST, 15.0D, 11.0D, "Soul Grasp");
        ci.cancel();
    }
}
