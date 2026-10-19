package com.ghosts.abyss.mixin;

import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.network.CsmSync;
import java.lang.reflect.Field;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = {"net.mcreator.jujutsucraft.procedures.SkillUzumakiProcedure"},
        remap = false
)
public class CsmUzumakiMixin {

   @Inject(
           method = {"execute"},
           at = {@At("HEAD")},
           cancellable = true,
           remap = false
   )
   private static void onExecute(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
      if (!(entity instanceof ServerPlayer player) || !isCsmPlayer(player)) {
         return;
      }

      if (!CsmRoster.hasRoster(player)) {
         return;
      }

      double cnt9 = entity.getPersistentData().getDouble("cnt9");

      // Charging Phase 1: Wind-up charging before key release check
      if (cnt9 < 20.0D) {
         entity.getPersistentData().putDouble("cnt9", cnt9 + 1.0D);

         // If player releases key before full charge, skip to state 30
         if (!entity.getPersistentData().getBoolean("PRESS_Z")) {
            entity.getPersistentData().putDouble("cnt9", 30.0D);
         }

         if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y + 1.5D, z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
         }

         ci.cancel();
         return;
      }

      // Early Release Trigger: Switches active skill ID to 1816
      if (cnt9 >= 30.0D && cnt9 < 35.0D) {
         entity.getPersistentData().putDouble("skill", 1816.0D);
         ci.cancel();
         return;
      }

