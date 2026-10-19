package com.ghosts.abyss.network;

import com.ghosts.abyss.BlackFlashPoints;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncBlackFlashData {
   private final int exp;
   private final int chanceTier;
   private final int fatigueTier;
   private final int energyTier;
   private final int brainTier;
   private final boolean atWillUnlocked;
   private final boolean atWillCharged;
   private final long atWillCooldownEnd;

   public PacketSyncBlackFlashData(int exp, int chanceTier, int fatigueTier, int energyTier, int brainTier, boolean atWillUnlocked, boolean atWillCharged, long atWillCooldownEnd) {
      this.exp = exp;
      this.chanceTier = chanceTier;
      this.fatigueTier = fatigueTier;
      this.energyTier = energyTier;
      this.brainTier = brainTier;
      this.atWillUnlocked = atWillUnlocked;
      this.atWillCharged = atWillCharged;
      this.atWillCooldownEnd = atWillCooldownEnd;
   }

   public static PacketSyncBlackFlashData decode(FriendlyByteBuf buf) {
      return new PacketSyncBlackFlashData(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readLong());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeInt(this.exp);
      buf.writeInt(this.chanceTier);
      buf.writeInt(this.fatigueTier);
      buf.writeInt(this.energyTier);
      buf.writeInt(this.brainTier);
      buf.writeBoolean(this.atWillUnlocked);
      buf.writeBoolean(this.atWillCharged);
      buf.writeLong(this.atWillCooldownEnd);
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      ((NetworkEvent.Context)ctx.get()).enqueueWork(() -> BlackFlashPoints.setClientData(this.exp, this.chanceTier, this.fatigueTier, this.energyTier, this.brainTier, this.atWillUnlocked, this.atWillCharged, this.atWillCooldownEnd));
      ((NetworkEvent.Context)ctx.get()).setPacketHandled(true);
   }
}
