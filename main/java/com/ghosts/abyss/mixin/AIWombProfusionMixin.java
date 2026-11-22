
package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AIWombProfusionProcedure",
        remap = false
)
public class AIWombProfusionMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$preserveWombProfusion(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Only protect Womb Profusion entities
        // spawned by Abyss's domain decoration system.
        if (!entity.getPersistentData()
                .getBoolean("abyss_domain_decoration")) {
            return;
        }

        // Only apply this inside Abyss's
        // custom domain dimension.
        if (!entity.level().dimension().equals(
                com.ghosts.abyss.domain.DomainDimension.ID
        )) {
            return;
        }

        // Stop JC's original ownership checks
        // from deleting the decoration.
        ci.cancel();
    }
}
