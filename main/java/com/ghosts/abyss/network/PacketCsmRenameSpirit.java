package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.CsmRoster;

import java.util.List;
import java.util.function.Supplier;

import com.ghosts.abyss.csm.CsmSpiritData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class PacketCsmRenameSpirit {
   private static final int MAX_NAME_LENGTH = 64;
   private final int slotIdx;
   private final String newName;

   public PacketCsmRenameSpirit(int slotIdx, String newName) {
      this.slotIdx = slotIdx;
      this.newName = newName;
   }

   public PacketCsmRenameSpirit(FriendlyByteBuf buf) {
      this.slotIdx = buf.readInt();
      this.newName = buf.readUtf(MAX_NAME_LENGTH);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.slotIdx);
      buf.writeUtf(this.newName.length() > MAX_NAME_LENGTH ? this.newName.substring(0, MAX_NAME_LENGTH) : this.newName);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && !this.newName.isBlank()) {
            List<CsmSpiritData> spirits = CsmRoster.getSpirits(player);
            if (this.slotIdx >= 0 && this.slotIdx < spirits.size()) {
               CsmRoster.renameSpirit(player, this.slotIdx, this.newName.strip());
               CsmSync.sendRoster(player);
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}