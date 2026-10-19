package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.CsmRoster;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class PacketCsmSelectSpirit {
   private final String deployedEntityUuid;

   public PacketCsmSelectSpirit(String deployedEntityUuid) {
      this.deployedEntityUuid = deployedEntityUuid;
   }

   public PacketCsmSelectSpirit(FriendlyByteBuf buf) {
      this.deployedEntityUuid = buf.readUtf();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeUtf(this.deployedEntityUuid == null ? "" : this.deployedEntityUuid);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            CsmRoster.setSelectedUuid(player, this.deployedEntityUuid);
            CsmSync.sendRoster(player);
         }
      });
      ctx.setPacketHandled(true);
   }
}