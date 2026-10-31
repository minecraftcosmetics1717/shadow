
package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

public final class DomainNeutralization {

    private static final ResourceLocation EFFECT_ID =
            new ResourceLocation(
                    "jujutsucraft",
                    "neutralization"
            );

    private static final int DURATION = 10;

    private DomainNeutralization() {}

    public static void refresh(
            ServerLevel level,
            DomainInstance inst
    ) {
        if (level == null || inst == null) return;

        MobEffect effect =
                ForgeRegistries.MOB_EFFECTS.getValue(EFFECT_ID);

        if (effect == null) return;

        BlockPos center = inst.arenaCenter;
        int radius = DomainDimension.INNER_RADIUS;

        AABB box = new AABB(
                center.getX() - radius,
                center.getY() - radius,
                center.getZ() - radius,
                center.getX() + radius + 1,
                center.getY() + radius + 1,
                center.getZ() + radius + 1
        );

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        LivingEntity::isAlive
                )) {

            double dx = target.getX()
                    - (center.getX() + 0.5);

            double dy = target.getY()
                    - (center.getY() + 0.5);

            double dz = target.getZ()
                    - (center.getZ() + 0.5);

            // Only affect entities inside the sphere.
            if (dx * dx + dy * dy + dz * dz
                    > radius * radius) {
                continue;
            }

            MobEffectInstance current =
                    target.getEffect(effect);

            // Refresh near expiration, rather than
            // repeatedly reapplying every tick.
            if (current != null
                    && current.getDuration() > 5) {
                continue;
            }

            target.addEffect(new MobEffectInstance(
                    effect,
                    DURATION,
                    0,
                    false, // Ambient
                    false, // No particles
                    false  // Hide effect icon
            ));
        }
    }
}
