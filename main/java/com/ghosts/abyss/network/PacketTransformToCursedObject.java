package com.ghosts.abyss.network;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.item.CursedFingerItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class PacketTransformToCursedObject {
   public PacketTransformToCursedObject() {
   }

   public PacketTransformToCursedObject(FriendlyByteBuf buf) {
   }

   public static void encode(PacketTransformToCursedObject msg, FriendlyByteBuf buf) {
   }

   public static void handle(PacketTransformToCursedObject msg, Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (!AbyssGamerules.isEnabled(player, AbyssGamerules.CURSED_OBJECT_TRANSFORMATION)) {
               player.sendSystemMessage(Component.literal("§cCursed Object Transformation has been disabled by a server rule"));
            } else if (!player.getPersistentData().getBoolean("cursed_object_transform_unlocked")) {
               player.sendSystemMessage(Component.literal("§cYou have not undergone the transformation"));
            } else {
               ItemStack finger = CursedFingerItem.createFor(player);
               if (!player.getInventory().add(finger)) {
                  player.drop(finger, false);
               }

               player.sendSystemMessage(Component.literal("§dYou have shed your form. Your cursed energy crystallises."));
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}