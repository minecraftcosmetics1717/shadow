
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

public final class DomainDecorations {

    private DomainDecorations() {}

    public static void spawn(
            ServerLevel level,
            DomainInstance inst
    ) {
        if (level == null || inst == null) return;

        int domainId = (int) inst.skillDomain;

        switch (domainId) {
            case 2 -> spawnGojo(level, inst);
            case 15 -> spawnMahito(level, inst);
            case 8 -> spawnDagon(level, inst);
            default -> {
                // No decorations configured.
            }
        }
    }
    private static void spawnGojo(
            ServerLevel level,
            DomainInstance inst
    ) {
        // Unlimited Void centerpiece near positive-Z wall.
        spawnDecoration(
                level,
                inst,
                "jujutsucraft:entity_unlimited_void",
                0.0,
                2.0,
                56.0,
                180.0F
        );
    }
    private static void spawnMahito(
            ServerLevel level,
            DomainInstance inst
    ) {
        // Giant hands near the positive-Z wall.
        spawnDecoration(
                level,
                inst,
                "jujutsucraft:entity_hands",
                0.0,
                1.0,
                57.0,
                180.0F
        );
    }

    private static void spawnDagon(
            ServerLevel level,
            DomainInstance inst
    ) {
        // We'll add Dagon's trees here later.
    }
    public static void tick(
            ServerLevel level
    ) {
        if (level == null) return;

        // Limit particle spawning to every 4 ticks.
        if (level.getGameTime() % 4 != 0) return;

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {

            DomainInstance inst = DomainInstanceManager.getBySlot(slot);

            if (inst == null || (int) inst.skillDomain != 2) {
                continue;
            }

            spawnGojoParticles(level, inst);
        }
    }


    private static void spawnGojoParticles(
            ServerLevel level,
            DomainInstance inst
    ) {
        RandomSource random = level.random;

        double cx = inst.arenaCenter.getX() + 0.5;
        double cy = inst.arenaCenter.getY() + 1.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        // Leave a small margin before the domain boundary.
        final double MAX_RADIUS = 58.0;


        // Larger Unlimited Void splashes.
        // Spread across the outer portion of the sphere,
        // including above and below the player.
        for (int i = 0; i < 3; i++) {

            double yDir = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;

            double horizontal = Math.sqrt(1.0 - yDir * yDir);

            // Bias splashes toward the distant environment.
            double radius = 35.0
                    + random.nextDouble() * 23.0;

            double x = cx
                    + Math.cos(angle) * horizontal * radius;

            double y = cy + yDir * radius;

            double z = cz
                    + Math.sin(angle) * horizontal * radius;

            level.sendParticles(
                    JujutsucraftModParticleTypes
                            .PARTICLE_UNLIMITED_VOID.get(),
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }

        // Jujutsucraft's white splash effects.
        // Spawn these less frequently near the outer arena.
        if (random.nextFloat() < 0.35F) {

            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 40.0 + random.nextDouble() * 12.0;

            double x = cx + Math.cos(angle) * radius;
            double y = cy + 4.0 + random.nextDouble() * 35.0;
            double z = cz + Math.sin(angle) * radius;

            level.sendParticles(
                    JujutsucraftModParticleTypes
                            .PARTICLE_UNLIMITED_VOID.get(),
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }
    private static void spawnDecoration(
            ServerLevel level,
            DomainInstance inst,
            String entityId,
            double offsetX,
            double offsetY,
            double offsetZ,
            float yaw
    ) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(entityId)
        );

        if (type == null) return;

        Entity entity = type.create(level);
        if (entity == null) return;

        double x = inst.arenaCenter.getX() + 0.5 + offsetX;
        double y = inst.arenaCenter.getY() + offsetY;
        double z = inst.arenaCenter.getZ() + 0.5 + offsetZ;

        entity.moveTo(x, y, z, yaw, 0.0F);
        entity.setInvulnerable(true);
        entity.setNoGravity(true);

        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        entity.getPersistentData().putBoolean(
                "abyss_domain_decoration", true
        );

        level.addFreshEntity(entity);
    }
}
