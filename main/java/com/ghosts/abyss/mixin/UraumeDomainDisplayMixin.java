
package com.ghosts.abyss.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.TechniqueDecideProcedure",
        remap = false
)
public class UraumeDomainDisplayMixin {

    @ModifyVariable(
            method = "execute",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private static String abyss$renameUraumeDomain(
            String originalName,
            net.minecraft.world.entity.Entity entity,
            boolean passive,
            boolean physical,
            double cost,
            double playerCT,
            double playerSelect
    ) {
        // Only rename Uraume's Domain Expansion.
        if (playerCT == 24.0D
                && playerSelect == 20.0D) {

            return "Distant Winds of Commitment";
        }

        // Leave every other ability unchanged.
        return originalName;
    }
}
