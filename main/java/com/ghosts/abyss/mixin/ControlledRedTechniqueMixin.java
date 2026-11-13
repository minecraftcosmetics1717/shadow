package com.ghosts.abyss.mixin;

import com.ghosts.abyss.gojo.ControlledRedManager;
import com.ghosts.abyss.progression.GojoProgression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.jujutsucraft.procedures.TechniqueRedProcedure",
        remap = false)
public abstract class ControlledRedTechniqueMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void abyss$overrideSixEyesRed(LevelAccessor world,
            double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player
                && GojoProgression.hasSixEyes(player)
                && !ControlledRedManager.allowOriginal(player)) {
            ci.cancel();
        }
    }
}
