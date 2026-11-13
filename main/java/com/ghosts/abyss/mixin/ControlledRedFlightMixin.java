
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.gojo.ControlledRedManager;
import com.ghosts.abyss.gojo.ControlledRedProjectile;
import com.ghosts.abyss.gojo.RedBlueCollision;

import net.mcreator.jujutsucraft.entity.RedEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AIRedProcedure",
        remap = false
)
public abstract class ControlledRedFlightMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$redFlight(
            LevelAccessor world,
            Entity entity,
            CallbackInfo ci
    ) {
        if (!(entity instanceof RedEntity red)) {
            return;
        }

        int mode = red.getPersistentData()
                .getInt(ControlledRedManager.MODE);

        if (mode == ControlledRedManager.FLYING) {
            // Replace JC's instant attack with visible flight.
            ControlledRedProjectile.tick(red);
            ci.cancel();
            return;
        }

        if (mode == ControlledRedManager.FIRED
                && RedBlueCollision.checkFlight(red)) {
            ci.cancel();
        }
    }
}
