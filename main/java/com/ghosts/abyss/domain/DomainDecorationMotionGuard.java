
package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber(modid = "abyss")
public final class DomainDecorationMotionGuard {

    private DomainDecorationMotionGuard() {}

    // Only freeze decorative entities that should
    // remain stationary.
    private static final Set<ResourceLocation> STATIONARY = Set.of(
            id("entity_sand_stone"),
            id("entity_domain_ishigori"),
            id("entity_unlimited_void"),
            id("entity_hands"),
            id("entity_chimera_shadow_garden"),
            id("entity_time_cell_moon_palace"),
            id("entity_threefold_affliction"),
            id("ticket_gate"),
            id("entity_item"),
            id("gravestone_4"),
            id("entity_awaji_knot"),
            id("entity_heart")
    );

    private static ResourceLocation id(String name) {
        return new ResourceLocation("jujutsucraft", name);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) return;

        if (!entity.getPersistentData()
                .getBoolean("abyss_domain_decoration")) {
            return;
        }

        ResourceLocation type =
                ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());

        if (type == null || !STATIONARY.contains(type)) {
            return;
        }

        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
    }
}
