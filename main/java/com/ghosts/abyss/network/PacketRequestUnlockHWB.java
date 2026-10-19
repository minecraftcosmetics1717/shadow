package com.ghosts.abyss.network;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.AbyssGamerules;
import java.util.function.Supplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketRequestUnlockHWB {

   public PacketRequestUnlockHWB() {
   }

   public PacketRequestUnlockHWB(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player == null) {
            return;
         }

         // Check if Hollow Wicker Basket is enabled in server gamerules
         if (!AbyssGamerules.isEnabled(player, AbyssGamerules.HOLLOW_WICKER_BASKET)) {
            player.sendSystemMessage(Component.literal("§cHollow Wicker Basket has been disabled by a server rule"), true);
            return;
         }

         CompoundTag persistentData = player.getPersistentData();

         // Check if player has already unlocked HWB
         if (!persistentData.getBoolean("unlocked_hwb")) {
            boolean hasSimpleDomain = this.hasAdvancement(player, "jujutsucraft:mastery_simple_domain");
            boolean hasGrade1 = this.hasAdvancement(player, "jujutsucraft:sorcerer_grade_1");
            boolean hasWitnessed = persistentData.getBoolean("witnessed_hwb");

            // Verify prerequisite requirements
            if (hasSimpleDomain && hasGrade1 && hasWitnessed) {
               int availablePoints = AdvTechPoints.getAvailable(player);

               if (availablePoints < 5) {
                  player.sendSystemMessage(Component.translatable("message.abyss.advtech_no_points"), true);
               } else {
                  // Deduct 5 points and unlock HWB
                  persistentData.putInt("advtech_points_available", availablePoints - 5);
                  persistentData.putBoolean("unlocked_hwb", true);

                  AdvTechPoints.sync(player);
                  player.sendSystemMessage(Component.translatable("message.abyss.hwb_unlocked"), true);
               }
            } else {
               player.sendSystemMessage(Component.translatable("message.abyss.hwb_requirements_not_met"), true);
            }
         }
      });
      ctx.setPacketHandled(true);
   }

   private boolean hasAdvancement(ServerPlayer player, String id) {
      if (player.getServer() == null) {
         return false;
      }

      Advancement advancement = player.getServer().getAdvancements().getAdvancement(new ResourceLocation(id));
      if (advancement == null) {
         return false;
      }

      return player.getAdvancements().getOrStartProgress(advancement).isDone();
   }
}