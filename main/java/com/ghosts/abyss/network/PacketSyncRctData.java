package com.ghosts.abyss.network;

import com.ghosts.abyss.RctPoints;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncRctData {
   private final int exp;
   private final float accumulator;
   private final int efficiencyTier;
   private final int proficiencyTier;
   private final boolean outputUnlocked;

   public PacketSyncRctData(int exp, float accumulator, int efficiencyTier, int proficiencyTier, boolean outputUnlocked) {
      this.exp = exp;
      this.accumulator = accumulator;
      this.efficiencyTier = efficiencyTier;
      this.proficiencyTier = proficiencyTier;
      this.outputUnlocked = outputUnlocked;
   }

   public static PacketSyncRctData decode(FriendlyByteBuf buf) {
      return new PacketSyncRctData(
              buf.readInt(),
              buf.readFloat(),
              buf.readInt(),
              buf.readInt(),
              buf.readBoolean()
      );
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.exp);
      buf.writeFloat(this.accumulator);
      buf.writeInt(this.efficiencyTier);
      buf.writeInt(this.proficiencyTier);
      buf.writeBoolean(this.outputUnlocked);
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ctx.get().enqueueWork(() -> RctPoints.setClientData(this.exp, this.accumulator, this.efficiencyTier, this.proficiencyTier, this.outputUnlocked));
      ctx.get().setPacketHandled(true);
   }
}