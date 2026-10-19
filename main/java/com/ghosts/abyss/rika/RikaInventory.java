package com.ghosts.abyss.rika;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class RikaInventory extends SimpleContainer {
   public static final String NBT_KEY = "jslop_rika_inv";
   public static final int SIZE = 27;

   public RikaInventory() {
      super(SIZE);
   }

   public static RikaInventory load(ServerPlayer player) {
      RikaInventory inv = new RikaInventory();
      CompoundTag data = player.getPersistentData();

      // Tag.TAG_LIST = 9
      if (data.contains(NBT_KEY, Tag.TAG_LIST)) {
         // Tag.TAG_COMPOUND = 10
         ListTag list = data.getList(NBT_KEY, Tag.TAG_COMPOUND);

         for (int i = 0; i < list.size(); ++i) {
            CompoundTag slotTag = list.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot < SIZE) {
               inv.setItem(slot, ItemStack.of(slotTag));
            }
         }
      }

      return inv;
   }

   public void save(ServerPlayer player) {
      ListTag list = new ListTag();

      for (int i = 0; i < SIZE; ++i) {
         ItemStack stack = this.getItem(i);
         if (!stack.isEmpty()) {
            CompoundTag slotTag = stack.save(new CompoundTag());
            slotTag.putByte("Slot", (byte) i);
            list.add(slotTag);
         }
      }

      player.getPersistentData().put(NBT_KEY, list);
   }
}