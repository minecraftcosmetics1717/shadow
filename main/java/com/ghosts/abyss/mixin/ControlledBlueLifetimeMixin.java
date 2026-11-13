
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.gojo.ControlledBlueManager;

import net.mcreator.jujutsucraft.entity.BlueEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AIBlueProcedure",
        remap = false
)
public abstract class ControlledBlueLifetimeMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            remap = false
    )
    private static void abyss$preserveControlledBlue(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof BlueEntity blue)) {
            return;
        }

        int mode = blue.getPersistentData()
                .getInt(ControlledBlueManager.MODE);

        if (mode != ControlledBlueManager.CONTROLLED
                && mode != ControlledBlueManager.ANCHORED) {
            return;
        }

        // JC increments cnt1 during AIBlueProcedure.
        // Starting at 2 makes it reach 3, allowing
        // block destruction without normal expiration.
        blue.getPersistentData().putDouble("cnt1", 2.0D);

        blue.getEntityData().set(
                BlueEntity.DATA_flag_start,
                true
        );

        // Stationary attraction, not moving Blue's
        // normal "circle" flight behavior.
        blue.getPersistentData().putBoolean(
                "circle", false
        );
    }
}
