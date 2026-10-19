package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ghosts.abyss.util.InventoryRestrictionHelper;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void jujutsuslop$restrictMayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (self.container instanceof Inventory inv) {
            Player player = inv.player;
            if (player != null && !InventoryRestrictionHelper.isSlotAllowed(player, self.getSlotIndex())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void jujutsuslop$restrictMayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (self.container instanceof Inventory) {
            if (!InventoryRestrictionHelper.isSlotAllowed(player, self.getSlotIndex())) {
                cir.setReturnValue(false);
            }
        }
    }
}