package com.ghosts.abyss.network;

import com.ghosts.abyss.ClientAbilityCache;
import com.ghosts.abyss.ScannedAbility;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncAbilities {
   private final List<ScannedAbility> abilities;

   public PacketSyncAbilities(List<ScannedAbility> abilities) {
      this.abilities = abilities;
   }

   public static PacketSyncAbilities decode(FriendlyByteBuf buf) {
      int size = buf.readVarInt();
      List<ScannedAbility> list = new ArrayList<>(size);

      for (int i = 0; i < size; ++i) {
         int ct = buf.readInt();
         int select = buf.readInt();
         String name = buf.readUtf();
         double cost = buf.readDouble();
         boolean physical = buf.readBoolean();
         boolean passive = buf.readBoolean();
         boolean available = buf.readBoolean();
         list.add(new ScannedAbility(ct, select, name, cost, physical, passive, available));
      }

      return new PacketSyncAbilities(list);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.abilities.size());

      for (ScannedAbility ab : this.abilities) {
         buf.writeInt(ab.ct());
         buf.writeInt(ab.select());
         buf.writeUtf(ab.name());
         buf.writeDouble(ab.cost());
         buf.writeBoolean(ab.physical());
         buf.writeBoolean(ab.passive());
         buf.writeBoolean(ab.available());
      }
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context context = ctx.get();
      context.enqueueWork(() -> ClientAbilityCache.update(this.abilities));
      context.setPacketHandled(true);
   }
}