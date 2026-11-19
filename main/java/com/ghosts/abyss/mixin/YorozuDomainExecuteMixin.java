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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueYorozuProcedure",
        remap = false
)
public class YorozuDomainExecuteMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceYorozuDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Yorozu's Threefold Affliction skill is 3920.
        if (entity.getPersistentData()
                .getDouble("skill") != 3920.0D) {
            return;
        }

        // Stop JC's original domain creation.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickYorozuDomain(
                    world,
                    living
            );
        }
    }
}
