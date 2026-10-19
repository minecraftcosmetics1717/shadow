package com.ghosts.abyss.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.capability.ModCapabilities;
import com.ghosts.abyss.capability.PlayerBodyDataProvider;
import com.ghosts.abyss.util.InventoryRestrictionHelper;

@Mod.EventBusSubscriber(modid = "abyss")
public class InventoryRestrictionEvents {

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation("abyss", "player_body_data"),
                    new PlayerBodyDataProvider()
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ModCapabilities.PLAYER_BODY_DATA).ifPresent(oldData -> {
            event.getEntity().getCapability(ModCapabilities.PLAYER_BODY_DATA).ifPresent(newData -> {
                newData.setHasPerfectBody(oldData.hasPerfectBody());
            });
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        Inventory inv = player.getInventory();
        int allowed = InventoryRestrictionHelper.getAllowedHotbarSlots(player);

        // Eject items from every disabled slot
        for (int i = 0; i < 36; i++) {
            if (!InventoryRestrictionHelper.isSlotAllowed(player, i)) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    player.drop(stack, false);
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        // Clamp selected hotbar index
        if (inv.selected >= allowed) {
            inv.selected = Math.max(0, allowed - 1);
        }
    }
    @SubscribeEvent
    public static void onEntityItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        // If the player has no free allowed slots, cancel the pickup completely
        if (!InventoryRestrictionHelper.hasFreeAllowedSlot(player)) {
            event.setCanceled(true);
        }
    }
}