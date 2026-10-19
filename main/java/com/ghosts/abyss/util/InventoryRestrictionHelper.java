package com.ghosts.abyss.util;

import net.minecraft.world.entity.player.Player;

public class InventoryRestrictionHelper {

    public static int getAllowedHotbarSlots(Player player) {
        boolean hasPerfectBody = player.getPersistentData().getBoolean("perfect_body");
        return hasPerfectBody ? 5 : 3;
    }

    /**
     * Returns true if the player is allowed to use this inventory slot.
     * Only the first N hotbar slots are allowed.
     * Armor (36-39) and Offhand (40) are always allowed.
     */
    public static boolean isSlotAllowed(Player player, int slotIndex) {
        // Always allow armor + offhand
        if (slotIndex >= 36) {
            return true;
        }

        // Only allow the first few hotbar slots
        return slotIndex >= 0 && slotIndex < getAllowedHotbarSlots(player);
    }

    public static boolean hasFreeAllowedSlot(Player player) {
        int allowed = getAllowedHotbarSlots(player);
        for (int i = 0; i < allowed; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}