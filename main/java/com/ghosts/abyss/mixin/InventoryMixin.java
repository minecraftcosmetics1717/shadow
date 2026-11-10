package com.ghosts.abyss.mixin;

import com.ghosts.abyss.util.InventoryRestrictionHelper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow @Final
    public Player player;

    @Shadow
    public int selected;

    @Inject(method = "tick", at = @At("HEAD"))
    private void abyss$clampSelectedSlot(CallbackInfo ci) {
        int allowed =
                InventoryRestrictionHelper.getAllowedHotbarSlots(player);

        if (selected < 0 || selected >= allowed) {
            selected = 0;
        }
    }

    @Inject(
            method = "swapPaint",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictedScrolling(
            double direction,
            CallbackInfo ci
    ) {
        if (player == null || !player.level().isClientSide) {
            return;
        }

        int allowed =
                InventoryRestrictionHelper.getAllowedHotbarSlots(player);

        if (selected < 0 || selected >= allowed) {
            selected = 0;
        }

        if (direction > 0) {
            selected = (selected - 1 + allowed) % allowed;
        } else if (direction < 0) {
            selected = (selected + 1) % allowed;
        }

        ci.cancel();
    }

    // Handle ordinary inventory additions, including item pickups.
    // Do not redirect items into the 27 locked main-inventory slots.
    @Inject(
            method = "add(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictedInsertion(
            int requestedSlot,
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (player == null || requestedSlot != -1) {
            return;
        }

        if (incoming.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        Inventory inventory = (Inventory) (Object) this;
        int allowed =
                InventoryRestrictionHelper.getAllowedHotbarSlots(player);

        int originalCount = incoming.getCount();

        // First merge with compatible stacks.
        for (int i = 0; i < allowed && !incoming.isEmpty(); i++) {
            ItemStack existing = inventory.getItem(i);

            if (existing.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(existing, incoming)) {
                continue;
            }

            int max = Math.min(
                    existing.getMaxStackSize(),
                    inventory.getMaxStackSize()
            );

            int space = max - existing.getCount();

            if (space <= 0) {
                continue;
            }

            int amount = Math.min(space, incoming.getCount());

            existing.grow(amount);
            incoming.shrink(amount);

            inventory.setChanged();
        }

        // Then place items into empty unlocked slots.
        for (int i = 0; i < allowed && !incoming.isEmpty(); i++) {
            if (!inventory.getItem(i).isEmpty()) {
                continue;
            }

            int amount = Math.min(
                    incoming.getCount(),
                    Math.min(
                            incoming.getMaxStackSize(),
                            inventory.getMaxStackSize()
                    )
            );

            ItemStack placed = incoming.copy();
            placed.setCount(amount);

            inventory.setItem(i, placed);
            incoming.shrink(amount);
        }

        // Preserve any remainder in the incoming stack.
        // The caller can then leave uncollected items in the world.
        cir.setReturnValue(incoming.getCount() < originalCount);
    }
}