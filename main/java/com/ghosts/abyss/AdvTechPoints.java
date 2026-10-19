package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncAdvTechData;
import java.lang.reflect.Field;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.network.PacketDistributor;

public class AdvTechPoints {
   public static final String KEY_AVAILABLE = "advtech_points_available";
   public static final String KEY_LAST_FAME = "advtech_last_known_fame";
   public static final String KEY_UNLOCKED_HWB = "unlocked_hwb";
   public static final int MAX_AVAILABLE = Integer.MAX_VALUE;
   public static final double FAME_PER_POINT = 1000.0;

   public static void onTick(ServerPlayer player) {
      double currentFame = getPlayerFame(player);
      CompoundTag data = player.getPersistentData();
      double lastFame = data.getDouble("advtech_last_known_fame");

      long currentThreshold = (long) (currentFame / 1000.0);
      long lastThreshold = (long) (lastFame / 1000.0);

      if (currentThreshold > lastThreshold) {
         int pointsToGrant = (int) (currentThreshold - lastThreshold);
         grantPoints(player, pointsToGrant);
      }

      data.putDouble("advtech_last_known_fame", currentFame);
   }

   public static void grantPoints(ServerPlayer player, int amount) {
      CompoundTag data = player.getPersistentData();
      int current = (int) data.getDouble("advtech_points_available");
      int newTotal = current + amount;
      data.putDouble("advtech_points_available", (double) newTotal);

      player.displayClientMessage(
              Component.translatable("message.abyss.advtech_point_earned", newTotal),
              true
      );

      Abyss.LOGGER.debug("Granted {} advtech point(s) to {}. Available: {}",
              amount, player.getName().getString(), newTotal);
      sync(player);
   }

   public static boolean tryUnlock(ServerPlayer player, String techniqueKey) {
      CompoundTag data = player.getPersistentData();

      if (data.getBoolean(techniqueKey)) {
         return true; // already unlocked
      }

      int available = (int) data.getDouble("advtech_points_available");
      if (available < 1) {
         return false;
      }

      data.putDouble("advtech_points_available", (double) (available - 1));
      data.putBoolean(techniqueKey, true);

      Abyss.LOGGER.debug("{} unlocked technique: {}",
              player.getName().getString(), techniqueKey);
      sync(player);
      return true;
   }

   public static double getPlayerFame(Player player) {
      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);
         Capability<Object> cap = (Capability) capField.get(null);

         Object vars = player.getCapability(cap, null).orElse(null);
         if (vars == null) {
            return 0.0;
         }

         Field fameField = vars.getClass().getDeclaredField("PlayerFame");
         fameField.setAccessible(true);
         return (Double) fameField.get(vars);
      } catch (Exception e) {
         Abyss.LOGGER.warn("Could not read PlayerFame via reflection: {}", e.getMessage());
         return 0.0;
      }
   }

   public static int getAvailable(Player player) {
      return (int) player.getPersistentData().getDouble("advtech_points_available");
   }

   public static boolean isUnlocked(Player player, String techniqueKey) {
      return player.getPersistentData().getBoolean(techniqueKey);
   }

   public static void sync(ServerPlayer player) {
      ModNetwork.CHANNEL.send(
              PacketDistributor.PLAYER.with(() -> player),
              new PacketSyncAdvTechData(
                      getAvailable(player),
                      player.getPersistentData().getBoolean("unlocked_hwb"),
                      player.getPersistentData().getBoolean("witnessed_hwb"),
                      player.getPersistentData().getBoolean("perfect_body"),
                      player.getPersistentData().getBoolean("cursed_object_transform_unlocked"),
                      player.getPersistentData().getBoolean("binding_vow_purchased")
              )
      );
   }
}