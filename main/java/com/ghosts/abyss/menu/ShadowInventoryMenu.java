package com.ghosts.abyss.menu;

import com.ghosts.abyss.capability.ModCapabilities;
import com.ghosts.abyss.util.TenShadowsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ShadowInventoryMenu extends AbstractContainerMenu {

    private final ItemStackHandler shadowInv;
    private final int activeSlots; // how many the player is currently allowed to use

    // Client constructor
    public ShadowInventoryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, buf.readInt());
    }

    // Server constructor
    public ShadowInventoryMenu(int id, Inventory playerInv, int activeSlots) {
        super(ModMenus.SHADOW_INVENTORY.get(), id);
        this.activeSlots = activeSlots;

        Player player = playerInv.player;
        this.shadowInv = player.getCapability(ModCapabilities.SHADOW_INVENTORY)
                .map(cap -> cap.getInventory())
                .orElse(new ItemStackHandler(9));

        // ===== 3×3 Shadow grid (exactly like a dispenser) =====
        // Position matches vanilla dispenser
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new SlotItemHandler(shadowInv, index, 62 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Only allow interaction with unlocked slots
                        return index < activeSlots;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return index < activeSlots;
                    }
                });
            }
        }

        // ===== Player inventory =====
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // ===== Hotbar =====
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
                // From shadow → player inventory
                if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player → only into the currently unlocked shadow slots
                if (!this.moveItemStackTo(stack, 0, activeSlots, false)) {
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
        return TenShadowsHelper.isTenShadows(player);
    }
}