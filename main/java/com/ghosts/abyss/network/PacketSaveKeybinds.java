package com.ghosts.abyss.network;

import com.ghosts.abyss.KeybindConfig;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketSaveKeybinds {
   private final KeybindConfig.Ability[] slots;

   public PacketSaveKeybinds(KeybindConfig.Ability[] slots) {
      this.slots = slots;
   }

   public static PacketSaveKeybinds decode(FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      KeybindConfig.Ability[] slots = new KeybindConfig.Ability[count];

      for (int i = 0; i < count; ++i) {
         int ct = buf.readInt();
         int sel = buf.readInt();
         String name = buf.readUtf();
         slots[i] = (ct >= 0 && sel >= 0) ? new KeybindConfig.Ability(ct, sel, name) : KeybindConfig.Ability.UNBOUND;
      }

      return new PacketSaveKeybinds(slots);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.slots.length);

      for (KeybindConfig.Ability a : this.slots) {
         buf.writeInt(a.ct());
         buf.writeInt(a.selectValue());
         buf.writeUtf(a.displayName());
      }
   }

   public void handle(Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context context = ctx.get();
      ServerPlayer player = context.getSender();

      if (player != null) {
         context.enqueueWork(() -> {
            ListTag list = new ListTag();

            for (KeybindConfig.Ability a : this.slots) {
               CompoundTag tag = new CompoundTag();
               tag.putInt("ct", a.ct());
               tag.putInt("select", a.selectValue());
               tag.putString("name", a.displayName());
               list.add(tag);
            }

            player.getPersistentData().put("jujutsuslop_keybinds", list);
         });
      }

      context.setPacketHandled(true);
   }
}