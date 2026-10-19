package com.ghosts.abyss.network;

import com.ghosts.abyss.EventListeners;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketToggleFourArms {
   public static final String KEY_HIDE_FOUR_ARMS = "jslop_hide_four_arms";

   public PacketToggleFourArms() {
   }

   public PacketToggleFourArms(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            boolean current = player.getPersistentData().getBoolean("jslop_hide_four_arms");
            player.getPersistentData().putBoolean("jslop_hide_four_arms", !current);
            boolean perfectBody = player.getPersistentData().getBoolean("perfect_body");
            EventListeners.broadcastPerfectBodyToAll(player, perfectBody);
         }
      });
      ctx.setPacketHandled(true);
   }
}