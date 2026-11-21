
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueTakakoUroProcedure",
        remap = false
)
public class UroDomainExecuteMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceUroDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Uro's original domain skill.
        if (entity.getPersistentData()
                .getDouble("skill") != 3820.0D) {
            return;
        }

        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickUroDomain(
                    world,
                    living
            );
        }
    }
}
