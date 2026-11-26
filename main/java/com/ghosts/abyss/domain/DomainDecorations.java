package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.tags.TagKey;
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

    public static void spawnParticipant(ServerLevel level, DomainInstance inst, DomainClashParticipants.Participant participant) {
        if (level == null || inst == null || participant == null) return;
// Sukuna — Malevolent Shrine.
// Centered when alone; moved to the caster's
// assigned side whenever domains are clashing.
        if ((int) participant.skillDomain == 1) {
            syncSukunaShrine(level, inst, participant);
            return;
        }
// Yuta has multiple interactive decorations, so he needs
// a separate one-time spawning system.
        if ((int) participant.skillDomain == 5) {
            spawnYutaDecorations(level, inst, participant);
            return;
        }
// Reposition Uro's existing centerpiece when
// a clash starts or ends.
        if ((int) participant.skillDomain == 38) {
            updateUroDecorationPosition(level, inst, participant);
        }

// Dagon — Horizon of the Captivating Skies.
// Multiple palm trees on the grassy side.
        if ((int) participant.skillDomain == 8) {
            spawnDagonPalmTrees(level, inst, participant);
            return;
        }

        if (hasDecoration(level, inst, participant.entityId)) return;

        // Higuruma — Deadly Sentencing.
        if ((int) participant.skillDomain == 27) {
            spawnHigurumaDecorations(level, inst, participant);
            return;
        }
// Ryu Ishigori — All-Satiating Flavor Festival
        if ((int) participant.skillDomain == 12) {
            RyuDomainDecorations.spawnInitial(
                    level,
                    inst,
                    participant);
            return;
        }

// Yorozu has 12 separate decorations.
        if ((int) participant.skillDomain == 39) {
            spawnYorozuDecorations(level, inst, participant);
            return;
        }
        // Hakari — Idle Death Gamble
        if ((int) participant.skillDomain == 29) {
            spawnHakariGates(level, inst, participant);
            return;
        }

        String entityId = switch ((int) participant.skillDomain) {
            case 2 -> "jujutsucraft:entity_unlimited_void";
            case 6 -> "jujutsucraft:entity_chimera_shadow_garden";
            case 9 -> "jujutsucraft:entity_jupiter";
            case 10 -> "jujutsucraft:entity_heart";
            case 15 -> "jujutsucraft:entity_hands";
            case 18 -> "jujutsucraft:entity_womb_profusion";
            case 19 -> "jujutsucraft:entity_time_cell_moon_palace";
            case 38 -> "jujutsucraft:entity_domain_uro";
            default -> null;
        };
        if (entityId == null) return;

        double distance = switch ((int) participant.skillDomain) {
            case 2 -> 56.0;
            case 6 -> 52.0;
            case 9 -> 42.0;
            case 10 -> 51.0;
            case 15 -> 57.0;
            case 18 -> inst.participants.size() > 1 ? 52.0D : 0.0D;
            case 19 -> 43.0;
            case 38 -> inst.participants.size() > 1 ? DomainDimension.INNER_RADIUS * 0.5D : 0.0D;
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
            case 6 -> 1.0;  // Chimera Shadow Garden
            case 9 -> 6.0; // Jupiter elevated above the floor
            case 10 -> 2.0;
            case 15 -> 1.0; // Mahito (unchanged)
            case 18 -> 1.0D;
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

    private static void spawnYorozuDecorations(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant
    ) {
        if (level == null || inst == null
                || participant == null) {
            return;
        }

        // Match JC's original quantity.
        final int decorationCount = 12;

        // Keep the decorations inside the Abyss arena.
        final double minDistance = 24.0D;
        final double maxDistance = 53.0D;

        // Position the decorations around the caster's
        // side of the arena during domain clashes.
        double sideAngle = switch (participant.side) {
            case NORTH -> Math.PI;
            case SOUTH -> 0.0D;
            case WEST -> Math.PI * 1.5D;
            case EAST -> Math.PI * 0.5D;
        };

        for (int i = 0; i < decorationCount; i++) {

            double angle =
                    sideAngle
                            + (Math.PI * 2.0D * i / decorationCount)
                            + (level.random.nextDouble() - 0.5D)
                            * 0.25D;

            double distance =
                    minDistance
                            + level.random.nextDouble()
                            * (maxDistance - minDistance);

            double offsetX = Math.sin(angle) * distance;
            double offsetZ = Math.cos(angle) * distance;

            float yaw = level.random.nextFloat() * 360.0F;

            spawnDecoration(
                    level,
                    inst,
                    "jujutsucraft:entity_threefold_affliction",
                    offsetX,
                    3.0D,
                    offsetZ,
                    yaw,
                    participant.entityId);
        }
    }

    private static boolean hasDecoration(ServerLevel level, DomainInstance inst, UUID casterId) {
        return !level.getEntities((Entity) null, arenaBox(inst), entity ->
                entity.getPersistentData().getBoolean("abyss_domain_decoration")
                        && entity.getPersistentData().hasUUID("abyss_decoration_owner")
                        && casterId.equals(entity.getPersistentData().getUUID("abyss_decoration_owner"))).isEmpty();
    }

    public static void sync(ServerLevel level, DomainInstance inst) {
        if (level == null || inst == null) return;
        cleanupDefeatedMegumiSummons(level, inst);
        inst.yutaDecorationsSpawned.removeIf(
                casterId -> !inst.participants.contains(casterId));
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
            ServerLevel level) {
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
            DomainInstance inst) {
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
                    0.0);
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
                    0.0);
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
                    0.0);
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
                    0.0);
        }
    }

    /**
     * Removes decorations belonging to this arena,
     * without affecting players, mobs, or other arenas.
     */

    public static void clear(ServerLevel level, DomainInstance inst) {
        if (level == null || inst == null) return;

        // Remove all existing decorations in this arena.
        for (Entity entity : level.getEntities(
                (Entity) null,
                arenaBox(inst),
                e -> e.getPersistentData()
                        .getBoolean("abyss_domain_decoration"))) {
            entity.discard();
        }

        // Allow Yuta's decorations to be generated again
        // after the arena is rebuilt following a clash.
        inst.yutaDecorationsSpawned.clear();
    }


    private static void spawnDecoration(
            ServerLevel level,
            DomainInstance inst,
            String entityId,
            double offsetX,
            double offsetY,
            double offsetZ,
            float yaw,
            UUID casterId) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(entityId));

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
                || "jujutsucraft:entity_unlimited_void".equals(entityId)
                || "jujutsucraft:entity_chimera_shadow_garden".equals(entityId)
                || "jujutsucraft:entity_heart".equals(entityId)) {


            // Mahito, Gojo, and Megumi decorations are 3x.
            ScaleTypes.BASE.getScaleData(entity).setScale(3.0F);

        } else if ("jujutsucraft:ticket_gate".equals(entityId)) {

            // Hakari's ticket gates remain 2x.
            ScaleTypes.BASE.getScaleData(entity).setScale(2.0F);

        } else if ("jujutsucraft:entity_guillotine".equals(entityId)
                || "jujutsucraft:judgeman".equals(entityId)) {

            // Higuruma's courtroom decorations are 2x.
            ScaleTypes.BASE.getScaleData(entity).setScale(2.0F);
        }

        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        entity.getPersistentData().putBoolean(
                "abyss_domain_decoration", true);

        entity.getPersistentData().putUUID("abyss_decoration_owner", casterId);
        level.addFreshEntity(entity);
    }

    private static void spawnHigurumaDecorations(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant) {
        final int decorationCount = 20;
        final double radius = 48.0D;

        for (int i = 0; i < decorationCount; i++) {
            double angle = Math.PI * 2.0D * i / decorationCount;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            float yaw = (float) Math.toDegrees(angle) + 90.0F;

            spawnDecoration(level, inst,
                    "jujutsucraft:entity_guillotine",
                    offsetX, 1.0D, offsetZ, yaw,
                    participant.entityId);
        }

        double judgeX = 0.0D;
        double judgeZ = 0.0D;
        float judgeYaw = 0.0F;

        switch (participant.side) {
            case NORTH -> { judgeZ = -18.0D; judgeYaw = 0.0F; }
            case SOUTH -> { judgeZ = 18.0D; judgeYaw = 180.0F; }
            case WEST -> { judgeX = -18.0D; judgeYaw = 270.0F; }
            case EAST -> { judgeX = 18.0D; judgeYaw = 90.0F; }
        }

        spawnDecoration(level, inst, "jujutsucraft:judgeman",
                judgeX, 1.0D, judgeZ, judgeYaw,
                participant.entityId);
    }

    private static void spawnHakariGates(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant) {
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
                        participant.entityId);
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
            DomainClashParticipants.Participant participant) {
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
                        "sword_okkotsu_yuta"));

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
                            new ItemStack(swordItem));
                }

                try {
                    sword.getEntityData().set(
                            swordDecorationAccessor,
                            true);
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
            UUID casterId) {
        entity.getPersistentData().putBoolean(
                "abyss_domain_decoration",
                true
        );

        entity.getPersistentData().putUUID(
                "abyss_decoration_owner",
                casterId);

        // Compatibility with Jujutsucraft's original
        // decoration ownership conventions.
        entity.getPersistentData().putString(
                "OWNER_UUID",
                casterId.toString());
    }

    private static EntityDataAccessor<Boolean>
    getYutaSwordDecorationAccessor() {
        try {
            Class<?> cls = Class.forName(
                    "net.mcreator.jujutsucraft.entity.EntityItemEntity");

            Field field = cls.getDeclaredField(
                    "DATA_domain_decoration");

            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            EntityDataAccessor<Boolean> accessor =
                    (EntityDataAccessor<Boolean>) field.get(null);

            return accessor;

        } catch (Exception e) {
            return null;
        }
    }

    private static void cleanupDefeatedMegumiSummons(
            ServerLevel level,
            DomainInstance inst) {
        TagKey<EntityType<?>> tenShadowsTag = TagKey.create(
                Registries.ENTITY_TYPE,
                new ResourceLocation(
                        "jujutsucraft",
                        "ten_shadows_technique"));

        for (Entity summoned : level.getEntities(
                (Entity) null,
                arenaBox(inst),
                e -> e.getPersistentData()
                        .getBoolean("domain_entity")
                        && e.getType().is(tenShadowsTag))) {

            String ownerUuid = summoned.getPersistentData()
                    .getString("OWNER_UUID");

            if (ownerUuid.isEmpty()) continue;

            boolean ownerStillParticipating = false;

            for (DomainClashParticipants.Participant participant
                    : inst.participants.all()) {

                if (participant.skillDomain == 6.0D
                        && participant.entityId.toString()
                        .equals(ownerUuid)) {

                    ownerStillParticipating = true;
                    break;
                }
            }

            // The Megumi who created this domain summon
            // is no longer participating in the arena.
            if (!ownerStillParticipating) {
                summoned.discard();
            }
        }
    }

    private static void updateUroDecorationPosition(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant) {
        // Center when alone, halfway to the wall
        // on Uro's side during clashes.
        double distance = inst.participants.size() > 1
                ? DomainDimension.INNER_RADIUS * 0.5D
                : 0.0D;

        double x = inst.arenaCenter.getX() + 0.5D;
        double y = inst.arenaCenter.getY() + 1.0D;
        double z = inst.arenaCenter.getZ() + 0.5D;

        switch (participant.side) {
            case NORTH -> z -= distance;
            case SOUTH -> z += distance;
            case WEST -> x -= distance;
            case EAST -> x += distance;
        }

        for (Entity entity : level.getEntities(
                (Entity) null,
                arenaBox(inst),
                e -> e.getType() ==
                        JujutsucraftModEntities.ENTITY_DOMAIN_URO.get()
                        && e.getPersistentData()
                        .getBoolean("abyss_domain_decoration")
                        && e.getPersistentData()
                        .hasUUID("abyss_decoration_owner")
                        && participant.entityId.equals(
                        e.getPersistentData()
                                .getUUID("abyss_decoration_owner")))) {
            // Avoid unnecessary position updates.
            if (entity.distanceToSqr(x, y, z) > 0.01D) {
                entity.setPos(x, y, z);
            }
        }
    }

    private static void spawnDagonPalmTrees(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant) {
        final int TREE_COUNT = 16;
        final double MIN_SPACING = 8.0D;

        // Fixed positions so the trees don't randomly
        // move every time the domain rebuilds.
        RandomSource random = RandomSource.create(820816L);

        java.util.List<BlockPos> positions =
                new java.util.ArrayList<>();

        // Generate points on Dagon's grassy side.
        for (int attempt = 0;
             attempt < 500 && positions.size() < TREE_COUNT;
             attempt++) {

            double forward = 29.0D
                    + random.nextDouble() * 24.0D;

            double lateral = -44.0D
                    + random.nextDouble() * 88.0D;

            int x;
            int z;

            switch (participant.side) {
                case NORTH -> {
                    x = (int) Math.round(lateral);
                    z = (int) Math.round(-forward);
                }
                case SOUTH -> {
                    x = (int) Math.round(-lateral);
                    z = (int) Math.round(forward);
                }
                case WEST -> {
                    x = (int) Math.round(-forward);
                    z = (int) Math.round(-lateral);
                }
                case EAST -> {
                    x = (int) Math.round(forward);
                    z = (int) Math.round(lateral);
                }
                default -> {
                    continue;
                }
            }

            // Stay away from the spherical wall.
            if (x * x + z * z > 55 * 55) {
                continue;
            }

            // Palm trees only belong in raised grass.
            if (DagonDomainTerrain.regionAt(
                    x, z, participant.side) != DagonDomainTerrain.Region.GRASS) {
                continue;
            }

            boolean tooClose = false;

            for (BlockPos other : positions) {
                double dx = x - other.getX();
                double dz = z - other.getZ();

                if (dx * dx + dz * dz
                        < MIN_SPACING * MIN_SPACING) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                positions.add(new BlockPos(x, 0, z));
            }
        }

        // Find existing trees belonging to Dagon.
        // This prevents duplicates on the 4-tick sync.
        java.util.Map<Integer, Entity> existing =
                new java.util.HashMap<>();

        for (Entity entity : level.getEntities(
                (Entity) null,
                arenaBox(inst),
                e -> e.getPersistentData()
                        .getBoolean("abyss_domain_decoration")
                        && e.getType()
                        == JujutsucraftModEntities.PALM_TREE.get()
                        && e.getPersistentData()
                        .hasUUID("abyss_decoration_owner")
                        && participant.entityId.equals(
                        e.getPersistentData()
                                .getUUID("abyss_decoration_owner")))) {
            int index = entity.getPersistentData()
                    .getInt("abyss_dagon_palm_index");

            if (existing.containsKey(index)) {
                entity.discard();
            } else {
                existing.put(index, entity);
            }
        }

        for (int i = 0; i < positions.size(); i++) {
            BlockPos offset = positions.get(i);

            BlockPos grassPos = inst.arenaCenter.offset(
                    offset.getX(),
                    1,
                    offset.getZ());

            BlockPos sandPos = grassPos.below();

            // Only keep trees while their raised grass
            // and supporting sand still exist.
            boolean hasGrass = level.getBlockState(grassPos)
                    .is(com.ghosts.abyss.block.ModBlocks
                            .DOMAIN_GRASS.get());

            boolean hasSand = level.getBlockState(sandPos)
                    .is(com.ghosts.abyss.block.ModBlocks
                            .DOMAIN_SAND.get());

            boolean validGround = hasGrass && hasSand;

            Entity oldTree = existing.get(i);

            if (!validGround) {
                if (oldTree != null) {
                    oldTree.discard();
                }
                continue;
            }

            if (oldTree != null && !oldTree.isRemoved()) {
                continue;
            }

            Entity tree = JujutsucraftModEntities
                    .PALM_TREE.get().create(level);

            if (tree == null) continue;

            double px = inst.arenaCenter.getX()
                    + 0.5D + offset.getX();

            double py = inst.arenaCenter.getY() + 2.0D;

            double pz = inst.arenaCenter.getZ()
                    + 0.5D + offset.getZ();

            float yaw = random.nextFloat() * 360.0F;

            tree.moveTo(px, py, pz, yaw, 0.0F);
            tree.setInvulnerable(true);
            tree.setNoGravity(true);

            if (tree instanceof Mob mob) {
                mob.setNoAi(true);
            }

            if (tree instanceof LivingEntity living) {
                living.setYHeadRot(yaw);
            }

            tree.getPersistentData().putBoolean(
                    "abyss_domain_decoration",
                    true);

            tree.getPersistentData().putUUID(
                    "abyss_decoration_owner",
                    participant.entityId);

            tree.getPersistentData().putInt(
                    "abyss_dagon_palm_index",
                    i);

            // Keep compatibility with the owner data
            // used by Jujutsucraft decorations.
            tree.getPersistentData().putString(
                    "OWNER_UUID",
                    participant.entityId.toString());

            level.addFreshEntity(tree);
        }
    }

    private static void syncSukunaShrine(
            ServerLevel level,
            DomainInstance inst,
            DomainClashParticipants.Participant participant) {
        // Put the shrine in the center when there is
        // only one domain in the arena.
        //
        // During a clash, position it toward
        // its owner's side, close to the wall.
        double distance = inst.participants.size() > 1
                ? 54.0D
                : 0.0D;

        double x = inst.arenaCenter.getX() + 0.5D;
        double y = inst.arenaCenter.getY() + 1.0D;
        double z = inst.arenaCenter.getZ() + 0.5D;

        float yaw = 0.0F;

        switch (participant.side) {
            case NORTH -> {
                z -= distance;
                yaw = 0.0F;
            }
            case SOUTH -> {
                z += distance;
                yaw = 180.0F;
            }
            case WEST -> {
                x -= distance;
                yaw = 90.0F;
            }
            case EAST -> {
                x += distance;
                yaw = 270.0F;
            }
        }

        // Find an existing Abyss-owned shrine.
        Entity existing = null;

        for (Entity entity : level.getEntities(
                (Entity) null,
                arenaBox(inst),
                e -> e.getPersistentData()
                        .getBoolean("abyss_sukuna_shrine")
                        && e.getPersistentData()
                        .hasUUID("abyss_decoration_owner")
                        && participant.entityId.equals(
                        e.getPersistentData()
                                .getUUID("abyss_decoration_owner")))) {
            if (existing == null) {
                existing = entity;
            } else {
                // Prevent duplicate shrines.
                entity.discard();
            }
        }

        if (existing != null) {
            ScaleTypes.BASE.getScaleData(existing).setScale(3.0F);
            // Reposition the same shrine during clashes
            // instead of spawning a new one.
            if (existing.distanceToSqr(x, y, z) > 0.01D) {
                existing.setPos(x, y, z);
            }

            existing.setYRot(yaw);
            if (existing instanceof net.minecraft.world.entity.LivingEntity living) {
                living.setYHeadRot(yaw);
                living.setYBodyRot(yaw);
            }

            return;
        }

        // Create the standard Jujutsucraft shrine.
        Entity shrine =
                JujutsucraftModEntities
                        .ENTITY_MALEVOLENT_SHRINE
                        .get()
                        .create(level);

        if (shrine == null) return;

        shrine.moveTo(x, y, z, yaw, 0.0F);
        shrine.setInvulnerable(true);
        shrine.setNoGravity(true);

        ScaleTypes.BASE.getScaleData(shrine).setScale(3.0F);


        if (shrine instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(true);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
        }

        // Allow Abyss's existing decoration cleanup
        // to track this shrine.
        shrine.getPersistentData().putBoolean(
                "abyss_domain_decoration",
                true);

        shrine.getPersistentData().putBoolean(
                "abyss_sukuna_shrine",
                true);

        shrine.getPersistentData().putUUID(
                "abyss_decoration_owner",
                participant.entityId);

        level.addFreshEntity(shrine);
    }

}
