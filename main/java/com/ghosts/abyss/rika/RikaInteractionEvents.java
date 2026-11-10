
package com.ghosts.abyss.rika;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

public final class RikaInteractionEvents {

    private static final ResourceLocation RIKA_RL =
            new ResourceLocation("jujutsucraft", "rika");

    private static final ResourceLocation RIKA_2_RL =
            new ResourceLocation("jujutsucraft", "rika_2");

    private RikaInteractionEvents() {}

    public static void onRikaInteract(
            PlayerInteractEvent.EntityInteract event) {

        Entity target = event.getTarget();

        if (target != null) {
            ResourceLocation entityType =
                    ForgeRegistries.ENTITY_TYPES
                            .getKey(target.getType());

            if (RIKA_RL.equals(entityType)
                    || RIKA_2_RL.equals(entityType)) {

                String ownerUUID = target.getPersistentData()
                        .getString("OWNER_UUID");

                if (!ownerUUID.isEmpty()) {
                    Player player = event.getEntity();

                    if (player.getStringUUID().equals(ownerUUID)
                            && player instanceof ServerPlayer serverPlayer) {

                        event.setCanceled(true);

                        RikaInventory rikaInv =
                                RikaInventory.load(serverPlayer);

                        NetworkHooks.openScreen(
                                serverPlayer,
                                new SimpleMenuProvider(
                                        (id, playerInv, p) ->
                                                new RikaInventoryMenu(
                                                        id, playerInv, rikaInv
                                                ),
                                        Component.translatable(
                                                "container.abyss.rika_inventory"
                                        )
                                )
                        );
                    }
                }
            }
        }
    }
}
