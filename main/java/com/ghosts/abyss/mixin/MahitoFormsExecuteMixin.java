package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stop Jujutsucraft's old 1515 transformation and the WIP fallbacks. */
@Pseudo
@Mixin(targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueMahitoProcedure", remap = false)
public final class MahitoFormsExecuteMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void abyss$interceptFormSkills(LevelAccessor world, double x, double y,
                                                  double z, Entity entity, CallbackInfo ci) {
        if (entity == null) return;
        int skill = (int) Math.round(entity.getPersistentData().getDouble("skill"));
        if (skill == 1512 || skill == 1513 || skill == 1515) {
            ci.cancel();
            if (!entity.level().isClientSide) entity.getPersistentData().putDouble("skill", 0);
        }
    }
}
