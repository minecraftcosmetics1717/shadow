package com.ghosts.abyss.mixin;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.YutaTechniqueRegistry;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "net.mcreator.jujutsucraft.procedures.AuthenticMutualLoveActiveProcedure",
        remap = false
)
public class AuthenticMutualLoveMixin {

   @Inject(
           method = "execute",
           at = @At("HEAD"),
           cancellable = true,
           remap = false
   )
   private static void jslop$yutaDomainSureHit(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (entity instanceof ServerPlayer player) {
         int domainIdx = YutaTechniqueRegistry.getDomainIdx(player);
         if (domainIdx >= 0) {
            ListTag list = YutaTechniqueRegistry.getList(player);
            if (domainIdx < list.size()) {
               if (!player.getPersistentData().getBoolean("Failed")) {
                  CompoundTag tech = list.getCompound(domainIdx);
                  double skill = tech.getDouble("skill");
                  double cooldown = tech.getDouble("cooldown_ticks");
                  String shikigamiName = tech.getString("shikigami_name");
                  double shikigamiHp = tech.getDouble("shikigami_hp");

                  if (!shikigamiName.isEmpty()) {
                     jslop$shikigamiPath(world, player, skill, cooldown, shikigamiName, shikigamiHp);
                  } else {
                     boolean isDomainSkill = skill >= 100.0D && skill % 100.0D <= 20.0D && !(skill >= 500.0D && skill < 600.0D);
                     if (isDomainSkill) {
                        double oldSkill = player.getPersistentData().getDouble("skill");
                        double oldCooldown = player.getPersistentData().getDouble("COOLDOWN_TICKS");
                        double oldSkillDomain = player.getPersistentData().getDouble("skill_domain");

                        player.getPersistentData().putDouble("skill", skill);
                        player.getPersistentData().putDouble("COOLDOWN_TICKS", cooldown);
                        player.getPersistentData().putDouble("skill_domain", Math.floor(skill / 100.0D));

                        try {
                           Class.forName("net.mcreator.jujutsucraft.procedures.DomainActiveProcedure")
                                   .getMethod("execute", LevelAccessor.class, double.class, double.class, double.class, Entity.class)
                                   .invoke(null, world, x, y, z, player);
                        } catch (Exception e) {
                           Abyss.LOGGER.warn("[JSlop] AuthenticMutualLoveMixin: DomainActiveProcedure failed: {}", e.getMessage());
                        }

                        player.getPersistentData().putDouble("skill", oldSkill);
                        player.getPersistentData().putDouble("COOLDOWN_TICKS", oldCooldown);
                        player.getPersistentData().putDouble("skill_domain", oldSkillDomain);
                     } else {
                        Abyss.LOGGER.warn("[JSlop] AuthenticMutualLoveMixin: selected technique skill={} is not a valid domain or shikigami skill; sure-hit suppressed.", skill);
                     }
                  }

                  ci.cancel();
               }
            }
         }
      }
   }

