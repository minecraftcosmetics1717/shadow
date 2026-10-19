package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncRefinementData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class RefinementPoints {
   public static final String KEY_AVAILABLE = "refinement_points_available";
   public static final String KEY_SPENT = "domain_refinement";
   public static final int MAX_AVAILABLE = Integer.MAX_VALUE;
   public static final int MAX_REFINEMENT = 50;

   public static void grantRefinementPoints(ServerPlayer player, int amount) {
      CompoundTag data = player.getPersistentData();
      int current = (int) data.getDouble(KEY_AVAILABLE);
      int newTotal = current + amount;

      data.putDouble(KEY_AVAILABLE, (double) newTotal);
      player.displayClientMessage(Component.translatable("message.abyss.point_earned", newTotal), true);
      Abyss.LOGGER.debug("Granted {} point(s) to {}. Available: {}", amount, player.getName().getString(), newTotal);

      sync(player);
   }

   public static void trySpendPoint(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      int available = (int) data.getDouble(KEY_AVAILABLE);
      int refinement = (int) data.getDouble(KEY_SPENT);

      if (available >= 1) {
         if (refinement >= MAX_REFINEMENT) {
            Abyss.LOGGER.debug("{} is at the refinement cap ({}) — point not spent.", player.getName().getString(), MAX_REFINEMENT);
         } else {
            data.putDouble(KEY_AVAILABLE, (double) (available - 1));
            data.putDouble(KEY_SPENT, (double) (refinement + 1));
            Abyss.LOGGER.debug("{} spent a point. Refinement: {}/{}, Available: {}", player.getName().getString(), refinement + 1, MAX_REFINEMENT, available - 1);

            sync(player);
         }
      }
   }

   public static void sync(ServerPlayer player) {
      ModNetwork.CHANNEL.send(
              PacketDistributor.PLAYER.with(() -> player),
              new PacketSyncRefinementData(getAvailable(player), getRefinement(player))
      );
   }

   public static int getAvailable(Player player) {
      return (int) player.getPersistentData().getDouble(KEY_AVAILABLE);
   }

   public static int getRefinement(Player player) {
      return (int) player.getPersistentData().getDouble(KEY_SPENT);
   }
}