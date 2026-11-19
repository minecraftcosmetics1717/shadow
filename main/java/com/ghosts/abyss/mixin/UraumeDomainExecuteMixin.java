
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueUraumeProcedure",
        remap = false
)
public class UraumeDomainExecuteMixin {

    /**
     * Replace Uraume's original Domain Expansion
     * (skill 2420) with Abyss's domain system.
     *
     * All other Uraume techniques remain unchanged.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceUraumeDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Uraume's Domain Expansion is skill 2420.
        if (entity.getPersistentData()
                .getDouble("skill") != 2420.0D) {
            return;
        }

        // Prevent JC's original barrier creation.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickUraumeDomain(
                    world,
                    living
            );
        }
    }
}
