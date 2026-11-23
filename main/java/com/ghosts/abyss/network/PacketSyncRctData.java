
package com.ghosts.abyss.network;

import com.ghosts.abyss.rct.RctPoints;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncRctData {

   private final int exp;
   private final float accumulator;
   private final int efficiencyTier;
   private final int proficiencyTier;
   private final boolean outputUnlocked;
   private final boolean burnoutUnlocked;

   public PacketSyncRctData(
           int exp,
           float accumulator,
           int efficiencyTier,
           int proficiencyTier,
           boolean outputUnlocked,
           boolean burnoutUnlocked
   ) {
      this.exp = exp;
      this.accumulator = accumulator;
      this.efficiencyTier = efficiencyTier;
      this.proficiencyTier = proficiencyTier;
      this.outputUnlocked = outputUnlocked;
      this.burnoutUnlocked = burnoutUnlocked;
   }

   public static PacketSyncRctData decode(FriendlyByteBuf buf) {
      return new PacketSyncRctData(
              buf.readInt(),
              buf.readFloat(),
              buf.readInt(),
              buf.readInt(),
              buf.readBoolean(),
              buf.readBoolean()
      );
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(exp);
      buf.writeFloat(accumulator);
      buf.writeInt(efficiencyTier);
      buf.writeInt(proficiencyTier);
      buf.writeBoolean(outputUnlocked);
      buf.writeBoolean(burnoutUnlocked);
   }

   public void handle(
           Supplier<NetworkEvent.Context> contextSupplier
   ) {
      NetworkEvent.Context ctx = contextSupplier.get();

      ctx.enqueueWork(() -> RctPoints.setClientData(
              exp,
              accumulator,
              efficiencyTier,
              proficiencyTier,
              outputUnlocked,
              burnoutUnlocked
      ));

      ctx.setPacketHandled(true);
   }
}
