package com.ghosts.abyss.event;

import java.util.Random;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class HwbParticleTicker {

   private static final Random RANDOM = new Random();

   private static final Set<String> HWB_BOSS_ENTITIES = Set.of(
           "jujutsucraft:sukuna_perfect",
           "jujutsucraft:sukuna",
           "jujutsucraft:sukuna_fushiguro",
           "jujutsucraft:reggie_star",
           "jujutsucraft:kashimo_hajime",
           "jujutsucrafts:kashimo_old"
   );

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
         if (player.getPersistentData().getBoolean("hwb_active") && player.level() instanceof ServerLevel serverLevel) {
            spawnMovingBasketParticles(serverLevel, player, 1.35D, player.getDeltaMovement());
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(TickEvent.LevelTickEvent event) {
      if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
         for (var entity : serverLevel.getAllEntities()) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
               ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
               if (rl != null && HWB_BOSS_ENTITIES.contains(rl.toString())) {
                  if (hasSimpleDomainEffect(living)) {
                     double radius = Math.max(1.35D, living.getBbWidth() * 1.2D);
                     spawnMovingBasketParticles(serverLevel, living, radius, living.getDeltaMovement());
                  }
               }
            }
         }
      }
   }

   private static boolean hasSimpleDomainEffect(LivingEntity living) {
      return living.getActiveEffects().stream().anyMatch(effect -> {
         ResourceLocation loc = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
         return loc != null && loc.toString().equals("jujutsucraft:simple_domain");
      });
   }

   /**
    * Spawns particles with negative delta velocity toward the entity's center,
    * preventing trailing particles while walking.
    */
   private static void spawnMovingBasketParticles(ServerLevel level, LivingEntity entity, double radius, Vec3 motion) {
      double px = entity.getX();
      double py = entity.getY() + (entity.getBbHeight() * 0.5D);
      double pz = entity.getZ();

      // Reduce particle count while moving to eliminate trailing visual clutter
      boolean isMoving = motion.horizontalDistanceSqr() > 0.005D;
      int particleCount = isMoving ? 8 : 16;

      double phi = Math.PI * (3.0D - Math.sqrt(5.0D));

      for (int i = 0; i < particleCount; i++) {
         int sampleIdx = RANDOM.nextInt(40);

         double y = 1.0D - (sampleIdx / (double) (40 - 1)) * 2.0D;
         double r = Math.sqrt(1.0D - y * y);
         double theta = phi * sampleIdx;

         double x = Math.cos(theta) * r;
         double z = Math.sin(theta) * r;

         double spawnX = px + (x * radius);
         double spawnY = py + (y * radius);
         double spawnZ = pz + (z * radius);

         // Send particles with zero speed modifier so they stay exactly where created
         level.sendParticles(
                 ParticleTypes.WAX_OFF,
                 spawnX, spawnY, spawnZ,
                 1,
                 0.0D, 0.0D, 0.0D, // Offsets set to 0
                 0.0D             // Speed set to 0 so they don't drift
         );
      }
   }
}