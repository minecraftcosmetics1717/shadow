
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.domain.OpenDomain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(
        targets =
                "net.mcreator.jujutsucraft.procedures.DomainExpansionCreateBarrierProcedure",
        remap = false
)
public class OpenDomainMixin {

   @ModifyArgs(
           method = "execute",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/nbt/CompoundTag;m_128347_(Ljava/lang/String;D)V",
                   remap = false
           ),
           remap = false
   )

   private static void abyss$selectBarrierType(
           Args args,
           LevelAccessor world,
           double x,
           double y,
           double z,
           Entity entity
   ) {
      if (!(entity instanceof ServerPlayer player)) {
         return;
      }

      String key = args.get(0);

      // Only override JC's barrier-mode decision.
      if (!"cnt2".equals(key)) {
         return;
      }

      var data = player.getPersistentData();

      // First implementation: Sukuna (domain ID 1).
      boolean sukuna =
              data.getDouble("select") == 1.0D
                      || data.getDouble("skill_domain") == 1.0D;

      if (!sukuna) {
         return;
      }

      boolean open =
              AbyssGamerules.isEnabled(
                      player,
                      AbyssGamerules.OPEN_DOMAIN
              )
                      && data.getBoolean(OpenDomain.KEY_UNLOCKED)
                      && data.getBoolean(OpenDomain.KEY_TOGGLED);

      // JC: 1 = open, 0 = closed.
      // Sneaking is deliberately ignored.
      args.set(1, open ? 1.0D : 0.0D);
   }
}
