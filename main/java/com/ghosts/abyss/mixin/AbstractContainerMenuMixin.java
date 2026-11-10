package com.ghosts.abyss.mixin;

import com.ghosts.abyss.util.InventoryRestrictionHelper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    private static boolean abyss$isLocked(Slot slot) {
        if (slot.container instanceof Inventory inventory) {
            return !InventoryRestrictionHelper.isSlotAllowed(
                    inventory.player,
                    slot.getSlotIndex()
            );
        }

        return false;
    }

    @Inject(
            method = "clicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictClicks(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo ci
    ) {
        AbstractContainerMenu menu =
                (AbstractContainerMenu) (Object) this;

        // Block number-key swaps into locked hotbar positions,
        // even when the clicked slot belongs to a chest.
        if (clickType == ClickType.SWAP
                && button >= 0 && button < 9
                && !InventoryRestrictionHelper.isSlotAllowed(
                player, button)) {
            ci.cancel();
            return;
        }

        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }

        Slot slot = menu.slots.get(slotId);

        if (abyss$isLocked(slot)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "moveItemStackTo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictShiftTransfers(
            ItemStack stack,
            int startIndex,
            int endIndex,
            boolean reverse,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AbstractContainerMenu menu =
                (AbstractContainerMenu) (Object) this;

        if (stack.isEmpty()) {
            return;
        }

        if (startIndex < 0
                || endIndex > menu.slots.size()
                || startIndex >= endIndex) {
            return;
        }

        // Only replace transfers whose destination range
        // contains player-inventory slots.
        boolean hasPlayerInventory = false;

        for (int i = startIndex; i < endIndex; i++) {
            if (menu.slots.get(i).container instanceof Inventory) {
                hasPlayerInventory = true;
                break;
            }
        }

        if (!hasPlayerInventory) {
            return;
        }

        boolean moved = false;

        // Pass 1: merge into existing compatible stacks.
        int index = reverse ? endIndex - 1 : startIndex;

        while (!stack.isEmpty()
                && index >= startIndex
                && index < endIndex) {

            Slot slot = menu.slots.get(index);

            if (!abyss$isLocked(slot) && slot.mayPlace(stack)) {
                ItemStack existing = slot.getItem();

                if (!existing.isEmpty()
                        && ItemStack.isSameItemSameTags(
                        existing, stack)) {

                    int max = Math.min(
                            slot.getMaxStackSize(stack),
                            existing.getMaxStackSize()
                    );

                    int space = max - existing.getCount();

                    if (space > 0) {
                        int amount = Math.min(
                                space,
                                stack.getCount()
                        );

                        existing.grow(amount);
                        stack.shrink(amount);

                        slot.setChanged();
                        moved = true;
                    }
                }
            }

            index += reverse ? -1 : 1;
        }

        // Pass 2: fill empty permitted slots.
        index = reverse ? endIndex - 1 : startIndex;

        while (!stack.isEmpty()
                && index >= startIndex
                && index < endIndex) {

            Slot slot = menu.slots.get(index);

            if (!abyss$isLocked(slot)
                    && !slot.hasItem()
                    && slot.mayPlace(stack)) {

                int amount = Math.min(
                        stack.getCount(),
                        slot.getMaxStackSize(stack)
                );

                if (amount > 0) {
                    ItemStack placed = stack.copy();
                    placed.setCount(amount);

                    slot.set(placed);
                    stack.shrink(amount);

                    slot.setChanged();
                    moved = true;
                }
            }

            index += reverse ? -1 : 1;
        }

        cir.setReturnValue(moved);
    }
}