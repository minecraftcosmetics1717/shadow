package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.ClientCsmCache;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class PacketSyncCsmCooldown {
   private final long expiresAtMs;

   public PacketSyncCsmCooldown(long expiresAtMs) {
      this.expiresAtMs = expiresAtMs;
   }

   public PacketSyncCsmCooldown(FriendlyByteBuf buf) {
      this.expiresAtMs = buf.readLong();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeLong(this.expiresAtMs);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = (NetworkEvent.Context)ctxSupplier.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCsmCache.setCooldownExpiresAt(this.expiresAtMs)));
      ctx.setPacketHandled(true);
   }
}
