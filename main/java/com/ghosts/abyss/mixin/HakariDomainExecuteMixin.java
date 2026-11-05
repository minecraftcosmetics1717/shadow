
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.domain.AbyssDomainCast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueHakariProcedure",
        remap = false
)
public class HakariDomainExecuteMixin {

    /**
     * Replace Hakari's original domain skill (2920)
     * with Abyss's custom domain system.
     *
     * Other Hakari techniques remain unchanged.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceHakariDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        if (entity.getPersistentData().getDouble("skill") != 2920.0D) {
            return;
        }

        // Prevent Jujutsucraft's original domain
        // procedure from executing.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickHakariDomain(world, living);
        }
    }
}
