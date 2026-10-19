package com.ghosts.abyss.menu;

import com.ghosts.abyss.capability.ModCapabilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class LifeCharmsMenu extends AbstractContainerMenu {

    private final ItemStackHandler charmsInv;

    public LifeCharmsMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv);
    }

    public LifeCharmsMenu(int id, Inventory playerInv) {
        super(ModMenus.LIFE_CHARMS.get(), id);

        this.charmsInv = playerInv.player.getCapability(ModCapabilities.LIFE_CHARMS)
                .map(cap -> cap.getInventory())
                .orElse(new ItemStackHandler(27));

        // ===== 3 rows of 9 (chest layout) =====
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new SlotItemHandler(charmsInv, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(Items.TOTEM_OF_UNDYING);
                    }
                });
            }
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!stack.is(Items.TOTEM_OF_UNDYING) || !this.moveItemStackTo(stack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}