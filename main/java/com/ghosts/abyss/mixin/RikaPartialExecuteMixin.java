package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.CursedTechniqueOkkotsuProcedure"},
        remap = false
)
public class RikaPartialExecuteMixin {
   private static final String PARTIAL_UUID_KEY = "jslop_partial_rika_uuid";

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void jslop$rikaPartialExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      double skill = entity.getPersistentData().getDouble("skill");

      // Skills 510 and 519 despawn any active partial Rika manifestation
      if (skill == 510.0D && entity instanceof ServerPlayer player) {
         if (player.level() instanceof ServerLevel serverLevel) {
            despawnPartialRika(player, serverLevel);
         }
      } else if (skill == 519.0D && entity instanceof ServerPlayer player) {
         if (player.level() instanceof ServerLevel serverLevel) {
            despawnPartialRika(player, serverLevel);
         }
      } else if (skill == 511.0D) { // Skill 511 = Spawn / Toggle Partial Rika Manifestation
         ci.cancel();

         if (entity instanceof ServerPlayer player) {
            boolean authorized = player.getPersistentData().getBoolean("jslop_rika_partial_auth");
            player.getPersistentData().putBoolean("jslop_rika_partial_auth", false);

            MobEffect cursedTechEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse("jujutsucraft:cursed_technique"));

            if (!authorized) {
               player.getPersistentData().putDouble("skill", 0.0D);
               if (cursedTechEffect != null) {
                  player.removeEffect(cursedTechEffect);
               }
            } else {
               if (player.level() instanceof ServerLevel serverLevel) {
                  // If partial Rika is already spawned, despawn her
                  if (player.getPersistentData().hasUUID("jslop_partial_rika_uuid")) {
                     despawnPartialRika(player, serverLevel);
                     player.getPersistentData().putDouble("skill", 0.0D);
                     if (cursedTechEffect != null) {
                        player.removeEffect(cursedTechEffect);
                     }
                  } else {
                     // Verify whether full Rika is already spawned in world before spawning partial Rika
                     String rikaUuidStr = player.getPersistentData().getString("RIKA_UUID");
                     if (!rikaUuidStr.isEmpty()) {
                        try {
                           UUID fullRikaUuid = UUID.fromString(rikaUuidStr);
                           Entity fullRika = serverLevel.getEntity(fullRikaUuid);

                           if (fullRika == null) {
                              for (ServerLevel lvl : player.getServer().getAllLevels()) {
                                 fullRika = lvl.getEntity(fullRikaUuid);
                                 if (fullRika != null) {
                                    break;
                                 }
                              }
                           }

                           if (fullRika != null && fullRika.isAlive()) {
                              player.getPersistentData().putDouble("skill", 0.0D);
                              if (cursedTechEffect != null) {
                                 player.removeEffect(cursedTechEffect);
                              }
                              return;
                           }
                        } catch (IllegalArgumentException ignored) {
                        }
                     }

                     Advancement curseLiftedAdv = player.getServer().getAdvancements().getAdvancement(ResourceLocation.tryParse("jujutsucraft:skill_curseis_lifted"));
                     boolean curseIsLifted = curseLiftedAdv != null && player.getAdvancements().getOrStartProgress(curseLiftedAdv).isDone();

                     // Raycast 6 blocks behind the player to find the partial Rika spawn location
                     Vec3 eyePos = player.getEyePosition();
                     Vec3 behindTarget = eyePos.add(player.getViewVector(1.0F).scale(-6.0D));
                     Vec3 spawnPos = player.level().clip(new ClipContext(eyePos, behindTarget, Block.OUTLINE, Fluid.NONE, player)).getLocation();

                     String entityId = curseIsLifted ? "rika_2" : "rika";
                     EntityType<Entity> entityType = (EntityType<Entity>) ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse("jujutsucraft:" + entityId));

                     if (entityType == null) {
                        Abyss.LOGGER.warn("[JSlop] RikaPartial: entity type jujutsucraft:{} not found", entityId);
                        player.getPersistentData().putDouble("skill", 0.0D);
                        if (cursedTechEffect != null) {
                           player.removeEffect(cursedTechEffect);
                        }
                     } else {
                        Entity rika = entityType.spawn(
                                serverLevel,
                                (CompoundTag) null,
                                (Consumer<Entity>) null,
                                BlockPos.containing(spawnPos.x, spawnPos.y, spawnPos.z),
                                MobSpawnType.MOB_SUMMONED,
                                false,
                                false
                        );

                        if (rika != null) {
                           rika.setYRot(player.getYRot());
                           rika.setXRot(0.0F);

                           player.getPersistentData().putUUID("jslop_partial_rika_uuid", rika.getUUID());
                           rika.getPersistentData().putString("OWNER_UUID", player.getStringUUID());

                           double friendNum = player.getPersistentData().getDouble("friend_num");
                           if (friendNum == 0.0D) {
                              friendNum = Math.random();
                              player.getPersistentData().putDouble("friend_num", friendNum);
                           }

                           rika.getPersistentData().putDouble("friend_num", friendNum);
                           rika.getPersistentData().putDouble("friend_num_worker", friendNum);
                           rika.getPersistentData().putBoolean("JujutsuSorcerer", player.getPersistentData().getBoolean("JujutsuSorcerer"));
                           rika.getPersistentData().putBoolean("CurseUser", player.getPersistentData().getBoolean("CurseUser"));
                           rika.getPersistentData().putBoolean("Player", true);

                           if (rika instanceof LivingEntity rikaLiving) {
                              if (!serverLevel.isClientSide()) {
                                 int ownerAmp = player.hasEffect(MobEffects.DAMAGE_BOOST) ? player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() : 0;
                                 int rikaBaseAmp = curseIsLifted ? 16 : 20;
                                 int finalAmp = Math.max(ownerAmp, rikaBaseAmp);

                                 rikaLiving.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, finalAmp, false, false));
                              }
                           }

                           serverLevel.sendParticles(ParticleTypes.SQUID_INK, spawnPos.x, spawnPos.y + 1.0D, spawnPos.z, 25, 0.5D, 0.5D, 0.5D, 0.1D);
                        } else {
                           Abyss.LOGGER.warn("[JSlop] RikaPartial: spawn returned null for jujutsucraft:{}", entityId);
                        }

                        player.getPersistentData().putDouble("skill", 0.0D);
                        if (cursedTechEffect != null) {
                           player.removeEffect(cursedTechEffect);
                        }
                     }
                  }
               } else {
                  player.getPersistentData().putDouble("skill", 0.0D);
                  if (cursedTechEffect != null) {
                     player.removeEffect(cursedTechEffect);
                  }
               }
            }
         }
      }
   }

   /**
    * Triggers the despawn sequence for partial Rika by passing despawn_flag=2.0 to the target entity.
    */
   private static void despawnPartialRika(ServerPlayer player, ServerLevel serverLevel) {
      if (player.getPersistentData().hasUUID("jslop_partial_rika_uuid")) {
         UUID uuid = player.getPersistentData().getUUID("jslop_partial_rika_uuid");
         Entity entity = serverLevel.getEntity(uuid);

         if (entity == null) {
            for (ServerLevel lvl : player.getServer().getAllLevels()) {
               entity = lvl.getEntity(uuid);
               if (entity != null) {
                  break;
               }
            }
         }

         if (entity != null && entity.isAlive()) {
            entity.getPersistentData().putDouble("despawn_flag", 2.0D);
         }

         player.getPersistentData().remove("jslop_partial_rika_uuid");
      }
   }
}