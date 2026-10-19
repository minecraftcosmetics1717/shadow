package com.ghosts.abyss.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.KeyStartTechniqueOnKeyReleasedProcedure",
        remap = false
)
public class YutaTechniqueKeyReleaseMixin {

   @Inject(
           method = "execute",
           at = @At("HEAD"),
           cancellable = true,
           remap = false
   )
   private static void jslop$blockReleaseWhileKeybindActive(Entity entity, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         if (player.getPersistentData().getBoolean("jslop_yuta_keybind_active")) {
            ci.cancel();
         }
      }
   }
}