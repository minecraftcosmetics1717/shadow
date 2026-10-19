package com.ghosts.abyss.network;

import com.ghosts.abyss.PerfectBodySync;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncPerfectBodyVisible {
   private final UUID playerUuid;
   private final boolean perfectBody;

   public PacketSyncPerfectBodyVisible(UUID playerUuid, boolean perfectBody) {
      this.playerUuid = playerUuid;
      this.perfectBody = perfectBody;
   }

   public static PacketSyncPerfectBodyVisible decode(FriendlyByteBuf buf) {
      return new PacketSyncPerfectBodyVisible(buf.readUUID(), buf.readBoolean());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeUUID(this.playerUuid);
      buf.writeBoolean(this.perfectBody);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> PerfectBodySync.MAP.put(this.playerUuid, this.perfectBody));
      ctx.setPacketHandled(true);
   }
}