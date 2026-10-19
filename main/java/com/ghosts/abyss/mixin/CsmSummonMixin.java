package com.ghosts.abyss.mixin;

import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.csm.CsmSpiritData;
import com.ghosts.abyss.network.CsmSync;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts Jujutsucraft's Test2Procedure (used for Cursed Spirit Manipulation summoning)[cite: 8].
 * Replaces default behavior with CSM Roster selection, custom entity positioning,
 * stat application, effects, and networking[cite: 8].
 */
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.Test2Procedure"},
        remap = false
)
public class CsmSummonMixin {
   private static final Map<String, String> FINAL_FORM_MAP = new HashMap<>();

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         if (isCsmPlayer(player)) {
            if (CsmRoster.hasRoster(player)) {
               List<CsmSpiritData> spirits = CsmRoster.getSpirits(player);
               if (spirits.isEmpty()) {
                  player.getPersistentData().putDouble("skill", 0.0D);
                  ci.cancel();
               } else {
                  int summonIdx = -1;

                  // 1. Check explicit target index override
                  if (player.getPersistentData().contains("jslop_csm_target_idx")) {
                     int targetIdx = player.getPersistentData().getInt("jslop_csm_target_idx");
                     player.getPersistentData().remove("jslop_csm_target_idx");
                     if (targetIdx >= 0 && targetIdx < spirits.size() && spirits.get(targetIdx).count > 0) {
                        summonIdx = targetIdx;
                     }
                  }

                  // 2. Fall back to Jujutsucraft GUI selection match
                  if (summonIdx < 0) {
                     String jjcSelected = getSelectedTechniqueName(player);
                     if (!jjcSelected.isEmpty()) {
                        for (int i = 0; i < spirits.size(); ++i) {
                           CsmSpiritData sd = spirits.get(i);
                           if (sd.count > 0 && (sd.displayName + " ×" + sd.count).equals(jjcSelected)) {
                              summonIdx = i;
                              break;
                           }
                        }
                     }
                  }

                  // 3. Fall back to current selected roster index
                  if (summonIdx < 0) {
                     int rosterIdx = CsmRoster.getSelectedRosterIdx(player);
                     if (rosterIdx >= 0 && rosterIdx < spirits.size() && spirits.get(rosterIdx).count > 0) {
                        summonIdx = rosterIdx;
                     }
                  }

                  // 4. Default to first available spirit
                  if (summonIdx < 0) {
                     for (int i = 0; i < spirits.size(); ++i) {
                        if (spirits.get(i).count > 0) {
                           summonIdx = i;
                           break;
                        }
                     }
                  }

                  if (summonIdx < 0) {
                     player.getPersistentData().putDouble("skill", 0.0D);
                     ci.cancel();
                  } else {
                     CsmSpiritData spirit = spirits.get(summonIdx);
                     if (!(world instanceof ServerLevel serverLevel)) {
                        player.getPersistentData().putDouble("skill", 0.0D);
                        ci.cancel();
                     } else {
                        Vec3 spawnPos = computeSpawnPos(player);
                        String spawnType = FINAL_FORM_MAP.getOrDefault(spirit.entityType, spirit.entityType);
                        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(spawnType));

                        if (type == null) {
                           player.sendSystemMessage(Component.literal("§c[CSM] Unknown spirit type: " + spawnType), true);
                           player.getPersistentData().putDouble("skill", 0.0D);
                           ci.cancel();
                        } else {
                           Entity spawned = type.create(serverLevel);
                           if (spawned == null) {
                              player.getPersistentData().putDouble("skill", 0.0D);
                              ci.cancel();
                           } else {
                              spawned.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                              spawned.setYRot(player.getYRot() + (float) ((Math.random() - 0.5D) * 45.0D));
                              spawned.setXRot(Math.min(player.getXRot(), 0.0F));

                              // Set entity persistent NBT flags
                              spawned.getPersistentData().putBoolean("CursedSpirit", true);
                              spawned.getPersistentData().putString("OWNER_UUID", player.getStringUUID());
                              spawned.getPersistentData().putBoolean("JujutsuSorcerer", false);
                              spawned.getPersistentData().putBoolean("Player", true);
                              spawned.getPersistentData().putBoolean("Ambush", true);

                              if (spawned instanceof Mob mob) {
                                 mob.setPersistenceRequired();
                              }

                              double friendNum = player.getPersistentData().getDouble("friend_num");
                              if (friendNum == 0.0D) {
                                 friendNum = Math.random();
                                 player.getPersistentData().putDouble("friend_num", friendNum);
                              }

                              spawned.getPersistentData().putDouble("friend_num", friendNum);
                              spawned.getPersistentData().putDouble("friend_num_worker", friendNum);
                              spawned.getPersistentData().putString("CSM_DISPLAY_NAME", spirit.displayName);

                              String spiritMarker = (spirit.isUnique && spirit.uuid != null && !spirit.uuid.isEmpty())
                                      ? spirit.uuid
                                      : "DEPLOYED:" + spirit.entityType;

                              spawned.getPersistentData().putString("CSM_SPIRIT_UUID", spiritMarker);
                              spawned.getPersistentData().putString("CSM_ROSTER_TYPE", spirit.entityType);
                              spawned.setCustomName(Component.literal(spirit.displayName));

                              if (spawned instanceof LivingEntity living) {
                                 applyStats(living, spirit);

                                 try {
                                    Class<?> modEffects = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects");

                                    Field cooldownField = modEffects.getField("COOLDOWN_TIME_COMBAT");
                                    cooldownField.setAccessible(true);
                                    Object cooldownHolder = cooldownField.get(null);
                                    MobEffect cooldownEffect = (MobEffect) ((RegistryObject<?>) cooldownHolder).get();
                                    living.addEffect(new MobEffectInstance(cooldownEffect, 50, 0, false, false));

                                    Field ctField = modEffects.getField("CURSED_TECHNIQUE");
                                    ctField.setAccessible(true);
                                    Object ctHolder = ctField.get(null);
                                    MobEffect ctEffect = (MobEffect) ((RegistryObject<?>) ctHolder).get();
                                    living.addEffect(new MobEffectInstance(ctEffect, Integer.MAX_VALUE, 0, false, false));
                                 } catch (Exception ignored) {
                                 }

                                 living.getPersistentData().putDouble("skill", 0.0D);
                                 living.getPersistentData().putDouble("cnt1", 0.0D);
                                 living.getPersistentData().putDouble("cnt6", 0.0D);
                              }

                              applyFinalForm(spawned, spawnType);
                              serverLevel.addFreshEntity(spawned);

                              serverLevel.sendParticles(
                                      ParticleTypes.SQUID_INK,
                                      spawnPos.x, spawnPos.y, spawnPos.z,
                                      25, 0.5D, 0.5D, 0.5D, 0.0D
                              );

                              CsmRoster.deploySpirit(player, summonIdx);
                              CsmRoster.setSelectedUuid(player, spawned.getStringUUID());
                              CsmSync.sendRoster(player);

                              player.getPersistentData().putDouble("skill", 0.0D);
                              ci.cancel();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static Vec3 computeSpawnPos(ServerPlayer player) {
      Vec3 start = player.getEyePosition(1.0F);
      Vec3 look = player.getLookAngle().scale(5.0D);
      Vec3 end = start.add(look);
      HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      return hit.getLocation();
   }

   private static void applyStats(LivingEntity living, CsmSpiritData spirit) {
      double hp = spirit.scaledHp();
      double atk = spirit.scaledAtk();

      if (living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
         living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
      }

      living.setHealth((float) hp);

      if (living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
         living.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
      }
   }

   @SuppressWarnings("unchecked")
   private static String getSelectedTechniqueName(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, (Direction) null).orElse(null);

         if (vars == null) {
            return "";
         } else {
            Field nameField = vars.getClass().getDeclaredField("PlayerSelectCurseTechniqueName");
            nameField.setAccessible(true);
            return (String) nameField.get(vars);
         }
      } catch (Exception e) {
         return "";
      }
   }

   private static void applyFinalForm(Entity spawned, String spawnType) {
      switch (spawnType) {
         case "jujutsucraft:dagon":
            setFormAndTexture(spawned, "net.mcreator.jujutsucraft.entity.DagonEntity", 1, "dagon2");
            break;
         case "jujutsucraft:zenin_naoya_cursed_spirit":
            setFormAndTexture(spawned, "net.mcreator.jujutsucraft.entity.ZeninNaoyaCursedSpiritEntity", 3, "naoya_cursed_spirit3");
            spawned.getPersistentData().putDouble("flag_domain", 1.0D);
            break;
      }
   }

   @SuppressWarnings("unchecked")
   private static void setFormAndTexture(Entity entity, String className, int form, String texture) {
      try {
         Class<?> cls = Class.forName(className);
         Field dataFormField = cls.getDeclaredField("DATA_form");
         dataFormField.setAccessible(true);
         EntityDataAccessor<Integer> dataForm = (EntityDataAccessor<Integer>) dataFormField.get(null);
         entity.getEntityData().set(dataForm, form);
         cls.getDeclaredMethod("setTexture", String.class).invoke(entity, texture);
         entity.refreshDimensions();
      } catch (Exception ignored) {
      }
   }

   @SuppressWarnings("unchecked")
   private static boolean isCsmPlayer(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, (Direction) null).orElse(null);

         if (vars == null) {
            return false;
         } else {
            Field ct = vars.getClass().getDeclaredField("PlayerCurseTechnique");
            Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
            ct.setAccessible(true);
            ct2.setAccessible(true);

            double c1 = (Double) ct.get(vars);
            double c2 = (Double) ct2.get(vars);
            return c1 == 18.0D || c2 == 18.0D;
         }
      } catch (Exception e) {
         return false;
      }
   }

   static {
      FINAL_FORM_MAP.put("jujutsucraft:zenin_naoya", "jujutsucraft:zenin_naoya_cursed_spirit");
   }
}