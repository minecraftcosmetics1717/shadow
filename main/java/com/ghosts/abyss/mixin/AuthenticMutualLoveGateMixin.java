package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AuthenticMutualLoveProcedure",
        remap = false
)
public class AuthenticMutualLoveGateMixin {

   @Inject(
           method = "execute",
           at = @At("HEAD"),
           cancellable = true,
           remap = false
   )
   private static void jslop$gatePureLove(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         if (player.getServer() == null) {
            return;
         }

         Advancement adv = player.getServer().getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft", "skill_curseis_lifted"));

         if (adv == null || !player.getAdvancements().getOrStartProgress(adv).isDone()) {
            player.sendSystemMessage(Component.literal("Authentic Mutual Love requires Pure Love to have been used first."), true);
            Abyss.LOGGER.debug("[JSlop] AuthenticMutualLoveGateMixin: blocked AML for {} (skill_curseis_lifted not complete)", player.getScoreboardName());

            entity.getPersistentData().putDouble("skill", 0.0D);
            ci.cancel();
         }
      }
   }
}