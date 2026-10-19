package com.ghosts.abyss.network;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.YutaTechniqueRegistry;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketYutaDomainSelect {
   private final int idx;

   public PacketYutaDomainSelect(int idx) {
      this.idx = idx;
   }

   public PacketYutaDomainSelect(FriendlyByteBuf buf) {
      this.idx = buf.readInt();
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.idx);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player == null || !YutaTechniqueRegistry.hasYutaCT(player)) {
            return;
         }

         int count = YutaTechniqueRegistry.getCount(player);
         int clampedIdx = Math.min(this.idx, count - 1);

         YutaTechniqueRegistry.setDomainIdx(player, clampedIdx);
         PacketSyncYutaTechniques.sync(player);
         Abyss.LOGGER.debug("[JSlop] {} set Yuta domain idx to {}", player.getScoreboardName(), clampedIdx);
      });
      ctx.setPacketHandled(true);
   }
}