package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.entity.RockEntity;
import com.ghosts.abyss.init.JujutsuSlopEntities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueTodoProcedure"},
        remap = false
)
public class TodoPebbleExecuteMixin {

   private static final double THROW_SPEED = 3.6D;
   private static final int COOLDOWN_TICKS = 13;

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jreborn$todoPebbleExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (!(entity instanceof ServerPlayer player)) {
         return;
      }

      boolean authorized = player.getPersistentData().getBoolean("jreborn_todorock_auth");
      player.getPersistentData().putBoolean("jreborn_todorock_auth", false);

      if (!authorized) {
         return;
      }

      player.getPersistentData().putDouble("skill", 0.0D);

      if (!(player.level() instanceof ServerLevel serverLevel)) {
         ci.cancel();
         return;
      }

      MobEffect cooldownEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "cooldown_time"));
      if (cooldownEffect != null) {
         player.addEffect(new MobEffectInstance(cooldownEffect, COOLDOWN_TICKS, 0, false, false, true));
      }

      RockEntity rock = new RockEntity(serverLevel);
      Vec3 eyes = player.getEyePosition();
      Vec3 look = player.getViewVector(1.0F);

      rock.setPos(eyes.x, eyes.y, eyes.z);
      rock.setDeltaMovement(look.scale(THROW_SPEED));
      rock.setYRot(player.getYRot());
      rock.setXRot(player.getXRot());

      serverLevel.addFreshEntity(rock);

      Abyss.LOGGER.info("[JReborn] Rock entity thrown by {}", player.getScoreboardName());

      ci.cancel();
   }
}