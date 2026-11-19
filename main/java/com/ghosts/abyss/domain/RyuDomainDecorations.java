
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModAttributes;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.PlayAnimationEntity2Procedure;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public final class RyuDomainDecorations {

    private static final int INITIAL_SANDSTONE = 22;
    private static final int INITIAL_STRUCTURES = 8;

    // Prevent the recurring decorations from
    // accumulating indefinitely.
    private static final int MAX_STRUCTURES = 24;

    private RyuDomainDecorations() {}

    public static void spawnInitial(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant
    ) {
        if (level == null || inst == null
                || participant == null) {
            return;
        }

        for (int i = 0; i < INITIAL_SANDSTONE; i++) {
            spawnOne(
                    level, inst, participant,
                    false
            );
        }

        for (int i = 0; i < INITIAL_STRUCTURES; i++) {
            spawnOne(
                    level, inst, participant,
                    true
            );
        }
    }

    public static void tick(
            ServerLevel level,
            DomainInstance inst,
            LivingEntity owner
    ) {
        if (level == null || inst == null
                || owner == null) {
            return;
        }
// Match Jujutsucraft's original behavior:
// large Ishigori structures disappear after 40 ticks.
        removeExpiredStructures(level, inst, owner.getUUID());
        // Original JC behavior attempts additional
        // decorations every 10 ticks with a
        // 50% chance.
        if (level.getGameTime() % 10 != 0) {
            return;
        }

        if (level.random.nextFloat() >= 0.5F) {
            return;
        }

        DomainClashParticipants.Participant participant =
                inst.participants.get(owner.getUUID());

        if (participant == null
                || (int) participant.skillDomain != 12) {
            return;
        }

        AABB bounds = new AABB(
                inst.arenaCenter
        ).inflate(DomainDimension.OUTER_RADIUS + 4);

        int count = level.getEntities(
                (Entity) null,
                bounds,
                entity ->
                        entity.isAlive()
                                && entity.getPersistentData()
                                .getBoolean("abyss_ryu_structure")
                                && entity.getPersistentData()
                                .hasUUID("abyss_decoration_owner")
                                && owner.getUUID().equals(
                                entity.getPersistentData()
                                        .getUUID("abyss_decoration_owner")
                        )
        ).size();

        int remaining = MAX_STRUCTURES - count;

        for (int i = 0; i < Math.min(3, remaining); i++) {
            spawnOne(
                    level, inst, participant,
                    true
            );
        }
    }

    private static void spawnOne(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant,
            boolean structure
    ) {
        EntityType<?> type = structure
                ? JujutsucraftModEntities
                .ENTITY_DOMAIN_ISHIGORI.get()
                : JujutsucraftModEntities
                .ENTITY_SAND_STONE.get();

        Entity entity = type.create(level);
        if (entity == null) return;

        // Position decorations throughout the arena.
        // During clashes, prefer the caster's side.
        double sideAngle = switch (participant.side) {
            case NORTH -> Math.PI * 1.5D;
            case SOUTH -> Math.PI * 0.5D;
            case WEST -> Math.PI;
            case EAST -> 0.0D;
        };

        double angle;

        if (inst.participants.size() > 1) {
            angle = sideAngle
                    + (level.random.nextDouble() - 0.5D)
                    * Math.PI;
        } else {
            angle = level.random.nextDouble()
                    * Math.PI * 2.0D;
        }

        double radius = structure
                ? 38.0D + level.random.nextDouble() * 14.0D
                : 24.0D + level.random.nextDouble() * 26.0D;

        double x = inst.arenaCenter.getX() + 0.5D
                + Math.cos(angle) * radius;

        double z = inst.arenaCenter.getZ() + 0.5D
                + Math.sin(angle) * radius;

        double y = inst.arenaCenter.getY()
                + (structure
                ? 7.0D + level.random.nextDouble() * 12.0D
                : 1.0D);

        float yaw = level.random.nextFloat() * 360.0F;

        entity.moveTo(x, y, z, yaw, 0.0F);

        // Domain decorations should not attack,
        // fall, or disappear because of damage.
        entity.setNoGravity(true);
        entity.setInvulnerable(true);

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        // Match the approximate original JC
        // size ranges.
        if (entity instanceof LivingEntity living) {
            AttributeInstance size = living.getAttribute(
                    JujutsucraftModAttributes.SIZE.get()
            );

            if (size != null) {
                double scale = structure
                        ? 3.0D + level.random.nextDouble()
                        : 1.0D + level.random.nextDouble();

                size.setBaseValue(scale);
            }
        }

        if (structure) {
            int animation = 1 + level.random.nextInt(4);

            PlayAnimationEntity2Procedure.execute(
                    entity,
                    "idle" + animation
            );

            entity.getPersistentData().putBoolean(
                    "abyss_ryu_structure",
                    true
            );
        }

        // Use Abyss's normal domain decoration
        // ownership and cleanup system.
        entity.getPersistentData().putBoolean(
                "abyss_domain_decoration",
                true
        );

        entity.getPersistentData().putUUID(
                "abyss_decoration_owner",
                participant.entityId
        );


// Preserve Jujutsucraft's original ownership check.
        Entity caster = level.getEntity(participant.entityId);

        if (caster != null) {

            double nameRanged = caster.getPersistentData()
                    .getDouble("NameRanged");

            if (nameRanged == 0.0D) {
                nameRanged = 0.5D
                        + level.random.nextDouble() * 0.5D;

                caster.getPersistentData().putDouble(
                        "NameRanged",
                        nameRanged);
            }

            entity.getPersistentData().putDouble(
                    "NameRanged_ranged",
                    nameRanged);}

        entity.getPersistentData().putString(
                "OWNER_UUID",
                participant.entityId.toString());

        level.addFreshEntity(entity);

    }
    private static void removeExpiredStructures(
            ServerLevel level,
            DomainInstance inst,
            UUID ownerId
    ) {
        AABB bounds = new AABB(
                inst.arenaCenter
        ).inflate(DomainDimension.OUTER_RADIUS + 4);

        for (Entity entity : level.getEntities(
                (Entity) null,
                bounds,
                e -> e.getPersistentData()
                        .getBoolean("abyss_ryu_structure")
        )) {

            // Only affect this caster's structures.
            if (!entity.getPersistentData()
                    .hasUUID("abyss_decoration_owner")) {
                continue;
            }

            if (!ownerId.equals(
                    entity.getPersistentData()
                            .getUUID("abyss_decoration_owner"))) {
                continue;
            }

            // JC removes these after approximately 2 seconds.
            if (entity.tickCount > 40) {
                entity.discard();
            }
        }
    }

}
