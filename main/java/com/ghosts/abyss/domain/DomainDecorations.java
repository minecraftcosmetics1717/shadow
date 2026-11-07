
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;
import virtuoel.pehkui.api.ScaleTypes;

public final class DomainDecorations {

    private DomainDecorations() {}

    public static void spawn(ServerLevel level, DomainInstance inst) {
        if (level == null || inst == null) return;
        for (DomainClashParticipants.Participant p : inst.participants.all()) {
            spawnParticipant(level, inst, p);
        }
    }

    public static void spawnParticipant(ServerLevel level, DomainInstance inst,
                                        DomainClashParticipants.Participant participant) {
        if (level == null || inst == null || participant == null) return;

// Yuta has multiple interactive decorations, so he needs
// a separate one-time spawning system.
        if ((int) participant.skillDomain == 5) {
            spawnYutaDecorations(level, inst, participant);
            return;
        }

        if (hasDecoration(level, inst, participant.entityId)) return;

        // Hakari — Idle Death Gamble
        if ((int) participant.skillDomain == 29) {
            spawnHakariGates(level, inst, participant);
            return;
        }

        String entityId = switch ((int) participant.skillDomain) {
            case 2 -> "jujutsucraft:entity_unlimited_void";
            case 15 -> "jujutsucraft:entity_hands";
            case 19 -> "jujutsucraft:entity_time_cell_moon_palace";
            default -> null;
        };
        if (entityId == null) return;

        double distance = switch ((int) participant.skillDomain) {
            case 2 -> 56.0;
            case 15 -> 57.0;
            case 19 -> 43.0;
            default -> 57.0;
        };
        double offsetX = 0.0;
        double offsetZ = 0.0;
        float yaw = 0.0F;
        switch (participant.side) {
            case NORTH -> { offsetZ = -distance; yaw = 0.0F; }
            case SOUTH -> { offsetZ = distance; yaw = 180.0F; }
            case WEST -> { offsetX = -distance; yaw = 90.0F; }
            case EAST -> { offsetX = distance; yaw = 270.0F; }
        }

        double offsetY = switch ((int) participant.skillDomain) {
            case 2 -> 2.0;  // Gojo (unchanged)
            case 15 -> 1.0; // Mahito (unchanged)
            case 19 -> 3.5; // Naoya (raised by 2 blocks)
            default -> 1.0;
        };
        spawnDecoration(level, inst, entityId, offsetX, offsetY,
                offsetZ, yaw, participant.entityId);
    }

    private static AABB arenaBox(DomainInstance inst) {
        BlockPos c = inst.arenaCenter;
        int r = DomainDimension.OUTER_RADIUS + 4;
        return new AABB(c.getX() - r, c.getY() - r, c.getZ() - r,
                c.getX() + r + 1, c.getY() + r + 1, c.getZ() + r + 1);
    }

    private static boolean hasDecoration(ServerLevel level, DomainInstance inst, UUID casterId) {
        return !level.getEntities((Entity) null, arenaBox(inst), entity ->
                entity.getPersistentData().getBoolean("abyss_domain_decoration")
                        && entity.getPersistentData().hasUUID("abyss_decoration_owner")
                        && casterId.equals(entity.getPersistentData().getUUID("abyss_decoration_owner"))
        ).isEmpty();
    }

    public static void sync(ServerLevel level, DomainInstance inst) {
        if (level == null || inst == null) return;
        inst.yutaDecorationsSpawned.removeIf(
                casterId -> !inst.participants.contains(casterId)
        );
        for (Entity entity : level.getEntities((Entity) null, arenaBox(inst), e ->
                e.getPersistentData().getBoolean("abyss_domain_decoration"))) {
            if (!entity.getPersistentData().hasUUID("abyss_decoration_owner")
                    || !inst.participants.contains(entity.getPersistentData()
                    .getUUID("abyss_decoration_owner"))) {
                entity.discard();
            }
        }
        spawn(level, inst);
    }

