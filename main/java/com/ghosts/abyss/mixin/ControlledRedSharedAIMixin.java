package com.ghosts.abyss.mixin;

import com.ghosts.abyss.gojo.ControlledRedManager;
import net.mcreator.jujutsucraft.entity.RedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.mcreator.jujutsucraft.procedures.AIBlueRedProcedure",
        remap = false)
public abstract class ControlledRedSharedAIMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void abyss$preserveControlledRed(LevelAccessor world,
            Entity entity, CallbackInfo ci) {
        if (entity instanceof RedEntity red
                && red.getPersistentData().getInt(ControlledRedManager.MODE)
                    == ControlledRedManager.CONTROLLED) {
            ci.cancel();
        }
    }
}
