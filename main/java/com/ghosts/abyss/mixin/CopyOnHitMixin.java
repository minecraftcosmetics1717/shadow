package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
   targets = {"net.mcreator.jujutsucraft.procedures.EffectCharactorProcedure"},
   remap = false
)
public class CopyOnHitMixin {
   @Redirect(
      method = {"execute"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/mcreator/jujutsucraft/procedures/LocateRikaProcedure;execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)Z"
),
      remap = false
   )
   private static boolean jslop$maybeSuppressCopy(LevelAccessor world, Entity entity) {
      if (entity.getPersistentData().getBoolean("jslop_disable_copy")) {
         return false;
      } else {
         try {
            return (Boolean)Class.forName("net.mcreator.jujutsucraft.procedures.LocateRikaProcedure").getMethod("execute", LevelAccessor.class, Entity.class).invoke((Object)null, world, entity);
         } catch (Exception e) {
            Abyss.LOGGER.warn("[JSlop] CopyOnHitMixin: LocateRikaProcedure failed: {}", e.getMessage());
            return false;
         }
      }
   }
}
