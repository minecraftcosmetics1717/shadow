
package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DomainSpawnPositions {

    private DomainSpawnPositions() {}

    // How close players can return to the edge of
    // the original overworld sphere.
    private static final double RETURN_RADIUS =
            AbyssDomainCast.OVERWORLD_RADIUS - 4.0;

    // Where the initial domain caster should appear.
    public static Vec3 casterSpawn(DomainInstance inst) {
        double x = inst.arenaCenter.getX() + 0.5;
        double y = inst.arenaCenter.getY() + 1.1;
        double z = inst.arenaCenter.getZ() + 0.5;

        int skill = (int) Math.round(inst.skillDomain);


        switch (skill) {
            // Gojo — Infinite Void (2)
            // Megumi — Chimera Shadow Garden (6)
            // Choso — Domain Expansion (10)
            // Mahito — Self-Embodiment of Perfection (15)
            // Naoya — Time Cell Moon Palace (19)
            case 2, 6, 10, 15, 19 -> {
                z -= 30.0;
            }

            default -> {
                // All other domains spawn at the center.
            }
        }


        return new Vec3(x, y, z);
    }

    // Preserve where a captured entity was relative
    // to the original caster in the overworld.
    public static Vec3 capturedSpawn(
            DomainInstance inst,
            Vec3 overworldCaster,
            Vec3 capturedPosition
    ) {
        Vec3 caster = casterSpawn(inst);

        double dx = capturedPosition.x - overworldCaster.x;
        double dz = capturedPosition.z - overworldCaster.z;

        double distance = Math.sqrt(dx * dx + dz * dz);

        // Avoid placing an entity directly inside
        // the caster if both shared a position.
        if (distance < 0.001) {
            dx = 1.0;
            dz = 0.0;
            distance = 1.0;
        }

        // Keep the direction but use comfortable
        // spacing inside the larger domain arena.
        double spawnDistance = Mth.clamp(
                distance * 1.1,
                9.0,
                20.0
        );

        double x = caster.x + dx / distance * spawnDistance;
        double z = caster.z + dz / distance * spawnDistance;

        // Make sure the target remains well inside
        // the domain rather than near the walls.
        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        double rx = x - cx;
        double rz = z - cz;

        double radialDistance = Math.sqrt(rx * rx + rz * rz);
        double maxRadius = DomainDimension.INNER_RADIUS - 10.0;

        if (radialDistance > maxRadius) {
            double scale = maxRadius / radialDistance;
            x = cx + rx * scale;
            z = cz + rz * scale;
        }

        return new Vec3(x, caster.y, z);
    }

    // Return an entity to the corresponding region
    // of the overworld when the domain ends.
    public static Vec3 returnPosition(
            ServerLevel returnLevel,
            DomainInstance inst,
            LivingEntity entity
    ) {
        Vec3 original = inst.returnPositions.getOrDefault(
                entity.getUUID(),
                inst.overworldCenter
        );

        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        double dx = entity.getX() - cx;
        double dz = entity.getZ() - cz;

        double scale = RETURN_RADIUS
                / (DomainDimension.INNER_RADIUS - 3.0);

        dx *= scale;
        dz *= scale;

        double length = Math.sqrt(dx * dx + dz * dz);

        if (length > RETURN_RADIUS) {
            dx = dx / length * RETURN_RADIUS;
            dz = dz / length * RETURN_RADIUS;
        }

        double targetX = inst.overworldCenter.x + dx;
        double targetZ = inst.overworldCenter.z + dz;

        int baseY = Mth.floor(original.y);

        // Nearby positions to consider if the target
        // would put an entity inside terrain.
        int[][] offsets = {
                {0, 0},
                {1, 0}, {-1, 0},
                {0, 1}, {0, -1},
                {1, 1}, {-1, 1},
                {1, -1}, {-1, -1},
                {2, 0}, {-2, 0},
                {0, 2}, {0, -2}
        };

        for (int[] offset : offsets) {
            int bx = Mth.floor(targetX) + offset[0];
            int bz = Mth.floor(targetZ) + offset[1];

            double x = bx + 0.5;
            double z = bz + 0.5;

            returnLevel.getChunkAt(
                    new BlockPos(bx, baseY, bz)
            );

            for (int d = 0; d <= 16; d++) {
                int above = baseY + d;

                if (isSafe(returnLevel, entity, x, above, z)) {
                    return new Vec3(x, above, z);
                }

                if (d > 0) {
                    int below = baseY - d;

                    if (isSafe(returnLevel, entity, x, below, z)) {
                        return new Vec3(x, below, z);
                    }
                }
            }
        }

        // If no nearby safe place was found, fall
        // back to this entity's original position.
        return original;
    }

    public static Vec3 heavenlyEscapePosition(
            ServerLevel returnLevel,
            DomainInstance inst,
            LivingEntity entity
    ) {
        // Match the actual block-aligned overworld sphere.
        BlockPos sphereBlock =
                BlockPos.containing(inst.overworldCenter);

        double centerX = sphereBlock.getX() + 0.5;
        double centerZ = sphereBlock.getZ() + 0.5;

        // Preserve the direction the player is standing
        // relative to the center of the domain arena.
        double dx = entity.getX()
                - (inst.arenaCenter.getX() + 0.5);

        double dz = entity.getZ()
                - (inst.arenaCenter.getZ() + 0.5);

        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.001) {
            dx = 0.0;
            dz = 1.0;
            distance = 1.0;
        }

        dx /= distance;
        dz /= distance;

        Vec3 original = inst.returnPositions.getOrDefault(
                entity.getUUID(),
                inst.overworldCenter
        );

        int baseY = Mth.floor(original.y);

        // Start approximately 3 blocks outside the
        // black overworld barrier.
        double startRadius =
                AbyssDomainCast.OVERWORLD_RADIUS + 3.5;

        // Search progressively farther outward only
        // if the first location is obstructed.
        for (int outward = 0; outward <= 9; outward++) {

            double radius = startRadius + outward;

            double targetX = centerX + dx * radius;
            double targetZ = centerZ + dz * radius;

            // Use block centers for predictable collision.
            double x = Mth.floor(targetX) + 0.5;
            double z = Mth.floor(targetZ) + 0.5;

            // Ensure rounding did not move the candidate
            // back into the sphere.
            double horizontalDistanceSq =
                    (x - centerX) * (x - centerX)
                            + (z - centerZ) * (z - centerZ);

            double minimumRadius =
                    AbyssDomainCast.OVERWORLD_RADIUS + 2.5;

            if (horizontalDistanceSq
                    < minimumRadius * minimumRadius) {
                continue;
            }

            returnLevel.getChunkAt(
                    BlockPos.containing(x, baseY, z)
            );

            // Search for solid ground and enough
            // empty space to fit the player.
            for (int offset = 0; offset <= 24; offset++) {

                int above = baseY + offset;

                if (above < returnLevel.getMaxBuildHeight() - 2
                        && isSafe(returnLevel, entity, x, above, z)) {
                    return new Vec3(x, above, z);
                }

                if (offset > 0) {
                    int below = baseY - offset;

                    if (below > returnLevel.getMinBuildHeight()
                            && isSafe(returnLevel, entity, x, below, z)) {
                        return new Vec3(x, below, z);
                    }
                }
            }
        }

        // No safe escape destination was found.
        // Never fall back inside the active sphere.
        return null;
    }

    private static boolean isSafe(
            ServerLevel level,
            LivingEntity entity,
            double x,
            int y,
            double z
    ) {
        BlockPos feet = BlockPos.containing(x, y, z);
        BlockPos ground = feet.below();

        if (!level.getBlockState(ground).isFaceSturdy(
                level, ground, Direction.UP)) {
            return false;
        }

        int height = Math.max(2, Mth.ceil(entity.getBbHeight()));

        for (int i = 0; i < height; i++) {
            BlockPos check = feet.above(i);

            if (!level.getBlockState(check)
                    .getCollisionShape(level, check)
                    .isEmpty()) {
                return false;
            }

            if (!level.getFluidState(check).isEmpty()) {
                return false;
            }
        }

        double half = entity.getBbWidth() / 2.0;

        AABB box = new AABB(
                x - half, y, z - half,
                x + half, y + entity.getBbHeight(), z + half
        );

        return level.noCollision(entity, box);
    }
}
