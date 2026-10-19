package com.ghosts.abyss.network;

import com.ghosts.abyss.AbilityScanHandler;
import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.YutaTechniqueRegistry;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketRenameYutaTechnique {
   private static final int MAX_NAME_LENGTH = 50;
   private final int index;
   private final String newName;

   public PacketRenameYutaTechnique(int index, String newName) {
      this.index = index;
      this.newName = newName;
   }

   public PacketRenameYutaTechnique(FriendlyByteBuf buf) {
      this.index = buf.readInt();
      this.newName = buf.readUtf(MAX_NAME_LENGTH);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.index);
      buf.writeUtf(this.newName);
   }

   public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
      NetworkEvent.Context ctx = ctxSupplier.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            if (YutaTechniqueRegistry.hasYutaCT(player)) {
               ListTag list = YutaTechniqueRegistry.getList(player);

               if (this.index >= 0 && this.index < list.size()) {
                  String trimmed = this.newName.strip();

                  if (!trimmed.isEmpty()) {
                     if (trimmed.length() > MAX_NAME_LENGTH) {
                        trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
                     }

                     CompoundTag entry = list.getCompound(this.index);
                     entry.putString("name", trimmed);

                     player.getPersistentData().put("jslop_yuta_registered", list);

                     PacketSyncYutaTechniques.sync(player);
                     AbilityScanHandler.scanAndSync(player);

                     Abyss.LOGGER.info(
                             "[JSlop] Yuta technique renamed: player={} idx={} name={}",
                             player.getScoreboardName(),
                             this.index,
                             trimmed
                     );
                  }
               }
            }
         }
      });
      ctx.setPacketHandled(true);
   }
}