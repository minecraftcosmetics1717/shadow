
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.progression.GojoProgression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.TechniqueBlueProcedure",
        remap = false
)
public abstract class ControlledBlueTechniqueMixin {

    @Inject(
            method = "execute",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$overrideSixEyesBlue(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo ci
    ) {
        if (entity instanceof ServerPlayer player
                && GojoProgression.hasSixEyes(player)) {
            // Abyss handles this Blue through its tick manager.
            ci.cancel();
        }
    }
}
