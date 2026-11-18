
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueFushiguroProcedure",
        remap = false
)
public class MegumiDomainExecuteMixin {

    private static final double DOMAIN_SKILL = 620.0D;

    /**
     * Replace Jujutsucraft's Chimera Shadow Garden
     * with Abyss's domain system.
     *
     * All other Ten Shadows abilities remain unchanged.
     */
    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceMegumiDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        if (entity.getPersistentData()
                .getDouble("skill") != DOMAIN_SKILL) {
            return;
        }

        // Cancel Jujutsucraft's original
        // Chimera Shadow Garden creation.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickMegumiDomain(
                    world,
                    living
            );
        }
    }
}