   private static void jslop$shikigamiPath(LevelAccessor world, ServerPlayer player, double skill, double cooldown, String shikigamiName, double shikigamiHp) {
      MobEffect domainFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "domain_expansion"));
      double tick = 0.0D;

      if (domainFx != null && player.hasEffect(domainFx)) {
         MobEffectInstance effectInstance = player.getEffect(domainFx);
         if (effectInstance != null) {
            tick = effectInstance.getDuration();
         }
      }

      double range = 20.0D;

      try {
         Class<?> mapVarsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables$MapVariables");
         Object mapVars = mapVarsClass.getMethod("get", LevelAccessor.class).invoke(null, world);
         double domainRadius = mapVarsClass.getDeclaredField("DomainExpansionRadius").getDouble(mapVars);
         range = domainRadius * 2.0D;
      } catch (Exception e) {
         Abyss.LOGGER.warn("[JSlop] AuthenticMutualLoveMixin: MapVariables.DomainExpansionRadius: {}", e.getMessage());
      }

      double interval = Math.round(Math.max(Math.ceil(cooldown / 75.0D), 1.0D) * 40.0D);
      boolean startDomainAttack = player.getPersistentData().getBoolean("StartDomainAttack");

      if (tick % interval == 0.0D || startDomainAttack) {
         double numToSpawn = Math.ceil(Math.max(250.0D - cooldown, 75.0D) / 75.0D);
         double domX = player.getPersistentData().getDouble("x_pos_doma");
         double domY = player.getPersistentData().getDouble("y_pos_doma");
         double domZ = player.getPersistentData().getDouble("z_pos_doma");
         Vec3 domCenter = new Vec3(domX, domY, domZ);
         List<Entity> candidates = world.getEntitiesOfClass(Entity.class, new AABB(domCenter, domCenter).inflate(range / 2.0D), exx -> true);
         boolean noTargets = true;

         for (Entity target : candidates) {
            if (target != player && target instanceof LivingEntity && target.isAlive()) {
               try {
                  boolean ld = (Boolean) Class.forName("net.mcreator.jujutsucraft.procedures.LogicAttackDomainProcedure")
                          .getMethod("execute", LevelAccessor.class, Entity.class, Entity.class)
                          .invoke(null, world, player, target);
                  boolean la = (Boolean) Class.forName("net.mcreator.jujutsucraft.procedures.LogicAttackProcedure")
                          .getMethod("execute", LevelAccessor.class, Entity.class, Entity.class)
                          .invoke(null, world, player, target);

                  if (ld && la) {
                     noTargets = false;
                     break;
                  }
               } catch (Exception ignored) {
               }
            }
         }

         if (noTargets) {
            player.getPersistentData().putBoolean("StartDomainAttack", true);
         } else {
            player.getPersistentData().putBoolean("StartDomainAttack", false);
            double oldSkill = player.getPersistentData().getDouble("skill");
            double oldCooldown = player.getPersistentData().getDouble("COOLDOWN_TICKS");

            player.getPersistentData().putDouble("skill", skill);
            player.getPersistentData().putDouble("COOLDOWN_TICKS", cooldown);

            TagKey<EntityType<?>> rangedAmmoTag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "ranged_ammo"));
            double spawnCount = 0.0D;

            for (int i = 0; i < (int) Math.round(numToSpawn); ++i) {
               ++spawnCount;
               if (Math.random() < 0.5D || spawnCount <= 1.0D) {
                  double angle = Math.toRadians(Math.random() * 360.0D);
                  double spawnX = domX + Math.sin(angle) * (range / 2.0D - 4.0D) + Math.random() * 0.5D;
                  double spawnY = domY + Math.random() * 0.5D;
                  double spawnZ = domZ + Math.cos(angle) * (range / 2.0D - 4.0D) + Math.random() * 0.5D;

                  if (world instanceof ServerLevel level) {
                     CommandSourceStack sourceStack = new CommandSourceStack(
                             CommandSource.NULL,
                             new Vec3(spawnX, spawnY, spawnZ),
                             Vec2.ZERO,
                             level,
                             4,
                             "",
                             Component.empty(),
                             level.getServer(),
                             null
                     ).withSuppressedOutput();

                     String summonCmd = String.format(
                             "summon %s ~ ~ ~ {Health:%df,Attributes:[{Name:generic.max_health,Base:%df}],Rotation:[%fF,0F]}",
                             shikigamiName, Math.round(shikigamiHp), Math.round(shikigamiHp), Math.toDegrees(angle)
                     );

                     level.getServer().getCommands().performPrefixedCommand(sourceStack, summonCmd);
                  }

                  Vec3 spawnCenter = new Vec3(spawnX, spawnY, spawnZ);

                  for (Entity e : world.getEntitiesOfClass(Entity.class, new AABB(spawnCenter, spawnCenter).inflate(0.5D), exx -> true)) {
                     if (e.getType().is(rangedAmmoTag) && e.getPersistentData().getDouble("NameRanged_ranged") == 0.0D) {
                        try {
                           Class.forName("net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure")
                                   .getMethod("execute", Entity.class, Entity.class)
                                   .invoke(null, player, e);
                        } catch (Exception ex) {
                           Abyss.LOGGER.warn("[JSlop] AuthenticMutualLoveMixin: SetRangedAmmoProcedure: {}", ex.getMessage());
                        }
                        break;
                     }
                  }
               }
            }

            player.getPersistentData().putDouble("skill", oldSkill);
            player.getPersistentData().putDouble("COOLDOWN_TICKS", oldCooldown);
         }
      }
   }
}