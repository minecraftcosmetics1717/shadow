package com.ghosts.abyss.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncRefinementData {
   private final int available;
   private final int refinement;

   public PacketSyncRefinementData(int available, int refinement) {
      this.available = available;
      this.refinement = refinement;
   }

   public PacketSyncRefinementData(FriendlyByteBuf buf) {
      this.available = buf.readInt();
      this.refinement = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.available);
      buf.writeInt(this.refinement);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient));
      ctx.setPacketHandled(true);
   }

   @OnlyIn(Dist.CLIENT)
   private void handleClient() {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         player.getPersistentData().putDouble("refinement_points_available", (double) this.available);
         player.getPersistentData().putDouble("domain_refinement", (double) this.refinement);
      }
   }
}