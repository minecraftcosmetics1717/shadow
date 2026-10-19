package com.ghosts.abyss.menu;

import com.ghosts.abyss.capability.ModCapabilities;
import com.ghosts.abyss.util.InnerselfHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class InnerselfInventoryMenu extends AbstractContainerMenu {

    private final ItemStackHandler inv;

    // Client constructor (still needs a buf for IForgeMenuType, even if unused)
    public InnerselfInventoryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv);
    }

    // Server constructor
    public InnerselfInventoryMenu(int id, Inventory playerInv) {
        super(ModMenus.INNERSELF_INVENTORY.get(), id);

        this.inv = playerInv.player.getCapability(ModCapabilities.INNERSELF_INVENTORY)
                .map(cap -> cap.getInventory())
                .orElse(new ItemStackHandler(9));

        // 3x3 grid — all 9 slots always usable
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new SlotItemHandler(inv, index, 62 + col * 18, 17 + row * 18));
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

            if (index < 9) {
                // From Innerself → player
                if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player → Innerself (all 9 slots)
                if (!this.moveItemStackTo(stack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return InnerselfHelper.isInnerselfUser(player);
    }
}