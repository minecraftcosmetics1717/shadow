package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncBlackFlashData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class BlackFlashPoints {
   public static final String KEY_EXP = "bf_exp";
   public static final String KEY_CHANCE_TIER = "bf_chance_tier";
   public static final String KEY_FATIGUE_TIER = "bf_fatigue_tier";
   public static final String KEY_ENERGY_TIER = "bf_energy_tier";
   public static final String KEY_BRAIN_TIER = "bf_brain_tier";
   public static final String KEY_AT_WILL_UNLOCKED = "bf_at_will_unlocked";
   public static final String KEY_AT_WILL_CHARGED = "bf_at_will_charged";
   public static final String KEY_AT_WILL_COOLDOWN_END = "bf_at_will_cooldown_end";

   public static final int MAX_CHANCE_TIER = 5;
   public static final int MAX_FATIGUE_TIER = 4;
   public static final int MAX_ENERGY_TIER = 3;
   public static final int MAX_BRAIN_TIER = 3;
   public static final int TIER_COST = 5;
   public static final int CAPSTONE_COST = 20;
   public static final long AT_WILL_COOLDOWN = 1200L;

   private static int clientExp = 0;
   private static int clientChanceTier = 0;
   private static int clientFatigueTier = 0;
   private static int clientEnergyTier = 0;
   private static int clientBrainTier = 0;
   private static boolean clientAtWillUnlocked = false;
   private static boolean clientAtWillCharged = false;
   private static long clientAtWillCooldownEnd = 0L;

   public static int getExp(ServerPlayer p) {
      return p.getPersistentData().getInt("bf_exp");
   }

   public static int getChanceTier(ServerPlayer p) {
      return p.getPersistentData().getInt("bf_chance_tier");
   }

   public static int getFatigueTier(ServerPlayer p) {
      return p.getPersistentData().getInt("bf_fatigue_tier");
   }

   public static int getEnergyTier(ServerPlayer p) {
      return p.getPersistentData().getInt("bf_energy_tier");
   }

   public static int getBrainTier(ServerPlayer p) {
      return p.getPersistentData().getInt("bf_brain_tier");
   }

   public static boolean isAtWillUnlocked(ServerPlayer p) {
      return p.getPersistentData().getBoolean("bf_at_will_unlocked");
   }

   public static boolean isAtWillCharged(ServerPlayer p) {
      return p.getPersistentData().getBoolean("bf_at_will_charged");
   }

   public static long getAtWillCooldownEnd(ServerPlayer p) {
      return p.getPersistentData().getLong("bf_at_will_cooldown_end");
   }

   public static int getBonusRolls(ServerPlayer p) {
      return getChanceTier(p);
   }

   public static int getBonusEnergy(ServerPlayer p) {
      return getEnergyTier(p) * 150;
   }

   public static int getBonusFatigueTicks(ServerPlayer p) {
      return getFatigueTier(p) * 600;
   }

   public static int getBonusBrainTicks(ServerPlayer p) {
      return switch (getBrainTier(p)) {
         case 1 -> 300;
         case 2 -> 600;
         case 3 -> 1200;
         default -> 0;
      };
   }

   public static void onBlackFlash(ServerPlayer p) {
      p.getPersistentData().putInt("bf_exp", getExp(p) + 1);
   }

   public static void applyFatigueBrainBonus(ServerPlayer p) {
      // Prevent duplicate execution within the same tick
      long currentTick = p.level().getGameTime();
      if (p.getPersistentData().getLong("bf_last_bonus_tick") == currentTick) {
         return;
      }
      p.getPersistentData().putLong("bf_last_bonus_tick", currentTick);

      int fatBonus = getBonusFatigueTicks(p);
      int brainBonus = getBonusBrainTicks(p);

      if (fatBonus > 0) {
         reduceEffect(p, "fatigue", fatBonus);
      }
      if (brainBonus > 0) {
         reduceEffect(p, "brain_damage", brainBonus);
      }
   }

   private static void reduceEffect(ServerPlayer p, String id, int ticks) {
      MobEffect fx = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("jujutsucraft", id));
      if (fx != null && p.hasEffect(fx)) {
         MobEffectInstance inst = p.getEffect(fx);
         if (inst != null) {
            int newDur = Math.max(0, inst.getDuration() - ticks);
            p.removeEffect(fx);
            if (newDur > 0) {
               p.addEffect(new MobEffectInstance(fx, newDur, inst.getAmplifier(), inst.isAmbient(), inst.isVisible()));
            }
         }
      }
   }

   public static boolean canActivateAtWill(ServerPlayer p) {
      if (!isAtWillUnlocked(p)) return false;
      if (isAtWillCharged(p)) return false;
      return p.level().getGameTime() >= getAtWillCooldownEnd(p);
   }

   public static boolean activateAtWill(ServerPlayer p) {
      if (!canActivateAtWill(p)) return false;
      p.getPersistentData().putBoolean("bf_at_will_charged", true);
      return true;
   }

   public static void consumeAtWillCharge(ServerPlayer p) {
      p.getPersistentData().putBoolean("bf_at_will_charged", false);
      p.getPersistentData().putLong("bf_at_will_cooldown_end", p.level().getGameTime() + 1200L);
   }

   public static boolean purchaseTier(ServerPlayer p, String key, int maxTier) {
      CompoundTag data = p.getPersistentData();
      int tier = data.getInt(key);
      int exp = data.getInt("bf_exp");

      if (tier < maxTier && exp >= 5) {
         data.putInt(key, tier + 1);
         data.putInt("bf_exp", exp - 5);
         return true;
      }
      return false;
   }

   public static boolean purchaseAtWill(ServerPlayer p) {
      CompoundTag data = p.getPersistentData();
      if (data.getBoolean("bf_at_will_unlocked")) return false;

      int exp = data.getInt("bf_exp");
      if (exp < 20) return false;

      data.putBoolean("bf_at_will_unlocked", true);
      data.putInt("bf_exp", exp - 20);
      return true;
   }

   public static void sync(ServerPlayer p) {
      CompoundTag d = p.getPersistentData();
      ModNetwork.CHANNEL.send(
              PacketDistributor.PLAYER.with(() -> p),
              new PacketSyncBlackFlashData(
                      d.getInt("bf_exp"),
                      d.getInt("bf_chance_tier"),
                      d.getInt("bf_fatigue_tier"),
                      d.getInt("bf_energy_tier"),
                      d.getInt("bf_brain_tier"),
                      d.getBoolean("bf_at_will_unlocked"),
                      d.getBoolean("bf_at_will_charged"),
                      d.getLong("bf_at_will_cooldown_end")
              )
      );
   }

   public static void setClientData(int exp, int chanceTier, int fatigueTier, int energyTier, int brainTier,
                                    boolean atWillUnlocked, boolean atWillCharged, long atWillCooldownEnd) {
      clientExp = exp;
      clientChanceTier = chanceTier;
      clientFatigueTier = fatigueTier;
      clientEnergyTier = energyTier;
      clientBrainTier = brainTier;
      clientAtWillUnlocked = atWillUnlocked;
      clientAtWillCharged = atWillCharged;
      clientAtWillCooldownEnd = atWillCooldownEnd;
   }

   public static int getClientExp() { return clientExp; }
   public static int getClientChanceTier() { return clientChanceTier; }
   public static int getClientFatigueTier() { return clientFatigueTier; }
   public static int getClientEnergyTier() { return clientEnergyTier; }
   public static int getClientBrainTier() { return clientBrainTier; }
   public static boolean isClientAtWillUnlocked() { return clientAtWillUnlocked; }
   public static boolean isClientAtWillCharged() { return clientAtWillCharged; }
   public static long getClientAtWillCooldownEnd() { return clientAtWillCooldownEnd; }
}