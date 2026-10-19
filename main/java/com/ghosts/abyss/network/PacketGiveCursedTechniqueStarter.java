package com.ghosts.abyss.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketGiveCursedTechniqueStarter {
   public PacketGiveCursedTechniqueStarter() {
   }

   public PacketGiveCursedTechniqueStarter(FriendlyByteBuf buf) {
   }

   public static void encode(PacketGiveCursedTechniqueStarter msg, FriendlyByteBuf buf) {
   }

   public static void handle(PacketGiveCursedTechniqueStarter msg, Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("jujutsucraft", "cursed_technique_starter"));
            if (item != null) {
               player.getInventory().add(new ItemStack(item));
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}