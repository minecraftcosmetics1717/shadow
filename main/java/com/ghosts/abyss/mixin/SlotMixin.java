package com.ghosts.abyss.mixin;

import com.ghosts.abyss.util.InventoryRestrictionHelper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(
            method = "mayPlace",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictPlacement(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Slot slot = (Slot) (Object) this;

        if (slot.container instanceof Inventory inventory) {
            if (!InventoryRestrictionHelper.isSlotAllowed(
                    inventory.player,
                    slot.getSlotIndex())) {

                cir.setReturnValue(false);
            }
        }
    }

    @Inject(
            method = "mayPickup",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictPickup(
            Player player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Slot slot = (Slot) (Object) this;

        if (slot.container instanceof Inventory inventory) {
            if (!InventoryRestrictionHelper.isSlotAllowed(
                    inventory.player,
                    slot.getSlotIndex())) {

                cir.setReturnValue(false);
            }
        }
    }
}