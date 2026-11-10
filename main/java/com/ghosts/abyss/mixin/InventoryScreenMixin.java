package com.ghosts.abyss.mixin;

import com.ghosts.abyss.util.InventoryRestrictionHelper;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(
            method = "findSlot",
            at = @At("RETURN"),
            cancellable = true
    )
    private void abyss$hideLockedSlotHover(
            double mouseX,
            double mouseY,
            CallbackInfoReturnable<Slot> cir
    ) {
        Slot slot = cir.getReturnValue();

        if (slot == null) {
            return;
        }

        if (slot.container instanceof Inventory inventory) {
            if (!InventoryRestrictionHelper.isSlotAllowed(
                    inventory.player,
                    slot.getSlotIndex())) {

                cir.setReturnValue(null);
            }
        }
    }
}