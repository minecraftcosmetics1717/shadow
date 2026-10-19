package com.ghosts.abyss;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncOpenDomainData;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class OpenDomain {
   public static final String KEY_UNLOCKED = "open_domain_unlocked";
   public static final String KEY_TOGGLED = "open_domain_toggled";
   public static final String KEY_ACTIVE = "open_domain";
   private static final int COST_RP = 20;
   private static final int COST_ATP = 20;

   public static boolean canUnlock(ServerPlayer player) {
      if (player.getPersistentData().getBoolean("open_domain_unlocked")) {
         return false;
      }
      if (!player.getPersistentData().getBoolean("witnessed_open_domain")) {
         return false;
      }
      if (!hasAdvancement(player, "jujutsucraft:sorcerer_grade_special")) {
         return false;
      }
      if (RefinementPoints.getRefinement(player) < 50) {
         return false;
      }
      if (RefinementPoints.getAvailable(player) < 20) {
         return false;
      }
      return AdvTechPoints.getAvailable(player) >= 20;
   }

   public static void tryUnlock(ServerPlayer player) {
      if (!canUnlock(player)) {
         player.displayClientMessage(Component.translatable("message.abyss.open_domain_cannot_unlock"), true);
         return;
      }

      player.getPersistentData().putDouble("refinement_points_available", RefinementPoints.getAvailable(player) - 20);
      player.getPersistentData().putDouble("advtech_points_available", AdvTechPoints.getAvailable(player) - 20);
      player.getPersistentData().putBoolean("open_domain_unlocked", true);
      player.getPersistentData().putBoolean("open_domain_toggled", true);
      player.getPersistentData().putBoolean("open_domain", true);

      player.displayClientMessage(Component.translatable("message.abyss.open_domain_unlocked"), true);
      Abyss.LOGGER.debug("{} has unlocked Open Domain.", player.getName().getString());

      RefinementPoints.sync(player);
      AdvTechPoints.sync(player);
      sync(player);
   }

   public static void toggle(ServerPlayer player) {
      if (!player.getPersistentData().getBoolean("open_domain_unlocked")) return;

      boolean current = player.getPersistentData().getBoolean("open_domain_toggled");
      boolean next = !current;

      player.getPersistentData().putBoolean("open_domain_toggled", next);
      player.getPersistentData().putBoolean("open_domain", next);

      player.displayClientMessage(
              Component.translatable(next ? "message.abyss.open_domain_enabled" : "message.abyss.open_domain_disabled"),
              true
      );
      sync(player);
   }

   public static void sync(ServerPlayer player) {
      ModNetwork.CHANNEL.send(
              PacketDistributor.PLAYER.with(() -> player),
              new PacketSyncOpenDomainData(
                      player.getPersistentData().getBoolean("open_domain_unlocked"),
                      player.getPersistentData().getBoolean("open_domain_toggled"),
                      player.getPersistentData().getBoolean("witnessed_open_domain")
              )
      );
   }

   private static boolean hasAdvancement(ServerPlayer player, String id) {
      Advancement adv = player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
      if (adv == null) return false;
      return player.getAdvancements().getOrStartProgress(adv).isDone();
   }
}