    public static void tick(
            ServerLevel level
    ) {
        if (level == null) return;

        // Limit particle spawning to every 4 ticks.
        if (level.getGameTime() % 4 != 0) return;

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {

            DomainInstance inst = DomainInstanceManager.getBySlot(slot);
            if (inst != null) sync(level, inst);

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

        // ==========================================
        // GOJO - END ROD PARTICLES
        // ==========================================

        // Most particles appear near the outer edge
        // of the domain, like an expanded Unlimited Void.
        for (int i = 0; i < 12; i++) {

            double angle = random.nextDouble() * Math.PI * 2.0;

            // Outer ring, close to the 63-block boundary.
            double radius = 52.0 + random.nextDouble() * 8.0;

            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;

            // Slight height variation near the ground.
            double y = cy + 0.5 + random.nextDouble() * 4.0;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }

        // A smaller number of End Rod particles
        // floating throughout the air.
        for (int i = 0; i < 3; i++) {

            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(random.nextDouble()) * 45.0;

            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;

            // Particles at different heights above the floor.
            double y = cy + 5.0 + random.nextDouble() * 30.0;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }


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

    /**
     * Removes decorations belonging to this arena,
     * without affecting players, mobs, or other arenas.
     */
    public static void clear(ServerLevel level, DomainInstance inst) {
        if (level == null || inst == null) return;
        for (Entity entity : level.getEntities((Entity) null, arenaBox(inst), e ->
                e.getPersistentData().getBoolean("abyss_domain_decoration"))) {
            entity.discard();
        }
    }

    private static void spawnDecoration(
            ServerLevel level,
            DomainInstance inst,
            String entityId,
            double offsetX,
            double offsetY,
            double offsetZ,
            float yaw,
            UUID casterId
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


// Enlarge domain decorations with Pehkui.

        if ("jujutsucraft:entity_hands".equals(entityId)
                || "jujutsucraft:entity_unlimited_void".equals(entityId)) {

            // Mahito and Gojo remain 3x.
            ScaleTypes.BASE.getScaleData(entity).setScale(3.0F);

        } else if ("jujutsucraft:ticket_gate".equals(entityId)) {

            // Hakari's ticket gates are 2x.
            ScaleTypes.BASE.getScaleData(entity).setScale(2.0F);
        }




        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        entity.getPersistentData().putBoolean(
                "abyss_domain_decoration", true
        );

        entity.getPersistentData().putUUID("abyss_decoration_owner", casterId);
        level.addFreshEntity(entity);
    }

    private static void spawnHakariGates(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant
    ) {
        // 6 ticket gates per side, 12 total.
        final int gatesPerSide = 6;
        final double rowOffset = 43.0;
        final double spacing = 14.0;

        // Align the rows with the participant's domain side.
        // NORTH/SOUTH: rows on east and west.
        // EAST/WEST: rows on north and south.
        boolean eastWestRows =
                participant.side == DomainClashParticipants.Side.NORTH
                        || participant.side == DomainClashParticipants.Side.SOUTH;

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < gatesPerSide; i++) {

                double along = (i - (gatesPerSide - 1) / 2.0)
                        * spacing;

                double x;
                double z;
                float yaw;

                if (eastWestRows) {
                    x = side * rowOffset;
                    z = along;
                    yaw = side < 0 ? 90.0F : 270.0F;
                } else {
                    x = along;
                    z = side * rowOffset;
                    yaw = side < 0 ? 0.0F : 180.0F;
                }

                spawnDecoration(
                        level,
                        inst,
                        "jujutsucraft:ticket_gate",
                        x,
                        1.0,
                        z,
                        yaw,
                        participant.entityId
                );
            }
        }
    }

    // ==========================================
    // YUTA - AUTHENTIC MUTUAL LOVE
    // ==========================================

    private static final int YUTA_SWORDS = 120;
    private static final int YUTA_GRAVESTONES = 42;
    private static final int YUTA_AWAJI_KNOTS = 19;

    private static final double YUTA_FLOOR_RADIUS = 54.0;
    private static final double YUTA_KNOT_RADIUS = 59.0;

    private static void spawnYutaDecorations(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant
    ) {
        UUID casterId = participant.entityId;

        // Prevent duplicate spawning, including after
        // players pick up swords.
        if (inst.yutaDecorationsSpawned.contains(casterId)) {
            return;
        }

        // Mark before spawning so repeated sync calls
        // cannot trigger a second generation.
        inst.yutaDecorationsSpawned.add(casterId);

        RandomSource random = level.random;

        double centerX = inst.arenaCenter.getX() + 0.5;
        double centerY = inst.arenaCenter.getY() + 1.0;
        double centerZ = inst.arenaCenter.getZ() + 0.5;

        // ------------------------------------------
        // 1. SWORDS
        // ------------------------------------------

        EntityType<?> swordType =
                JujutsucraftModEntities.ENTITY_ITEM.get();

        Item swordItem = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(
                        "jujutsucraft",
                        "sword_okkotsu_yuta"
                )
        );

        EntityDataAccessor<Boolean> swordDecorationAccessor =
                getYutaSwordDecorationAccessor();

