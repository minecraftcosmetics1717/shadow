package com.ghosts.abyss.network;

import com.ghosts.abyss.RctEventHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketRctOutputHold {
   private final boolean holding;

   public PacketRctOutputHold(boolean holding) {
      this.holding = holding;
   }

   public static PacketRctOutputHold decode(FriendlyByteBuf buf) {
      return new PacketRctOutputHold(buf.readBoolean());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.holding);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ServerPlayer player = ctx.getSender();
      if (player == null) {
         ctx.setPacketHandled(true);
         return;
      }

      ctx.enqueueWork(() -> RctEventHandler.setHoldingRctOutput(player, this.holding));
      ctx.setPacketHandled(true);
   }
}