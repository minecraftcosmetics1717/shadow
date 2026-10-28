
package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
            case 15 -> spawnMahito(level, inst);
            case 8 -> spawnDagon(level, inst);
            default -> {
                // No decorations configured.
            }
        }
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
                48.0,
                180.0F
        );
    }

    private static void spawnDagon(
            ServerLevel level,
            DomainInstance inst
    ) {
        // We'll add Dagon's trees here later.
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
