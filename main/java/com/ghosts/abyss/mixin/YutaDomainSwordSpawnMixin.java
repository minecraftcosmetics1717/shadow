package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.AuthenticMutualLoveProcedure"},
        remap = false
)
public class YutaDomainSwordSpawnMixin {
   private static final int EXTRA_SWORDS = 160;
   private static final int EXTRA_GRAVESTONES = 48;
   private static final ThreadLocal<Deque<Boolean>> SPAWN_FLAG = ThreadLocal.withInitial(ArrayDeque::new);

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           remap = false
   )
   private static void jslop$head(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity != null && entity.getPersistentData().getBoolean("open_domain")) {
         double cnt1 = entity.getPersistentData().getDouble("cnt1");
         boolean willSpawn = cnt1 >= 1.0D && cnt1 < 34.0D;
         SPAWN_FLAG.get().push(willSpawn);
         Abyss.LOGGER.debug("[JSlop] YutaDomainSwordSpawnMixin HEAD: cnt1={} willSpawn={}", cnt1, willSpawn);
      } else {
         SPAWN_FLAG.get().push(false);
      }
   }

   @Inject(
           method = {"execute"},
           at = {@At("RETURN")},
           remap = false
   )
   private static void jslop$spawnOpenDomainDecorations(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      Deque<Boolean> stack = SPAWN_FLAG.get();
      if (!stack.isEmpty()) {
         boolean flagged = stack.pop();
         if (flagged) {
            if (entity != null) {
               double cnt1 = entity.getPersistentData().getDouble("cnt1");
               if (cnt1 != 34.0D) {
                  Abyss.LOGGER.debug("[JSlop] YutaDomainSwordSpawnMixin RETURN: flagged but cnt1={} (not 34), skipping", cnt1);
               } else if (world instanceof ServerLevel serverLevel) {
                  Abyss.LOGGER.info("[JSlop] YutaDomainSwordSpawnMixin: spawning {} extra swords and {} extra gravestones", EXTRA_SWORDS, EXTRA_GRAVESTONES);

                  try {
                     Class<?> mapVarsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables$MapVariables");
                     Method mapVarsGet = mapVarsClass.getMethod("get", LevelAccessor.class);
                     Object mapVars = mapVarsGet.invoke(null, world);

                     Field disField = mapVarsClass.getDeclaredField("DomainExpansionRadius");
                     disField.setAccessible(true);
                     double domainRadius = (Double) disField.get(mapVars);
                     double openRadius = domainRadius * 3.0D;

                     Abyss.LOGGER.debug("[JSlop] YutaDomainSwordSpawnMixin: dis={} openRadius={}", domainRadius, openRadius);

                     double domX = entity.getPersistentData().getDouble("x_pos_doma");
                     double domY = entity.getPersistentData().getDouble("y_pos_doma");
                     double domZ = entity.getPersistentData().getDouble("z_pos_doma");
                     double nameRanged = entity.getPersistentData().getDouble("NameRanged");

                     Abyss.LOGGER.info("[JSlop] YutaDomainSwordSpawnMixin: domCenter=({},{},{}) dis={} openRadius={} nameRanged={}", domX, domY, domZ, domainRadius, openRadius, nameRanged);

                     Class<?> entitiesClass = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModEntities");
                     EntityType<?> entityItemType = resolveEntityType(entitiesClass, "ENTITY_ITEM");
                     EntityType<?> gravestone4Type = resolveEntityType(entitiesClass, "GRAVESTONE_4");

                     if (entityItemType == null) {
                        Abyss.LOGGER.warn("[JSlop] YutaDomainSwordSpawnMixin: ENTITY_ITEM type is null, aborting");
                        return;
                     }

                     if (gravestone4Type == null) {
                        Abyss.LOGGER.warn("[JSlop] YutaDomainSwordSpawnMixin: GRAVESTONE_4 type is null, aborting");
                        return;
                     }

                     Item swordItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("jujutsucraft:sword_okkotsu_yuta"));
                     if (swordItem == null) {
                        Abyss.LOGGER.warn("[JSlop] YutaDomainSwordSpawnMixin: sword_okkotsu_yuta item not found");
                        return;
                     }

                     EntityDataAccessor<Boolean> decorAccessor = null;

                     try {
                        Class<?> entityItemEntityClass = Class.forName("net.mcreator.jujutsucraft.entity.EntityItemEntity");
                        Field decorField = entityItemEntityClass.getDeclaredField("DATA_domain_decoration");
                        decorField.setAccessible(true);

                        @SuppressWarnings("unchecked")
                        EntityDataAccessor<Boolean> acc = (EntityDataAccessor<Boolean>) decorField.get(null);
                        decorAccessor = acc;
                     } catch (Exception e) {
                        Abyss.LOGGER.warn("[JSlop] YutaDomainSwordSpawnMixin: DATA_domain_decoration not found: {}", e.getMessage());
                     }

                     int swordsSpawned = 0;

                     for (int i = 0; i < EXTRA_SWORDS; ++i) {
                        double angle = Math.toRadians(Math.random() * 720.0D);
                        double radius = 0.0D;

                        for (int j = 0; j < 32; ++j) {
                           double r = Math.random();
                           if (r < 0.9D) {
                              radius = r * (openRadius - 3.0D);
                              break;
                           }
                        }

                        double pitch = (Math.random() - 0.5D) * 30.0D;
                        double spawnX = domX + Math.sin(angle) * radius;
                        double spawnZ = domZ + Math.cos(angle) * radius;
                        double spawnY = surfaceY(serverLevel, spawnX, spawnZ, domY) - Math.sin(Math.toRadians(Math.abs(pitch * 3.0D)));

                        Entity spawned = entityItemType.create(serverLevel);
                        if (spawned != null) {
                           spawned.moveTo(spawnX, spawnY, spawnZ, (float) (Math.random() * 360.0D), (float) pitch);
                           spawned.setNoGravity(true);
                           spawned.getPersistentData().putDouble("NameRanged_ranged", nameRanged);
                           spawned.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());

                           if (spawned instanceof LivingEntity living) {
                              living.setItemSlot(EquipmentSlot.HEAD, new ItemStack(swordItem));
                           }

                           if (decorAccessor != null) {
                              try {
                                 spawned.getEntityData().set(decorAccessor, true);
                              } catch (Exception ignored) {
                              }
                           }

                           boolean added = serverLevel.addFreshEntity(spawned);
                           if (swordsSpawned == 0) {
                              Abyss.LOGGER.info("[JSlop] First sword: pos=({},{},{}) entityClass={} added={} isLiving={}", spawnX, spawnY, spawnZ, spawned.getClass().getSimpleName(), added, spawned instanceof LivingEntity);
                           }

                           if (added) {
                              ++swordsSpawned;
                           }
                        }
                     }

                     int gravestonesSpawned = 0;

                     for (int i = 0; i < EXTRA_GRAVESTONES; ++i) {
                        double angle = Math.toRadians(Math.random() * 720.0D);
                        double radius = 0.0D;

                        for (int j = 0; j < 32; ++j) {
                           double r = Math.random();
                           if (r > 0.45D && r < 0.9D) {
                              radius = r * (openRadius - 3.0D);
                              break;
                           }
                        }

                        double spawnX = domX + Math.sin(angle) * radius;
                        double spawnZ = domZ + Math.cos(angle) * radius;
                        double spawnY = surfaceY(serverLevel, spawnX, spawnZ, domY) - (Math.random() + 1.0D);
                        double yaw = Math.random() * 360.0D;
                        double pitch = (Math.random() - 0.5D) * 70.0D;

                        Entity spawned = gravestone4Type.create(serverLevel);
                        if (spawned != null) {
                           spawned.moveTo(spawnX, spawnY, spawnZ, (float) yaw, (float) pitch);
                           spawned.setNoGravity(true);
                           spawned.getPersistentData().putDouble("NameRanged_ranged", nameRanged);
                           spawned.getPersistentData().putString("OWNER_UUID", entity.getStringUUID());
                           serverLevel.addFreshEntity(spawned);
                           ++gravestonesSpawned;
                        }
                     }

                     Abyss.LOGGER.info("[JSlop] YutaDomainSwordSpawnMixin: spawned {} swords, {} gravestones over r={}", swordsSpawned, gravestonesSpawned, openRadius);
                  } catch (Exception e) {
                     Abyss.LOGGER.warn("[JSlop] YutaDomainSwordSpawnMixin failed: {} {}", e.getClass().getSimpleName(), e.getMessage());

                     for (StackTraceElement el : e.getStackTrace()) {
                        Abyss.LOGGER.debug("[JSlop]   at {}", el);
                     }
                  }
               }
            }
         }
      }
   }

   private static double surfaceY(ServerLevel level, double x, double z, double fallbackY) {
      int cx = (int) Math.floor(x) >> 4;
      int cz = (int) Math.floor(z) >> 4;
      return !level.hasChunk(cx, cz) ? fallbackY : (double) level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
   }

   private static EntityType<?> resolveEntityType(Class<?> entitiesClass, String fieldName) {
      try {
         Field f = entitiesClass.getDeclaredField(fieldName);
         f.setAccessible(true);
         Object holder = f.get(null);
         return (EntityType<?>) holder.getClass().getMethod("get").invoke(holder);
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] YutaDomainSwordSpawnMixin: resolveEntityType({}) failed: {}", fieldName, e.getMessage());
         return null;
      }
   }
}