package com.ghosts.abyss.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncOpenDomainData {
   private final boolean unlocked;
   private final boolean toggled;
   private final boolean witnessed;

   public PacketSyncOpenDomainData(boolean unlocked, boolean toggled, boolean witnessed) {
      this.unlocked = unlocked;
      this.toggled = toggled;
      this.witnessed = witnessed;
   }

   public PacketSyncOpenDomainData(FriendlyByteBuf buf) {
      this.unlocked = buf.readBoolean();
      this.toggled = buf.readBoolean();
      this.witnessed = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeBoolean(this.unlocked);
      buf.writeBoolean(this.toggled);
      buf.writeBoolean(this.witnessed);
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
         player.getPersistentData().putBoolean("open_domain_unlocked", this.unlocked);
         player.getPersistentData().putBoolean("open_domain_toggled", this.toggled);
         player.getPersistentData().putBoolean("open_domain", this.toggled);
         player.getPersistentData().putBoolean("witnessed_open_domain", this.witnessed);
      }
   }
}