package com.ghosts.abyss.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.DomainExpansionOnEffectActiveTickProcedure",
        remap = false
)
public class JcDomainCeDrainMixin {

    // PlayerCursePowerChange -= 20.0  →  -= 5.0
    @ModifyConstant(
            method = "execute",
            constant = @Constant(doubleValue = 20.0),
            remap = false,
            require = 0
    )
    private static double abyss$reduceDomainCeDrain(double original) {
        // Only shrink the common "20" CE tick; if this hits a wrong constant, remove require=0 and refine
        if (Math.abs(original - 20.0) < 0.001) {
            return 5.0;
        }
        return original;
    }
}