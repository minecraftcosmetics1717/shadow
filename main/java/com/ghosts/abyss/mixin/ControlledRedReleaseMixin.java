package com.ghosts.abyss.mixin;

import com.ghosts.abyss.gojo.ControlledRedManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyReleasedProcedure",
        remap = false)
public abstract class ControlledRedReleaseMixin {
    @Inject(method = "execute", at = @At("HEAD"), remap = false)
    private static void abyss$redReleased(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            ControlledRedManager.onReleased(player);
        }
    }
}
