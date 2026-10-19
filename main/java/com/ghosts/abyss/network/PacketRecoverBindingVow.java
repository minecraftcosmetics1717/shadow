package com.ghosts.abyss.network;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.ModItems;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class PacketRecoverBindingVow {
   public static final String KEY_PURCHASED = "binding_vow_purchased";

   public PacketRecoverBindingVow() {
   }

   public PacketRecoverBindingVow(FriendlyByteBuf buf) {
   }

   public static void encode(PacketRecoverBindingVow msg, FriendlyByteBuf buf) {
   }

   public static void handle(PacketRecoverBindingVow msg, Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (player.getPersistentData().getBoolean("perfect_body")) {
               player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_already_bound"), true);
            } else if (!player.getPersistentData().getBoolean("binding_vow_purchased")) {
               player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_not_purchased"), true);
            } else {
               boolean alreadyHasVow = player.getInventory().contains(new ItemStack(ModItems.BINDING_VOW.get()));

               if (alreadyHasVow) {
                  player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_already_owned"), true);
               } else {
                  player.getInventory().add(new ItemStack(ModItems.BINDING_VOW.get()));
                  player.sendSystemMessage(Component.translatable("message.abyss.binding_vow_recovered"), true);
                  Abyss.LOGGER.debug("{} recovered their Binding Vow.", player.getScoreboardName());
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}