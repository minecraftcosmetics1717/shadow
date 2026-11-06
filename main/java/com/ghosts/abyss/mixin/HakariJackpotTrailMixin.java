
package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.JackpotOnEffectActiveTickProcedure",
        remap = false
)
public class HakariJackpotTrailMixin {

    @Redirect(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_20142_()Z",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private static boolean abyss$disableJackpotSprintTrail(
            Entity entity
    ) {
        // Prevent the sprint trail condition from passing.
        // Normal sprinting is unaffected.
        return false;
    }
}
