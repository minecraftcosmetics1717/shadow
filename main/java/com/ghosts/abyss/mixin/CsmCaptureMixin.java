package com.ghosts.abyss.mixin;

import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.network.CsmSync;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.GetoCancelTechniqueProcedure"},
        remap = false
)
public class CsmCaptureMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (!(entity instanceof ServerPlayer player)) {
         return;
      }

      // Only execute logic if the player has Cursed Spirit Manipulation (CSM)
      if (isCsmPlayer(player)) {

         // Initialize 'friend_num' NBT if it doesn't exist
         if (player.getPersistentData().getDouble("friend_num") == 0.0D) {
            player.getPersistentData().putDouble("friend_num", Math.random());
         }

         Vec3 center = new Vec3(x, y, z);
         // Search a 128-block bounding box around the target position for entities
         AABB searchArea = new AABB(center, center).inflate(128.0D);
         List<Entity> nearbyEntities = world.getEntitiesOfClass(Entity.class, searchArea, e -> true);

         boolean foundAny = false;

         for (Entity candidate : nearbyEntities) {
            if (candidate != entity && candidate instanceof LivingEntity living) {

               // Check if the entity is a valid Cursed Spirit owned by this player that isn't selected
               boolean isCursedSpirit = candidate.getPersistentData().getBoolean("CursedSpirit");
               boolean isOwner = candidate.getPersistentData().getString("OWNER_UUID").equals(player.getStringUUID());
               boolean isNotSelected = candidate.getPersistentData().getDouble("select") == 0.0D;

               if (isCursedSpirit && isOwner && isNotSelected) {
                  foundAny = true;

                  // Determine Entity Type
                  String csmRosterType = candidate.getPersistentData().getString("CSM_ROSTER_TYPE");
                  ResourceLocation registryName = ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType());
                  String entityType = !csmRosterType.isEmpty() ? csmRosterType : (registryName != null ? registryName.toString() : "unknown");

                  // Determine Display Name
                  String csmDisplayName = candidate.getPersistentData().getString("CSM_DISPLAY_NAME");
                  String displayName = !csmDisplayName.isEmpty() ? csmDisplayName : candidate.getName().getString();

                  // Fetch Attributes (Max HP & Attack Damage)
                  double maxHp = 20.0D;
                  double atk = 3.0D;

                  if (living.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                     maxHp = living.getAttributeValue(Attributes.MAX_HEALTH);
                  }
                  if (living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                     atk = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
                  }

                  // Check Wither effect level
                  int witherLevel = 0;
                  if (living.hasEffect(MobEffects.WITHER)) {
                     MobEffectInstance witherEffect = living.getEffect(MobEffects.WITHER);
                     if (witherEffect != null) {
                        witherLevel = witherEffect.getAmplifier();
                     }
                  }

                  // Spawn particle effects at the spirit's location
                  if (world instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(
                             ParticleTypes.SMOKE,
                             candidate.getX(),
                             candidate.getY() + (candidate.getBbHeight() * 0.5D),
                             candidate.getZ(),
                             15, 0.4, 0.4, 0.4, 0.0D
                     );
                  }

                  // Manage Spirit UUID and pending skill logic
                  String storedMarker = candidate.getPersistentData().getString("CSM_SPIRIT_UUID");
                  boolean isOurDeployed = !storedMarker.isEmpty();
                  String resolvedUuid = (isOurDeployed && !storedMarker.startsWith("DEPLOYED:")) ? storedMarker : "";

                  int[] pendingSkills = candidate.getPersistentData().getIntArray("CSM_PENDING_SKILLS");
                  if (pendingSkills.length > 0 && isOurDeployed) {
                     List<Integer> observedSkills = Arrays.stream(pendingSkills).boxed().collect(Collectors.toList());
                     CsmRoster.mergeDiscoveredSkills(player, entityType, resolvedUuid, observedSkills);
                  }

                  // Either restore an existing spirit or capture a new one
                  if (isOurDeployed) {
                     CsmRoster.restoreSpirit(player, entityType, resolvedUuid, displayName);
                     if (candidate.getStringUUID().equals(CsmRoster.getSelectedUuid(player))) {
                        CsmRoster.setSelectedUuid(player, "");
                     }
                  } else {
                     CsmRoster.captureSpirit(player, entityType, displayName, maxHp, atk, witherLevel);
                  }

                  // Despawn/Discard the entity from the world
                  candidate.discard();
                  break;
               }
            }
         }

         // Sync the new roster to the player's client
         if (foundAny) {
            CsmSync.sendRoster(player);
         }

         // Reset active skill NBT tag and cancel MCreator's default procedure
         player.getPersistentData().putDouble("skill", 0.0D);
         ci.cancel();
      }
   }

   /**
    * Checks via reflection if the player has the CSM (Cursed Spirit Manipulation)
    * technique assigned (ID 18.0) in Jujutsucraft variables.
    */
   private static boolean isCsmPlayer(ServerPlayer player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);

         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = player.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return false;
         }

         Field ct1 = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct1.setAccessible(true);
         ct2.setAccessible(true);

         double c1 = (Double) ct1.get(vars);
         double c2 = (Double) ct2.get(vars);

         // Technique ID 18.0 represents Cursed Spirit Manipulation (Geto / Kenjaku)
         return c1 == 18.0D || c2 == 18.0D;
      } catch (Exception e) {
         return false;
      }
   }
}