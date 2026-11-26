package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.TakadaOnEntityTickUpdateProcedure", remap = false)
public class JudgemanDecorationMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$keepJudgeman(
            LevelAccessor world, double x, double y, double z,
            Entity entity, CallbackInfo ci
    ) {
        if (entity != null && entity.getPersistentData().getBoolean("abyss_domain_decoration")) {
            ci.cancel();
        }
    }
}
