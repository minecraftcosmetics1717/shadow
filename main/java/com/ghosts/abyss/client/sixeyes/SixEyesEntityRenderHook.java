
package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesEntityRenderHook {

    private static final Map<UUID, SixEyesEntityColors.ThermalType>
            LAST_TYPES = new HashMap<>();

    private SixEyesEntityRenderHook() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Only check entities while Six Eyes is active.
        if (mc.player == null
                || mc.level == null
                || !SixEyesVisionHandler.shouldRender(mc.player)) {

            LAST_TYPES.clear();
            return;
        }

        Set<UUID> currentlyLoaded = new HashSet<>();

        // Find living entities within 128 blocks of the player.
        // This does not depend on RenderLivingEvent.
        for (LivingEntity living : mc.level.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(128.0D)
        )) {
            ResourceLocation entityId =
                    ForgeRegistries.ENTITY_TYPES.getKey(living.getType());

            if (entityId != null
                    && entityId.getNamespace().equals("jujutsucraft")) {

                String path = entityId.getPath();

                if (path.equals("entity_attack_slash")
                        || path.equals("entity_attack_strike")
                        || path.equals("entity_attack_sweep")) {
                    continue;
                }
            }
            UUID uuid = living.getUUID();
            currentlyLoaded.add(uuid);

            // Determine the entity's Six Eyes classification.
            SixEyesEntityColors.ThermalType type =
                    SixEyesEntityColors.getType(living);

            SixEyesEntityColors.ThermalType previous =
                    LAST_TYPES.put(uuid, type);

            // Log newly detected entities and classification changes.
            if (previous != type) {

                System.out.println(
                        "[Abyss Six Eyes] Entity: "
                                + ForgeRegistries.ENTITY_TYPES.getKey(living.getType())
                                + " | Name: "
                                + living.getName().getString()
                                + " | Classification: "
                                + type
                );
            }
        }

        // Forget entities that are no longer within the search area.
        LAST_TYPES.keySet().removeIf(
                uuid -> !currentlyLoaded.contains(uuid)
        );
    }
}
