package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ghosts.abyss.util.InventoryRestrictionHelper;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow @Final public Player player;

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void jujutsuslop$blockSetItem(int index, ItemStack stack, CallbackInfo ci) {
        if (this.player == null) return;

        if (!stack.isEmpty() && !InventoryRestrictionHelper.isSlotAllowed(this.player, index)) {
            ci.cancel();
        }
    }

    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void jujutsuslop$wrapScroll(double direction, CallbackInfo ci) {
        if (this.player == null) return;
        if (!this.player.level().isClientSide) return;

        int allowed = InventoryRestrictionHelper.getAllowedHotbarSlots(this.player);
        if (allowed <= 0) {
            ci.cancel();
            return;
        }

        ci.cancel();

        Inventory self = (Inventory) (Object) this;
        int selected = self.selected;

        if (direction > 0) {
            selected--;
        } else {
            selected++;
        }

        if (selected < 0) {
            selected = allowed - 1;
        } else if (selected >= allowed) {
            selected = 0;
        }

        self.selected = selected;
    }
}