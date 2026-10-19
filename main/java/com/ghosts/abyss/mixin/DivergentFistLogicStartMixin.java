package com.ghosts.abyss.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.LogicStartProcedure", remap = false)
public class DivergentFistLogicStartMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jreborn$duringDfWindow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;

        CompoundTag nbt = entity.getPersistentData();
        if (nbt.getBoolean("jreborn_df_window_open") && nbt.getBoolean("jreborn_df_active")) {
            cir.setReturnValue(true);
        }
    }
}