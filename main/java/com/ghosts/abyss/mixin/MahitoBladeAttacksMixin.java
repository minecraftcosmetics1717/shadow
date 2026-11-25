package com.ghosts.abyss.mixin;

import com.ghosts.abyss.mahito.MahitoForms;
import net.mcreator.jujutsucraft.procedures.LogicSwordProcedure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Idle: Blades uses JC's existing sword attack procedures, without giving a sword item. */
@Mixin(value = LogicSwordProcedure.class, remap = false)
public final class MahitoBladeAttacksMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$blades(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MahitoForms.blades(entity)) cir.setReturnValue(true);
    }
}
