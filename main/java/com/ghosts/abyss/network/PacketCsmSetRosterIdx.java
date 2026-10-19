package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.CsmRoster;
import com.ghosts.abyss.csm.CsmSpiritData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class PacketCsmSetRosterIdx {
   private final int idx;

   public PacketCsmSetRosterIdx(int idx) {
      this.idx = idx;
   }

   public PacketCsmSetRosterIdx(FriendlyByteBuf buf) {
      this.idx = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.idx);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = (NetworkEvent.Context)ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            List<CsmSpiritData> spirits = CsmRoster.getSpirits(player);
            if (this.idx >= 0 && this.idx < spirits.size() && ((CsmSpiritData)spirits.get(this.idx)).count > 0) {
               CsmRoster.setSelectedRosterIdx(player, this.idx);
               CsmSync.sendRoster(player);
            }

         }
      });
      ctx.setPacketHandled(true);
   }
}
