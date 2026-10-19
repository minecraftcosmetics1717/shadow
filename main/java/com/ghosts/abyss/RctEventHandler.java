package com.ghosts.abyss;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(
        modid = "abyss",
        bus = Bus.FORGE
)
public class RctEventHandler {
   private static final Map<UUID, Float> prevHealth = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> holdingRctOutput = new ConcurrentHashMap<>();

   public static void setHoldingRctOutput(ServerPlayer player, boolean holding) {
      if (holding) {
         if (!RctPoints.isOutputUnlocked(player)) {
            holdingRctOutput.remove(player.getUUID());
            return;
         }
         holdingRctOutput.put(player.getUUID(), true);
      } else {
         holdingRctOutput.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      prevHealth.remove(event.getEntity().getUUID());
      holdingRctOutput.remove(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
      prevHealth.put(event.getEntity().getUUID(), event.getEntity().getHealth());
   }

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase != Phase.END) return;
      Player playerEntity = event.player;
      if (playerEntity.level().isClientSide()) return;
      if (!(playerEntity instanceof ServerPlayer player)) return;

      if (!AbyssGamerules.isEnabled(player, AbyssGamerules.REVERSE_CURSED_TECHNIQUE)) return;

      // --- Track healing for EXP ---
      float currentHealth = player.getHealth();
      float oldHealth = prevHealth.getOrDefault(player.getUUID(), currentHealth);
      if (currentHealth > oldHealth) {
         RctPoints.addHealing(player, currentHealth - oldHealth);
      }
      prevHealth.put(player.getUUID(), currentHealth);

      MobEffect rctFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "reverse_cursed_technique"));
      MobEffect fatigueFx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", "fatigue"));

      // --- RCT Efficiency Bonus Healing ---
      if (rctFx != null && player.hasEffect(rctFx)) {
         float multiplier = RctPoints.getEfficiencyMultiplier(player);
         if (multiplier > 1.0F) {
            MobEffectInstance inst = player.getEffect(rctFx);
            int rctLevel = inst != null ? inst.getAmplifier() : 0;
            float amount = (1.0F + (float) rctLevel) * 7.5F;
            float baseHealPerTick = player.getMaxHealth() / 160.0F * amount * 0.1F;

            if (fatigueFx != null && player.hasEffect(fatigueFx)) {
               MobEffectInstance fatigueInst = player.getEffect(fatigueFx);
               if (fatigueInst != null) {
                  int fatigueTicks = fatigueInst.getDuration();
                  double fatigueDivisor = 1.0 + Math.min(Math.max(fatigueTicks, 0) / 600.0, 10.0) * 0.4;
                  baseHealPerTick /= (float) fatigueDivisor;
               }
            }

            float bonusHeal = baseHealPerTick * (multiplier - 1.0F);
            player.heal(bonusHeal);
         }
      }

      // --- Proficiency fatigue reduction ---
      float divisor = RctPoints.getProficiencyDivisor(player);
      if (divisor > 1.0F && rctFx != null && player.hasEffect(rctFx)
              && fatigueFx != null && player.hasEffect(fatigueFx)
              && player.getHealth() < player.getMaxHealth()) {

         MobEffectInstance inst = player.getEffect(fatigueFx);
         if (inst != null) {
            int reduction = Math.round((divisor - 1.0F) / divisor * 20.0F);
            if (reduction > 0) {
               int newDuration = inst.getDuration() - reduction;
               player.removeEffectNoUpdate(fatigueFx);
               if (newDuration > 0) {
                  player.addEffect(new MobEffectInstance(fatigueFx, newDuration, inst.getAmplifier(),
                          inst.isAmbient(), inst.isVisible(), inst.showIcon()));
               }
            }
         }
      }

      // --- RCT Output (hold key) ---
      if (holdingRctOutput.getOrDefault(player.getUUID(), false)) {
         if (!RctPoints.isOutputUnlocked(player)) {
            holdingRctOutput.remove(player.getUUID());
            return;
         }

         player.getCapability(net.mcreator.jujutsucraft.network.JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
            boolean hasSixEyes = vars.FlagSixEyes;
            boolean hasSukuna = vars.flag_sukuna;

            double baseCost = 20.0;
            if (hasSixEyes) baseCost *= 0.10;
            if (hasSukuna)  baseCost *= 0.50;

            if (vars.PlayerCursePower < baseCost) {
               holdingRctOutput.remove(player.getUUID());
               return;
            }

            vars.PlayerCursePower -= baseCost;
            vars.syncPlayerVariables(player);

            if (rctFx == null) return;

            AABB area = player.getBoundingBox().inflate(4.0);
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area);

            for (LivingEntity target : targets) {
               if (target == player) continue; // never apply to self
               target.setLastHurtByPlayer(player);
               target.addEffect(new MobEffectInstance(rctFx, 2, 1, false, true));
            }
         });
      }
   }
}