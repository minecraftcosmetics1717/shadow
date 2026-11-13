
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
        targets = "net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure",
        remap = false
)
public abstract class ControlledBlueBlockMixin {

    private static final double MIN_BLOCK_DAMAGE = 4.0D;

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            remap = false
    )
    private static void abyss$increaseBlueBlockDamage(
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

        double original = blue.getPersistentData()
                .getDouble("BlockDamage");

        blue.getPersistentData().putDouble(
                "BlockDamage",
                Math.max(original, MIN_BLOCK_DAMAGE)
        );
    }
}
