package com.ghosts.abyss.network;

import com.ghosts.abyss.csm.ClientCsmCache;
import com.ghosts.abyss.csm.CsmSpiritData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class PacketSyncCsmRoster {
   private final List<CsmSpiritData> spirits;
   private final String selectedUuid;
   private final int selectedAbility;
   private final int selectedRosterIdx;
   private final List<String[]> deployed;

   public PacketSyncCsmRoster(List<CsmSpiritData> spirits, String selectedUuid, int selectedAbility, int selectedRosterIdx, List<String[]> deployed) {
      this.spirits = spirits;
      this.selectedUuid = selectedUuid;
      this.selectedAbility = selectedAbility;
      this.selectedRosterIdx = selectedRosterIdx;
      this.deployed = deployed;
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.spirits.size());

      for(CsmSpiritData d : this.spirits) {
         buf.writeNbt(d.toNbt());
      }

      buf.writeUtf(this.selectedUuid == null ? "" : this.selectedUuid);
      buf.writeInt(this.selectedAbility);
      buf.writeInt(this.selectedRosterIdx);
      buf.writeInt(this.deployed.size());

      for(String[] d : this.deployed) {
         buf.writeUtf(d[0]);
         buf.writeUtf(d[1]);
         buf.writeUtf(d[2]);
      }
   }

   public static PacketSyncCsmRoster decode(FriendlyByteBuf buf) {
      int count = buf.readInt();
      List<CsmSpiritData> spirits = new ArrayList(count);

      for(int i = 0; i < count; ++i) {
         CompoundTag tag = buf.readNbt();
         if (tag != null) {
            spirits.add(CsmSpiritData.fromNbt(tag));
         }
      }

      String selectedUuid = buf.readUtf();
      int selectedAbility = buf.readInt();
      int selectedRosterIdx = buf.readInt();
      int dc = buf.readInt();
      List<String[]> deployed = new ArrayList(dc);

      for(int i = 0; i < dc; ++i) {
         deployed.add(new String[]{buf.readUtf(), buf.readUtf(), buf.readUtf()});
      }

      return new PacketSyncCsmRoster(spirits, selectedUuid, selectedAbility, selectedRosterIdx, deployed);
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCsmCache.update(this.spirits, this.selectedUuid, this.selectedAbility, this.selectedRosterIdx, this.deployed)));
      ctx.setPacketHandled(true);
   }
}