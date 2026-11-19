
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueIshigoriProcedure",
        remap = false
)
public class RyuDomainExecuteMixin {

    private static final double DOMAIN_SKILL = 1220.0D;

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceRyuDomain(
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

        // Replace Jujutsucraft's original domain
        // with Abyss's domain expansion system.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickRyuDomain(
                    world,
                    living
            );
        }
    }
}