      // Full Charge Execution Phase
      if (cnt9 >= 20.0D && cnt9 < 25.0D) {
         double cnt1 = entity.getPersistentData().getDouble("cnt1") + 1.0D;
         entity.getPersistentData().putDouble("cnt1", cnt1);

         // Calculate Uzumaki spawn position 3 blocks in front of player
         double yawRad = Math.toRadians(entity.getYRot() + 90.0F);
         double dist = -3.0D - entity.getBbWidth();

         entity.getPersistentData().putDouble("x_pos", x + Math.cos(yawRad) * dist);
         entity.getPersistentData().putDouble("y_pos", y + entity.getBbHeight() * 0.75D);
         entity.getPersistentData().putDouble("z_pos", z + Math.sin(yawRad) * dist);

         double xPos = entity.getPersistentData().getDouble("x_pos");
         double yPos = entity.getPersistentData().getDouble("y_pos");
         double zPos = entity.getPersistentData().getDouble("z_pos");

         if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, false));
         }

         // Charge Particle Build-Up Phase
         if (cnt1 < 20.0D) {
            double pct = cnt1 / 19.5D;
            if (world instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.SQUID_INK, xPos, yPos + 2.0D, zPos, (int) (50.0D * pct), 1.5D * pct, 1.5D * pct, 1.5D * pct, 0.0D);
               serverLevel.sendParticles(ParticleTypes.SMOKE, xPos, yPos + 2.0D, zPos, (int) (25.0D * pct), 1.5D * pct, 1.5D * pct, 1.5D * pct, 0.0D);
            }
         } else {
            // Main Uzumaki Spawning Trigger
            entity.getPersistentData().putDouble("cnt2", entity.getPersistentData().getDouble("cnt2") + 1.0D);

            if (entity.getPersistentData().getDouble("cnt3") == 0.0D) {
               entity.getPersistentData().putDouble("cnt3", 1.0D);

               // Consume spirits from roster to calculate bonus attack damage
               double witherAccum = CsmRoster.uzumakiConsume(player);
               double cnt6 = entity.getPersistentData().getDouble("cnt6");
               entity.getPersistentData().putDouble("cnt6", cnt6 + witherAccum);

               drainCursedEnergy(player, 700.0D);

               if (entity instanceof LivingEntity living) {
                  double cooldownTicks = entity.getPersistentData().getDouble("COOLDOWN_TICKS");
                  applyJjcEffect(living, "COOLDOWN_TIME", (int) (cooldownTicks * 2.0D), 0);
                  applyJjcEffect(living, "ZONE", 100, 0);
               }

               // Play audio effects
               if (world instanceof Level level) {
                  SoundEvent slowMotionSound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse("jujutsucraft:slow_motion_end"));
                  SoundEvent pianoSound = ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse("jujutsucraft:piano_horror"));

                  level.playSound((Player) null, BlockPos.containing(x, y, z), slowMotionSound, SoundSource.NEUTRAL, 2.0F, 1.0F);
                  level.playSound((Player) null, BlockPos.containing(x, y, z), pianoSound, SoundSource.NEUTRAL, 1.0F, 0.25F);
               }

               // Spawn Uzumaki Entity in world
               if (world instanceof ServerLevel serverLevel) {
                  int witherAmp = 0;
                  if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.WITHER)) {
                     MobEffectInstance witherEffect = living.getEffect(MobEffects.WITHER);
                     if (witherEffect != null) {
                        witherAmp = witherEffect.getAmplifier();
                     }
                  }

                  double hp = 400.0D + (witherAmp * 40.0D);
                  EntityType<?> uzumakiType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse("jujutsucraft:uzumaki"));

                  if (uzumakiType != null) {
                     Entity uzumaki = uzumakiType.create(serverLevel);
                     if (uzumaki != null) {
                        uzumaki.moveTo(xPos, yPos, zPos);
                        uzumaki.setYRot(entity.getYRot());
                        uzumaki.setXRot(entity.getXRot());

                        setRangedAmmo(entity, uzumaki);

                        if (uzumaki instanceof LivingEntity livingUzumaki) {
                           if (livingUzumaki.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
                              livingUzumaki.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                           }
                           livingUzumaki.setHealth((float) hp);

                           if (livingUzumaki.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                              double baseAtk = livingUzumaki.getAttributeValue(Attributes.ATTACK_DAMAGE);
                              livingUzumaki.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAtk + entity.getPersistentData().getDouble("cnt6"));
                           }
                        }

                        serverLevel.addFreshEntity(uzumaki);
                     }
                  }
               }

               CsmSync.sendRoster(player);
            }
         }
      }

      // Cleanup skill state after animation limit reached
      double cnt1 = entity.getPersistentData().getDouble("cnt1");
      if (cnt1 > 100.0D) {
         entity.getPersistentData().putDouble("skill", 0.0D);
      }

      ci.cancel();
   }

   /**
    * Reflectively drains player's Cursed Energy (PlayerCursePowerChange variable).
    */
   private static void drainCursedEnergy(ServerPlayer player, double amount) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);

         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         player.getCapability(cap, null).ifPresent(vars -> {
            try {
               Field chgField = vars.getClass().getDeclaredField("PlayerCursePowerChange");
               chgField.setAccessible(true);
               double cur = (Double) chgField.get(vars);
               chgField.set(vars, cur - amount);
               vars.getClass().getMethod("syncPlayerVariables", Entity.class).invoke(vars, player);
            } catch (Exception ignored) {
            }
         });
      } catch (Exception ignored) {
      }
   }

   /**
    * Reflectively applies standard Jujutsucraft Mob Effect statuses to target entity.
    */
   private static void applyJjcEffect(LivingEntity living, String effectFieldName, int duration, int amplifier) {
      try {
         Class<?> modEffects = Class.forName("net.mcreator.jujutsucraft.init.JujutsucraftModMobEffects");
         Field field = modEffects.getField(effectFieldName);
         field.setAccessible(true);
         Object holder = field.get(null);
         MobEffect effect = (MobEffect) ((RegistryObject<?>) holder).get();
         living.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
      } catch (Exception ignored) {
      }
   }

   /**
    * Registers entity relationship between projectile and owner.
    */
   private static void setRangedAmmo(Entity owner, Entity projectile) {
      try {
         Class<?> proc = Class.forName("net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure");
         proc.getMethod("execute", Entity.class, Entity.class).invoke(null, owner, projectile);
      } catch (Exception ignored) {
      }
   }

   /**
    * Checks if player has Technique ID 18.0 (CSM / Geto).
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

         Field ct = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct.setAccessible(true);
         ct2.setAccessible(true);

         return (Double) ct.get(vars) == 18.0D || (Double) ct2.get(vars) == 18.0D;
      } catch (Exception e) {
         return false;
      }
   }
}