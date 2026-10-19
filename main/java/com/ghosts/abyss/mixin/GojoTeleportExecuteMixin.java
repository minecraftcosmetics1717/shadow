package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueGojoProcedure"},
        remap = false
)
public class GojoTeleportExecuteMixin {
   private static final double TELEPORT_RANGE = 30.0D;
   private static final int COOLDOWN_TICKS = 30;

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jslop$gojoTeleport(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      int skill = (int) entity.getPersistentData().getDouble("skill");

      if (skill == 209) {
         if (entity instanceof ServerPlayer player) {
            boolean authorized = player.getPersistentData().getBoolean("jslop_teleport_auth");
            player.getPersistentData().putBoolean("jslop_teleport_auth", false);

            Abyss.LOGGER.info("[JSlop][Teleport] skill=209 mixin fired: authorized={}, player={}", authorized, player.getName().getString());

            if (!authorized) {
               player.getPersistentData().putDouble("skill", 0.0D);
               MobEffect cursedTechEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:cursed_technique"));

               if (cursedTechEffect != null) {
                  player.removeEffect(cursedTechEffect);
               }

               ci.cancel();
            } else {
               Vec3 eyes = player.getEyePosition();
               Vec3 look = player.getViewVector(1.0F);
               Vec3 end = eyes.add(look.scale(30.0D));

               BlockHitResult hit = player.level().clip(new ClipContext(eyes, end, Block.COLLIDER, Fluid.NONE, player));
               Vec3 dest;

               if (hit.getType() == Type.BLOCK) {
                  BlockPos bp = hit.getBlockPos();
                  Direction dir = hit.getDirection();

                  if (dir == Direction.UP) {
                     dest = new Vec3(hit.getLocation().x, bp.getY() + 1.0D, hit.getLocation().z);
                  } else {
                     dest = new Vec3(
                             bp.getX() + 0.5D + (dir.getStepX() * 0.7D),
                             player.getY(),
                             bp.getZ() + 0.5D + (dir.getStepZ() * 0.7D)
                     );
                  }
               } else {
                  dest = end;
               }

               player.teleportTo(dest.x, dest.y, dest.z);
               Level currentLevel = player.level();

               if (currentLevel instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0D, dest.z, 32, 0.3D, 0.5D, 0.3D, 0.0D);
                  serverLevel.playSound((Player) null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
               }

               if (!player.isCreative()) {
                  MobEffect cooldownFx = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:cooldown_time"));

                  if (cooldownFx == null) {
                     Abyss.LOGGER.warn("[JSlop][Teleport] cooldown_time effect NOT found in registry");
                  } else {
                     MobEffectInstance before = player.getEffect(cooldownFx);
                     Abyss.LOGGER.info("[JSlop][Teleport] COOLDOWN_TIME before override: {}", before != null ? before.getDuration() + " ticks" : "not active");

                     player.removeEffect(cooldownFx);
                     player.addEffect(new MobEffectInstance(cooldownFx, 30, 0, false, false));

                     MobEffectInstance after = player.getEffect(cooldownFx);
                     Abyss.LOGGER.info("[JSlop][Teleport] COOLDOWN_TIME after override: {}", after != null ? after.getDuration() + " ticks" : "MISSING — addEffect failed");
                  }
               } else {
                  Abyss.LOGGER.info("[JSlop][Teleport] creative mode — skipping cooldown override");
               }

               ci.cancel();
            }
         } else {
            ci.cancel();
         }
      }
   }
}