
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.blood.AbyssPiercingBlood;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.CursedTechniqueChosoProcedure",
        remap = false
)
public class BloodTechniqueOverrideMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$replaceBloodTechniques(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity == null) {
            return;
        }

        int skill = (int) entity
                .getPersistentData()
                .getDouble("skill");

        if (skill == 1007) {

            // Stop Jujutsucraft's original dispatcher.
            ci.cancel();

            // Execute our Abyss-controlled implementation.
            AbyssPiercingBlood.execute(
                    world,
                    x,
                    y,
                    z,
                    entity
            );
        }
    }
}
