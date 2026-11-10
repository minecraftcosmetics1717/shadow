package com.ghosts.abyss.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryRestrictionHelper {

    public static final int NORMAL_SLOTS = 3;
    public static final int PERFECT_BODY_SLOTS = 5;

    private InventoryRestrictionHelper() {}

    public static int getAllowedHotbarSlots(Player player) {
        if (player == null) {
            return NORMAL_SLOTS;
        }

        return player.getPersistentData()
                .getBoolean("perfect_body")
                ? PERFECT_BODY_SLOTS
                : NORMAL_SLOTS;
    }

    public static boolean isSlotAllowed(
            Player player,
            int slotIndex
    ) {
        if (player == null) {
            return false;
        }

        // Hotbar: first 3 or 5 slots.
        if (slotIndex >= 0 && slotIndex < 9) {
            return slotIndex < getAllowedHotbarSlots(player);
        }

        // Main inventory: completely locked.
        if (slotIndex >= 9 && slotIndex < 36) {
            return false;
        }

        // Armor and offhand remain usable.
        return slotIndex >= 36 && slotIndex <= 40;
    }

    public static boolean hasFreeAllowedSlot(Player player) {
        return findFreeAllowedSlot(player) != -1;
    }

    public static int findFreeAllowedSlot(Player player) {
        if (player == null) {
            return -1;
        }

        Inventory inventory = player.getInventory();
        int allowed = getAllowedHotbarSlots(player);

        for (int i = 0; i < allowed; i++) {
            if (inventory.getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public static int findAllowedStackSlot(
            Player player,
            ItemStack incoming
    ) {
        if (player == null || incoming.isEmpty()) {
            return -1;
        }

        Inventory inventory = player.getInventory();
        int allowed = getAllowedHotbarSlots(player);

        for (int i = 0; i < allowed; i++) {
            ItemStack existing = inventory.getItem(i);

            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameTags(
                    existing, incoming)
                    && existing.getCount()
                    < Math.min(
                    existing.getMaxStackSize(),
                    inventory.getMaxStackSize())) {
                return i;
            }
        }

        return -1;
    }
}