package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.CsmRoster;

import java.util.List;
import java.util.function.Supplier;

import com.ghosts.abyss.csm.CsmSpiritData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class PacketCsmToggleSpared {
   private final int slotIdx;

   public PacketCsmToggleSpared(int slotIdx) {
      this.slotIdx = slotIdx;
   }

   public PacketCsmToggleSpared(FriendlyByteBuf buf) {
      this.slotIdx = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.slotIdx);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            List<CsmSpiritData> spirits = CsmRoster.getSpirits(player);
            if (this.slotIdx >= 0 && this.slotIdx < spirits.size()) {
               CsmRoster.toggleSpared(player, this.slotIdx);
               CsmSync.sendRoster(player);
            }
         }
      });
      ctx.setPacketHandled(true);
   }}
