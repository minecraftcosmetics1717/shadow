package com.ghosts.abyss.mixin;

import com.ghosts.abyss.gojo.ControlledRedManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyPressedProcedure",
        remap = false)
public abstract class ControlledRedPressMixin {
    @Inject(method = "execute", at = @At("TAIL"), remap = false)
    private static void abyss$redPressed(LevelAccessor world, double x,
            double y, double z, Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            ControlledRedManager.onPressed(player);
        }
    }
}
