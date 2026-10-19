package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncRctData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class RctPoints {
   public static final String KEY_EXP = "rct_exp";
   public static final String KEY_EXP_ACCUMULATOR = "rct_exp_accumulator";
   public static final String KEY_EFFICIENCY_TIER = "rct_efficiency_tier";
   public static final String KEY_PROFICIENCY_TIER = "rct_proficiency_tier";
   public static final String KEY_OUTPUT_UNLOCKED = "rct_output_unlocked";

   public static final int MAX_EFFICIENCY_TIER = 5;
   public static final int MAX_PROFICIENCY_TIER = 4;
   public static final int COST_PER_TIER = 5;
   public static final int COST_OUTPUT = 20;

   private static int clientExp = 0;
   private static float clientExpAccumulator = 0.0F;
   private static int clientEfficiencyTier = 0;
   private static int clientProficiencyTier = 0;
   private static boolean clientOutputUnlocked = false;

   public static int getExp(ServerPlayer player) {
      return player.getPersistentData().getInt(KEY_EXP);
   }

   public static float getExpAccumulator(ServerPlayer player) {
      return player.getPersistentData().getFloat(KEY_EXP_ACCUMULATOR);
   }

   public static void addHealing(ServerPlayer player, float amount) {
      if (amount <= 0.0F) return;

      CompoundTag data = player.getPersistentData();
      float currentAcc = data.getFloat(KEY_EXP_ACCUMULATOR);
      float newAcc = currentAcc + amount;

      int currentExp = data.getInt(KEY_EXP);

      while (newAcc >= 100.0F) {
         newAcc -= 100.0F;
         currentExp++;
      }

      data.putFloat(KEY_EXP_ACCUMULATOR, newAcc);
      data.putInt(KEY_EXP, currentExp);
      sync(player);
   }

   public static int getEfficiencyTier(ServerPlayer player) {
      return player.getPersistentData().getInt(KEY_EFFICIENCY_TIER);
   }

   public static int getProficiencyTier(ServerPlayer player) {
      return player.getPersistentData().getInt(KEY_PROFICIENCY_TIER);
   }

   public static boolean isOutputUnlocked(ServerPlayer player) {
      return player.getPersistentData().getBoolean(KEY_OUTPUT_UNLOCKED);
   }

   public static boolean purchaseEfficiencyTier(ServerPlayer player) {
      int exp = getExp(player);
      int current = getEfficiencyTier(player);
      if (exp < COST_PER_TIER || current >= MAX_EFFICIENCY_TIER) return false;

      player.getPersistentData().putInt(KEY_EXP, exp - COST_PER_TIER);
      player.getPersistentData().putInt(KEY_EFFICIENCY_TIER, current + 1);
      return true;
   }

   public static boolean purchaseProficiencyTier(ServerPlayer player) {
      int exp = getExp(player);
      int current = getProficiencyTier(player);
      if (exp < COST_PER_TIER || current >= MAX_PROFICIENCY_TIER) return false;

      player.getPersistentData().putInt(KEY_EXP, exp - COST_PER_TIER);
      player.getPersistentData().putInt(KEY_PROFICIENCY_TIER, current + 1);
      return true;
   }

   public static boolean purchaseOutputUnlock(ServerPlayer player) {
      int exp = getExp(player);
      if (exp < COST_OUTPUT || isOutputUnlocked(player)) return false;

      player.getPersistentData().putInt(KEY_EXP, exp - COST_OUTPUT);
      player.getPersistentData().putBoolean(KEY_OUTPUT_UNLOCKED, true);
      return true;
   }

   public static void sync(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      ModNetwork.CHANNEL.send(
              PacketDistributor.PLAYER.with(() -> player),
              new PacketSyncRctData(
                      data.getInt(KEY_EXP),
                      data.getFloat(KEY_EXP_ACCUMULATOR),
                      data.getInt(KEY_EFFICIENCY_TIER),
                      data.getInt(KEY_PROFICIENCY_TIER),
                      data.getBoolean(KEY_OUTPUT_UNLOCKED)
              )
      );
   }

   // --- Client Data Methods ---

   public static void setClientData(int exp, float acc, int effTier, int profTier, boolean outputUnlocked) {
      clientExp = exp;
      clientExpAccumulator = acc;
      clientEfficiencyTier = effTier;
      clientProficiencyTier = profTier;
      clientOutputUnlocked = outputUnlocked;
   }

   public static int getClientExp() {
      return clientExp;
   }

   public static float getClientExpAccumulator() {
      return clientExpAccumulator;
   }

   public static int getClientEfficiencyTier() {
      return clientEfficiencyTier;
   }

   public static int getClientProficiencyTier() {
      return clientProficiencyTier;
   }

   public static boolean isClientOutputUnlocked() {
      return clientOutputUnlocked;
   }

   public static float getClientEfficiencyMultiplier() {
      return 1.0F + (float) clientEfficiencyTier * 0.1F;
   }

   public static float getClientProficiencyDivisor() {
      return 1.0F + (float) clientProficiencyTier * 0.25F;
   }

   public static float getEfficiencyMultiplier(ServerPlayer player) {
      return 1.0F + (float) getEfficiencyTier(player) * 0.1F;
   }

   public static float getProficiencyDivisor(ServerPlayer player) {
      return 1.0F + (float) getProficiencyTier(player) * 0.25F;
   }
}