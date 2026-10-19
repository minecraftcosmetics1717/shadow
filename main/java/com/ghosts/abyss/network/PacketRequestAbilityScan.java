package com.ghosts.abyss.network;

import com.ghosts.abyss.AbilityScanHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketRequestAbilityScan {
   public static PacketRequestAbilityScan decode(FriendlyByteBuf buf) {
      return new PacketRequestAbilityScan();
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
      if (player != null) {
         ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> AbilityScanHandler.scanAndSync(player));
      }

      ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
   }
}
