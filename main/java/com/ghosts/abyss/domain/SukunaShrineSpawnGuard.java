
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abyss")
public final class SukunaShrineSpawnGuard {

    private SukunaShrineSpawnGuard() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Only the two original Malevolent Shrine entities.
        Entity shrine = event.getEntity();

        if (shrine.getType()
                != JujutsucraftModEntities.ENTITY_MALEVOLENT_SHRINE.get()
                && shrine.getType()
                != JujutsucraftModEntities.ENTITY_MALEVOLENT_SHRINE_2.get()) {
            return;
        }

        // Don't interfere with decorations inside
        // Abyss's separate domain dimension.
        if (level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        String ownerString = shrine.getPersistentData()
                .getString("OWNER_UUID");

        if (ownerString.isEmpty()) {
            return;
        }

        UUID ownerId;

        try {
            ownerId = UUID.fromString(ownerString);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        ServerPlayer owner = level.getServer()
                .getPlayerList()
                .getPlayer(ownerId);

        // Leave NPC-owned shrine entities alone.
        if (owner == null) {
            return;
        }

        boolean open =
                OpenDomain.shouldUseOpenDomain(owner, 1.0D);

        // A closed-domain shrine must not remain
        // in the overworld.
        if (!open) {
            event.setCanceled(true);
        }
    }
}
