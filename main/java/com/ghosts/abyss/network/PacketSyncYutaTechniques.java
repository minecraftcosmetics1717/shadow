package com.ghosts.abyss.network;

import com.ghosts.abyss.ClientYutaTechniqueCache;
import com.ghosts.abyss.YutaTechniqueRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketSyncYutaTechniques {
   private final List<ClientYutaTechniqueCache.Entry> entries;
   private final int domainIdx;

   public PacketSyncYutaTechniques(List<ClientYutaTechniqueCache.Entry> entries, int domainIdx) {
      this.entries = entries;
      this.domainIdx = domainIdx;
   }

   public static PacketSyncYutaTechniques from(ServerPlayer player) {
      ListTag list = YutaTechniqueRegistry.getList(player);
      List<ClientYutaTechniqueCache.Entry> entries = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); ++i) {
         CompoundTag t = list.getCompound(i);
         entries.add(new ClientYutaTechniqueCache.Entry(
                 t.getDouble("skill"),
                 t.getDouble("cooldown_ticks"),
                 t.getString("name"),
                 t.getDouble("ce_cost")
         ));
      }

      return new PacketSyncYutaTechniques(entries, YutaTechniqueRegistry.getDomainIdx(player));
   }

   public static PacketSyncYutaTechniques decode(FriendlyByteBuf buf) {
      int size = buf.readVarInt();
      List<ClientYutaTechniqueCache.Entry> entries = new ArrayList<>(size);

      for (int i = 0; i < size; ++i) {
         double skill = buf.readDouble();
         double cooldown = buf.readDouble();
         String name = buf.readUtf();
         double ceCost = buf.readDouble();
         entries.add(new ClientYutaTechniqueCache.Entry(skill, cooldown, name, ceCost));
      }

      int domainIdx = buf.readInt();
      return new PacketSyncYutaTechniques(entries, domainIdx);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.entries.size());

      for (ClientYutaTechniqueCache.Entry e : this.entries) {
         buf.writeDouble(e.skill());
         buf.writeDouble(e.cooldownTicks());
         buf.writeUtf(e.name());
         buf.writeDouble(e.ceCost());
      }

      buf.writeInt(this.domainIdx);
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context context = ctx.get();
      context.enqueueWork(() -> ClientYutaTechniqueCache.update(this.entries, this.domainIdx));
      context.setPacketHandled(true);
   }

   public static void sync(ServerPlayer player) {
      ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), from(player));
   }
}