        if (swordItem != null && swordDecorationAccessor != null) {

            for (int i = 0; i < YUTA_SWORDS; i++) {

                double angle = random.nextDouble() * Math.PI * 2.0;

                // Square root gives a more even distribution
                // across the circular floor.
                double radius = Math.sqrt(random.nextDouble())
                        * YUTA_FLOOR_RADIUS;

                double x = centerX + Math.cos(angle) * radius;
                double z = centerZ + Math.sin(angle) * radius;

                float yaw = random.nextFloat() * 360.0F;
                float pitch = (random.nextFloat() - 0.5F) * 30.0F;

                double y = centerY
                        - Math.sin(Math.toRadians(Math.abs(pitch * 3.0F)))
                        - 0.5;

                Entity sword = swordType.create(level);
                if (sword == null) continue;

                sword.moveTo(x, y, z, yaw, pitch);
                sword.setNoGravity(true);
                sword.setInvulnerable(true);

                if (sword instanceof Mob mob) {
                    mob.setNoAi(true);
                }

                if (sword instanceof LivingEntity living) {
                    living.setYHeadRot(yaw);

                    living.setItemSlot(
                            EquipmentSlot.HEAD,
                            new ItemStack(swordItem)
                    );
                }

                try {
                    sword.getEntityData().set(
                            swordDecorationAccessor,
                            true
                    );
                } catch (Exception ignored) {
                    continue;
                }

                markYutaDecoration(sword, casterId);
                level.addFreshEntity(sword);
            }
        }

        // ------------------------------------------
        // 2. CROSSES / GRAVESTONES
        // ------------------------------------------

        EntityType<?> gravestoneType =
                JujutsucraftModEntities.GRAVESTONE_4.get();

        for (int i = 0; i < YUTA_GRAVESTONES; i++) {

            double angle = random.nextDouble() * Math.PI * 2.0;

            // Keep most gravestones away from the center.
            double radius = 12.0
                    + Math.sqrt(random.nextDouble()) * 40.0;

            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;

            double y = centerY - 1.0 - random.nextDouble();

            float yaw = random.nextFloat() * 360.0F;
            float pitch = (random.nextFloat() - 0.5F) * 70.0F;

            Entity gravestone = gravestoneType.create(level);
            if (gravestone == null) continue;

            gravestone.moveTo(x, y, z, yaw, pitch);
            gravestone.setNoGravity(true);
            gravestone.setInvulnerable(true);

            if (gravestone instanceof Mob mob) {
                mob.setNoAi(true);
            }

            if (gravestone instanceof LivingEntity living) {
                living.setYHeadRot(yaw);
            }

            markYutaDecoration(gravestone, casterId);
            level.addFreshEntity(gravestone);
        }

        // ------------------------------------------
        // 3. AWAJI KNOTS
        // ------------------------------------------

        EntityType<?> knotType =
                JujutsucraftModEntities.ENTITY_AWAJI_KNOT.get();

        for (int i = 0; i < YUTA_AWAJI_KNOTS; i++) {

            double angle = (Math.PI * 2.0 * i)
                    / YUTA_AWAJI_KNOTS;

            double x = centerX
                    + Math.cos(angle) * YUTA_KNOT_RADIUS;

            double z = centerZ
                    + Math.sin(angle) * YUTA_KNOT_RADIUS;

            // Tangential rotation, following the circle.
            float yaw = (float) Math.toDegrees(angle) + 90.0F;

            Entity knot = knotType.create(level);
            if (knot == null) continue;

            knot.moveTo(x, centerY, z, yaw, 0.0F);
            knot.setNoGravity(true);
            knot.setInvulnerable(true);

// Double the Awaji knot's size using Pehkui.
            ScaleTypes.BASE.getScaleData(knot).setScale(2.0F);

            if (knot instanceof Mob mob) {
                mob.setNoAi(true);
            }

            if (knot instanceof LivingEntity living) {
                living.setYHeadRot(yaw);
            }

            markYutaDecoration(knot, casterId);
            level.addFreshEntity(knot);
        }
    }

    private static void markYutaDecoration(
            Entity entity,
            UUID casterId
    ) {
        entity.getPersistentData().putBoolean(
                "abyss_domain_decoration",
                true
        );

        entity.getPersistentData().putUUID(
                "abyss_decoration_owner",
                casterId
        );

        // Compatibility with Jujutsucraft's original
        // decoration ownership conventions.
        entity.getPersistentData().putString(
                "OWNER_UUID",
                casterId.toString()
        );
    }

    private static EntityDataAccessor<Boolean>
    getYutaSwordDecorationAccessor() {
        try {
            Class<?> cls = Class.forName(
                    "net.mcreator.jujutsucraft.entity.EntityItemEntity"
            );

            Field field = cls.getDeclaredField(
                    "DATA_domain_decoration"
            );

            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            EntityDataAccessor<Boolean> accessor =
                    (EntityDataAccessor<Boolean>) field.get(null);

            return accessor;

        } catch (Exception e) {
            return null;
        }
    }

}
