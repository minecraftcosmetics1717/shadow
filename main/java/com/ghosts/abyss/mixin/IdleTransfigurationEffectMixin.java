
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.procedures.EffectProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EffectProcedure.class, remap = false)
public class IdleTransfigurationEffectMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void abyss$replaceIdleTransfigurationEffect(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity attacker,
            Entity target,
            CallbackInfo ci
    ) {
        if (attacker == null || target == null) {
            return;
        }

        if (attacker.getPersistentData().getDouble("skill")
                != 1505.0D) {
            return;
        }

        // Prevent Jujutsucraft's original post-hit effect,
        // including Effect12's non-player entity removal.
        ci.cancel();
    }
}
