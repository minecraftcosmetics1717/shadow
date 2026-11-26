package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Abyss moves its own witness stands; leave normal JC stands unchanged. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.AIWitnessStandProcedure", remap = false)
public class HigurumaWitnessStandMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$customStand(LevelAccessor world, Entity entity, CallbackInfo ci) {
        if (entity != null && entity.getPersistentData().getBoolean("abyss_higuruma_stand")) {
            ci.cancel();
        }
    }
}
