
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
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueSukunaProcedure",
        remap = false
)
public class SukunaDomainExecuteMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceSukunaDomain(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) return;

        // Malevolent Shrine is skill 120.
        if (entity.getPersistentData()
                .getDouble("skill") != 120.0D) {
            return;
        }

        // Prevent JC from running its original
        // Malevolent Shrine expansion.
        ci.cancel();

        if (entity instanceof LivingEntity living) {
            AbyssDomainCast.tickSukunaDomain(
                    world,
                    living
            );
        }
    }
}
