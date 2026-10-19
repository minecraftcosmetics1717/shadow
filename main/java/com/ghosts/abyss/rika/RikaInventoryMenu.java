package com.ghosts.abyss.rika;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RikaInventoryMenu extends AbstractContainerMenu {
   private final RikaInventory rikaInv;
   private final Player player;

   public RikaInventoryMenu(int containerId, Inventory playerInv, RikaInventory rikaInv) {
      super((MenuType<?>) RikaMenuTypes.RIKA_INVENTORY.get(), containerId);
      this.rikaInv = rikaInv;
      this.player = playerInv.player;

      // Rika Inventory Slots (0 - 26)
      for (int row = 0; row < 3; ++row) {
         for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(rikaInv, col + row * 9, 8 + col * 18, 18 + row * 18));
         }
      }

      // Player Main Inventory Slots (27 - 53)
      for (int row = 0; row < 3; ++row) {
         for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
         }
      }

      // Player Hotbar Slots (54 - 62)
      for (int col = 0; col < 9; ++col) {
         this.addSlot(new Slot(playerInv, col, 8 + col * 18, 143));
      }
   }

   public RikaInventoryMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
      this(containerId, playerInv, new RikaInventory());
   }

   @Override
   public boolean stillValid(Player player) {
      return true;
   }

   @Override
   public void removed(Player player) {
      super.removed(player);
      if (player instanceof ServerPlayer serverPlayer) {
         this.rikaInv.save(serverPlayer); // Save Rika's inventory upon closing
      }
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack result = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);

      if (slot != null && slot.hasItem()) {
         ItemStack slotStack = slot.getItem();
         result = slotStack.copy();

         // Shift-clicking from Rika Inventory -> Player Inventory
         if (slotIndex < 27) {
            if (!this.moveItemStackTo(slotStack, 54, 63, false) && !this.moveItemStackTo(slotStack, 27, 54, false)) {
               return ItemStack.EMPTY;
            }
         }
         // Shift-clicking from Player Inventory -> Rika Inventory
         else if (!this.moveItemStackTo(slotStack, 0, 27, false)) {
            return ItemStack.EMPTY;
         }

         if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return result;
   }
}