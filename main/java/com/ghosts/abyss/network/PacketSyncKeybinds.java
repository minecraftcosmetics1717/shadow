package com.ghosts.abyss.network;

import com.ghosts.abyss.KeybindConfig;
import com.ghosts.abyss.KeybindConfig.Ability;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncKeybinds {
   private final KeybindConfig.Ability[] slots;

   public PacketSyncKeybinds(KeybindConfig.Ability[] slots) {
      this.slots = slots;
   }

   public static PacketSyncKeybinds decode(FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      KeybindConfig.Ability[] slots = new KeybindConfig.Ability[count];

      for(int i = 0; i < count; ++i) {
         int ct = buf.readInt();
         int sel = buf.readInt();
         String name = buf.readUtf();
         slots[i] = ct >= 0 && sel >= 0 ? new KeybindConfig.Ability(ct, sel, name) : Ability.UNBOUND;
      }

      return new PacketSyncKeybinds(slots);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.slots.length);

      for(KeybindConfig.Ability a : this.slots) {
         buf.writeInt(a.ct());
         buf.writeInt(a.selectValue());
         buf.writeUtf(a.displayName());
      }
   }

   public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
      NetworkEvent.Context ctx = contextSupplier.get();
      ctx.enqueueWork(() -> {
         for(int i = 0; i < this.slots.length && i < 8; ++i) {
            KeybindConfig.setSlot(i, this.slots[i]);
         }
      });
      ctx.setPacketHandled(true);
   }
}