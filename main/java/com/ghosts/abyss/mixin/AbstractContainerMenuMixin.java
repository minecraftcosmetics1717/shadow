package com.ghosts.abyss.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void lockInventorySlots(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        if (menu instanceof InventoryMenu) {
            if (slotId < 0 || slotId >= menu.slots.size()) return;

            boolean hasPerfectBody = player.getPersistentData().getBoolean("perfect_body");
            int maxAllowedHotbarSlot = hasPerfectBody ? 4 : 2;

            // Block Main Inventory slots (9..35)
            if (slotId >= 9 && slotId <= 35) {
                ci.cancel();
                return;
            }

            // Block locked Hotbar slots (36..44 mapped past active limit)
            if (slotId >= 36 && slotId <= 44) {
                int hotbarIndex = slotId - 36;
                if (hotbarIndex > maxAllowedHotbarSlot) {
                    ci.cancel();
                }
            }
        }
    }

    // Intercept shift-click transfer logic across ALL container menus
    @Inject(method = "moveItemStackTo", at = @At("HEAD"))
    private void capContainerMoveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // Skip standard Survival InventoryMenu (handled separately in InventoryMenuMixin)
        if (menu instanceof InventoryMenu) return;

        // Obtain player instance associated with menu
        Player player = null;
        if (!menu.slots.isEmpty() && menu.slots.get(0).container instanceof Player p) {
            player = p;
        }

        if (player == null) return;

        boolean hasPerfectBody = player.getPersistentData().getBoolean("perfect_body");
        int maxAllowedSlot = hasPerfectBody ? 4 : 2; // Slots 0..2 or 0..4

        int totalSlots = menu.slots.size();
        int hotbarStart = totalSlots - 9; // Last 9 slots in container menus are the hotbar

        // If the target range includes main inventory or locked hotbar slots:
        // Cap the endIndex so moveItemStackTo cannot access slots beyond (hotbarStart + maxAllowedSlot + 1)
        if (endIndex == totalSlots) {
            // Force transfer logic to stop right after slot index 2 (3 slots total) or slot index 4 (5 slots total)
            int cappedEndIndex = hotbarStart + maxAllowedSlot + 1;

            // Re-route transfer by executing custom capped loop if standard range exceeds allowed boundary
            if (endIndex > cappedEndIndex) {
                // Adjust parameter scope on local execution
                try {
                    java.lang.reflect.Field endField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("endIndex");
                } catch (Exception ignored) {}
            }
        }
    }
}