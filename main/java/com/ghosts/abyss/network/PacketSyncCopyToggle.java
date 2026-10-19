package com.ghosts.abyss.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncCopyToggle {
   private final boolean disabled;

   public PacketSyncCopyToggle(boolean disabled) {
      this.disabled = disabled;
   }

   public static PacketSyncCopyToggle decode(FriendlyByteBuf buf) {
      return new PacketSyncCopyToggle(buf.readBoolean());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.disabled);
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
         player.getPersistentData().putBoolean("jslop_disable_copy", this.disabled);
      }
   }
}