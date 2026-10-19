package com.ghosts.abyss.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketToggleCopyOnHit {
   public static final String KEY_DISABLE_COPY = "jslop_disable_copy";

   public PacketToggleCopyOnHit() {
   }

   public PacketToggleCopyOnHit(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            boolean current = player.getPersistentData().getBoolean("jslop_disable_copy");
            boolean next = !current;
            player.getPersistentData().putBoolean("jslop_disable_copy", next);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketSyncCopyToggle(next));
         }
      });
      ctx.setPacketHandled(true);
   }
}