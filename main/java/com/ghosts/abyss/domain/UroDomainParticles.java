
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class UroDomainParticles {

    private UroDomainParticles() {}

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (entity.getType() !=
                JujutsucraftModEntities.ENTITY_DOMAIN_URO.get()) {
            return;
        }

        if (!entity.getPersistentData()
                .getBoolean("abyss_domain_decoration")) {
            return;
        }

        // Reduce particle processing while keeping
        // the original sweeping visual pattern.
        if (entity.tickCount % 4 != 0) {
            return;
        }

        double radius =
                Math.max(DomainDimension.INNER_RADIUS - 2.0D, 0.0D);

        double yaw = Math.toRadians(
                entity.getYRot() + 90.0D
        );

        double startX =
                entity.getX() + Math.cos(yaw) * radius;

        double startZ =
                entity.getZ() + Math.sin(yaw) * radius;

        double y = entity.getY();

        // Sweep inward from the edge of the arena.
        double stepX = -Math.cos(yaw) * 4.0D;
        double stepZ = -Math.sin(yaw) * 4.0D;

        double steps = Math.min(
                Math.max(radius / 2.0D, 1.0D),
                entity.tickCount * 0.25D
        );

        for (int i = 0; i < (int) steps; i++) {

            for (int side = -1; side <= 1; side += 2) {

                double sideAngle =
                        yaw + Math.toRadians(side * 90.0D);

                double px =
                        startX + Math.cos(sideAngle) * 4.0D;

                double pz =
                        startZ + Math.sin(sideAngle) * 4.0D;

                level.sendParticles(
                        JujutsucraftModParticleTypes
                                .PARTICLE_CURSE_POWER_BLUE.get(),
                        px, y, pz,
                        1,
                        0.25D,
                        0.5D,
                        0.25D,
                        0.025D
                );
            }

            startX += stepX;
            startZ += stepZ;
        }
    }
}
