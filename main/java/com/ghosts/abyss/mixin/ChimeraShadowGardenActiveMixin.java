
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.DomainDimension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.ChimeraShadowGardenActiveProcedure",
        remap = false
)
public class ChimeraShadowGardenActiveMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceChimeraActive(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        // Only suppress the original procedure
        // inside Abyss's custom domain dimension.
        if (world instanceof ServerLevel level
                && level.dimension().equals(DomainDimension.ID)) {

            ci.cancel();
        }
    }
}
