package com.ghosts.abyss.network;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.ModItems;
import java.util.function.Supplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class PacketPurchaseBindingVow {

   public PacketPurchaseBindingVow() {
   }

   public PacketPurchaseBindingVow(FriendlyByteBuf buf) {
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

         // Check if player is already bound with Heavenly Restriction / Perfect Body
         if (player.getPersistentData().getBoolean("perfect_body")) {
            player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_already_bound"), true);
            return;
         }

         // Check if player already owns a Binding Vow item
         boolean alreadyHasVow = player.getInventory().contains(new ItemStack(ModItems.BINDING_VOW.get()));
         if (alreadyHasVow) {
            player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_already_owned"), true);
            return;
         }

         int availablePoints = AdvTechPoints.getAvailable(player);
         if (availablePoints < 20) {
            player.sendSystemMessage(Component.translatable("message.abyss.not_enough_advtech"), true);
            return;
         }

         if (player.getServer() == null) {
            return;
         }

         // Check if player is Grade 1 Sorcerer
         Advancement grade1Advancement = player.getServer().getAdvancements().getAdvancement(new ResourceLocation("jujutsucraft", "sorcerer_grade_1"));
         boolean isGrade1 = grade1Advancement != null && player.getAdvancements().getOrStartProgress(grade1Advancement).isDone();

         if (isGrade1) {
            // Deduct 20 Advanced Technique points and issue Binding Vow item
            player.getPersistentData().putInt("advtech_points_available", availablePoints - 20);
            player.getPersistentData().putBoolean("binding_vow_purchased", true);
            AdvTechPoints.sync(player);

            player.getInventory().add(new ItemStack(ModItems.BINDING_VOW.get()));
            player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_obtained"), true);

            Abyss.LOGGER.debug("{} purchased Binding Vow: Perfect Body.", player.getScoreboardName());
         } else {
            player.sendSystemMessage(Component.translatable("message.abyss.grade_required"), true);
         }
      });

      ctx.setPacketHandled(true);
   }
}