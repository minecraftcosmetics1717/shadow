package com.ghosts.abyss.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncAdvTechData {
   private final int available;
   private final boolean hwbUnlocked;
   private final boolean hwbWitnessed;
   private final boolean perfectBody;
   private final boolean cotUnlocked;
   private final boolean vowPurchased;

   public PacketSyncAdvTechData(int available, boolean hwbUnlocked, boolean hwbWitnessed, boolean perfectBody, boolean cotUnlocked, boolean vowPurchased) {
      this.available = available;
      this.hwbUnlocked = hwbUnlocked;
      this.hwbWitnessed = hwbWitnessed;
      this.perfectBody = perfectBody;
      this.cotUnlocked = cotUnlocked;
      this.vowPurchased = vowPurchased;
   }

   public PacketSyncAdvTechData(FriendlyByteBuf buf) {
      this.available = buf.readInt();
      this.hwbUnlocked = buf.readBoolean();
      this.hwbWitnessed = buf.readBoolean();
      this.perfectBody = buf.readBoolean();
      this.cotUnlocked = buf.readBoolean();
      this.vowPurchased = buf.readBoolean();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.available);
      buf.writeBoolean(this.hwbUnlocked);
      buf.writeBoolean(this.hwbWitnessed);
      buf.writeBoolean(this.perfectBody);
      buf.writeBoolean(this.cotUnlocked);
      buf.writeBoolean(this.vowPurchased);
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
         player.getPersistentData().putInt("advtech_points_available", this.available);
         player.getPersistentData().putBoolean("unlocked_hwb", this.hwbUnlocked);
         player.getPersistentData().putBoolean("witnessed_hwb", this.hwbWitnessed);
         player.getPersistentData().putBoolean("perfect_body", this.perfectBody);
         player.getPersistentData().putBoolean("cursed_object_transform_unlocked", this.cotUnlocked);
         player.getPersistentData().putBoolean("binding_vow_purchased", this.vowPurchased);
      }
   }
}