package com.ghosts.abyss.network;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.AbyssGamerules;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketUnlockCursedObjectTransformation {
   private static final int ATP_COST = 20;

   public PacketUnlockCursedObjectTransformation() {
   }

   public PacketUnlockCursedObjectTransformation(FriendlyByteBuf buf) {
   }

   public static void encode(PacketUnlockCursedObjectTransformation msg, FriendlyByteBuf buf) {
   }

   public static void handle(PacketUnlockCursedObjectTransformation msg, Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (!AbyssGamerules.isEnabled(player, AbyssGamerules.CURSED_OBJECT_TRANSFORMATION)) {
               player.sendSystemMessage(Component.literal("§cCursed Object Transformation has been disabled by a server rule"));
            } else {
               CompoundTag data = player.getPersistentData();
               if (!data.getBoolean("cursed_object_transform_unlocked")) {
                  int atp = AdvTechPoints.getAvailable(player);
                  if (atp < 20) {
                     player.sendSystemMessage(Component.literal("§cInsufficient Advanced Technique Points (Need 20)"));
                  } else {
                     data.putDouble("advtech_points_available", (double)(atp - 20));
                     data.putBoolean("cursed_object_transform_unlocked", true);
                     AdvTechPoints.sync(player);
                     player.sendSystemMessage(Component.literal("§dYour cursed energy has been crystallised. You may now shed your form."));
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}