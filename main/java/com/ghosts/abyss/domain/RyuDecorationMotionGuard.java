
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class RyuDecorationMotionGuard {

    private RyuDecorationMotionGuard() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (!entity.getPersistentData()
                .getBoolean("abyss_domain_decoration")) {
            return;
        }

        boolean isSandstone = entity.getType()
                == JujutsucraftModEntities.ENTITY_SAND_STONE.get();

        boolean isStructure = entity.getType()
                == JujutsucraftModEntities.ENTITY_DOMAIN_ISHIGORI.get();

        if (!isSandstone && !isStructure) {
            return;
        }

        // Keep Ryu's decorations stationary.
        // This runs before Jujutsucraft's normal
        // high-speed collision handler.
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
    }
}
