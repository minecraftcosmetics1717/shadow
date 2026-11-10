
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

    private boolean abyss$isLocked() {
        Slot slot = (Slot) (Object) this;

        if (slot.container instanceof Inventory inventory) {
            return !InventoryRestrictionHelper.isSlotAllowed(
                    inventory.player,
                    slot.getSlotIndex()
            );
        }

        return false;
    }

    @Inject(
            method = "mayPlace",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictPlacement(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (abyss$isLocked()) {
            cir.setReturnValue(false);
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
        if (abyss$isLocked()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "isHighlightable",
            at = @At("HEAD"),
            cancellable = true
    )
    private void abyss$restrictHighlight(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (abyss$isLocked()) {
            cir.setReturnValue(false);
        }
    }
}
