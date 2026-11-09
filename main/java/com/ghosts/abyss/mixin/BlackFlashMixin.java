
package com.ghosts.abyss.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.RangeAttackProcedure",
        remap = false
)
public class BlackFlashMixin {

   private static final ThreadLocal<Boolean> PLAYER_ATTACK =
           ThreadLocal.withInitial(() -> false);

   @Inject(method = "execute", at = @At("HEAD"), remap = false)
   private static void abyss$begin(
           LevelAccessor world,
           double x, double y, double z,
           Entity entity,
           CallbackInfo ci
   ) {
      PLAYER_ATTACK.set(entity instanceof ServerPlayer);
   }

   @Redirect(
           method = "execute",
           at = @At(
                   value = "INVOKE",
                   target = "Ljava/lang/Math;random()D"
           ),
           remap = false
   )
   private static double abyss$disableRandomBlackFlash() {
      // The original BF roll succeeds only when Math.random()
      // exceeds its high threshold. Zero prevents that.
      return PLAYER_ATTACK.get() ? 0.0D : Math.random();
   }

   @Inject(method = "execute", at = @At("TAIL"), remap = false)
   private static void abyss$end(
           LevelAccessor world,
           double x, double y, double z,
           Entity entity,
           CallbackInfo ci
   ) {
      PLAYER_ATTACK.remove();
   }
}
