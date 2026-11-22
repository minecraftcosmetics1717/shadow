
package com.ghosts.abyss.domain;

import com.ghosts.abyss.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class DagonDomainTerrain {

    private DagonDomainTerrain() {}

    public enum Region {
        GRASS,
        BEACH,
        WATER
    }

    // Approximate one-third bands across the
    // arena, oriented toward Dagon's assigned side.


    public static Region regionAt(
            int x,
            int z,
            DomainClashParticipants.Side side
    ) {
        double towardDagon = towardDagon(x, z, side);
        double sideways = sideways(x, z, side);

        double grassBoundary = grassBoundary(sideways);

        double third = DomainDimension.INNER_RADIUS / 3.0D;

        // Preserve the natural, irregular water shoreline.
        double waterCurve =
                Math.sin(sideways * 0.065D + 2.1D) * 7.0D
                        + Math.sin(sideways * 0.15D + 0.6D) * 3.0D
                        + Math.cos(sideways * 0.028D) * 2.5D;

        double waterBoundary = -third + waterCurve;

        if (towardDagon > grassBoundary) {
            return Region.GRASS;
        }

        if (towardDagon < waterBoundary) {
            return Region.WATER;
        }

        return Region.BEACH;
    }

    private static double towardDagon(
            int x,
            int z,
            DomainClashParticipants.Side side
    ) {
        return switch (side) {
            case NORTH -> -z;
            case SOUTH -> z;
            case WEST -> -x;
            case EAST -> x;
        };
    }

    private static double sideways(
            int x, int z,
            DomainClashParticipants.Side side) {
        return switch (side) {
            case NORTH, SOUTH -> x;
            case WEST, EAST -> z;};
    }

    private static double grassBoundary(double sideways) {
        double third = DomainDimension.INNER_RADIUS / 3.0D;

        double grassCurve =
                Math.sin(sideways * 0.075D) * 6.0D
                        + Math.sin(sideways * 0.17D + 1.5D) * 2.5D
                        + Math.cos(sideways * 0.035D + 0.8D) * 3.0D;

        return third + grassCurve;
    }


    public static BlockState floorAt(
            int x,
            int z,
            DomainClashParticipants.Side side
    ) {
        Region region = regionAt(x, z, side);

        // One-block-deep vanilla ocean.
        if (region == Region.WATER) {
            return Blocks.WATER.defaultBlockState();
        }

        // Grass patches mixed directly into the sand.
        if (hasGrassBlend(x, z, side)) {
            return ModBlocks.DOMAIN_GRASS.get()
                    .defaultBlockState();
        }

        // Sand remains beneath the raised grass
        // and throughout the rest of the beach.
        return ModBlocks.DOMAIN_SAND.get()
                .defaultBlockState();
    }

    public static boolean isDagonFloor(BlockState state) {
        return state.is(ModBlocks.DOMAIN_SAND.get())
                || state.is(ModBlocks.DOMAIN_GRASS.get())
                || state.is(Blocks.WATER);
    }


    // Used when building Dagon's complete domain.

    public static void placeColumn(
            ServerLevel level, BlockPos center, int x, int z) {
        Region region = regionAt(x, z, DomainClashParticipants.Side.NORTH);

        BlockPos floor = center.offset(x, 0, z);
        BlockPos below = floor.below();
        BlockPos above = floor.above();

        BlockState foundation = region == Region.WATER
                ? ModBlocks.DEEP_BLUE.get().defaultBlockState()
                : ModBlocks.DOMAIN_BARRIER.get().defaultBlockState();

        level.setBlock(below, foundation, 2);

        level.setBlock(floor, floorAt(x, z, DomainClashParticipants.Side.NORTH),2);

        if (region == Region.GRASS) {level.setBlock(above,ModBlocks.DOMAIN_GRASS.get().defaultBlockState(),2);}

        // Also generates short grass on beach grass patches.
        updateShortGrass(
                level, floor, x, z, DomainClashParticipants.Side.NORTH);}


    public static boolean hasGrassBlend(
            int x,
            int z,
            DomainClashParticipants.Side side
    ) {
        // Only blend grass into the beach section.
        if (regionAt(x, z, side) != Region.BEACH) {
            return false;
        }

        double forward = towardDagon(x, z, side);
        double lateral = sideways(x, z, side);

        // Distance into the beach from the grass boundary.
        double distance = grassBoundary(lateral) - forward;

        // Grass spreads up to 11 blocks into the beach.
        if (distance < 0.0D || distance > 11.0D) {
            return false;
        }

        // Create connected-looking irregular patches.
        // These are deterministic, not random every tick.
        double pattern =
                Math.sin(x * 0.52D) * 0.42D
                        + Math.cos(z * 0.49D) * 0.37D
                        + Math.sin(x * 0.21D + z * 0.31D) * 0.28D;

        // Close to the grass: lots of patches.
        // Farther into the beach: fewer patches.
        double threshold =
                -0.40D + (distance / 11.0D) * 1.25D;

        return pattern > threshold;
    }


    public static void updateExtraLayers(
            ServerLevel level,
            BlockPos floor,
            int x,
            int z,
            DomainClashParticipants.Participant winner
    ) {
        boolean dagonOwns =
                (int) Math.round(winner.skillDomain) == 8;

        Region region = dagonOwns
                ? regionAt(x, z, winner.side)
                : Region.BEACH;

        BlockPos below = floor.below();
        BlockPos above = floor.above();

        // Restore the correct foundation.
        BlockState desiredBelow =
                dagonOwns && region == Region.WATER
                        ? ModBlocks.DEEP_BLUE.get()
                        .defaultBlockState()
                        : ModBlocks.DOMAIN_BARRIER.get()
                        .defaultBlockState();

        if (!level.getBlockState(below).equals(desiredBelow)) {
            level.setBlock(below, desiredBelow, 2);
        }

        // Remove vegetation from territory Dagon no longer owns.
        if (!dagonOwns) {
            removeShortGrass(level, floor);
        }

        BlockState currentAbove = level.getBlockState(above);

        if (dagonOwns && region == Region.GRASS) {

            // Clear a short-grass plant before restoring
            // the raised solid grass block.
            if (currentAbove.is(ModBlocks.DOMAIN_SHORT_GRASS.get())) {
                level.setBlock(
                        above,
                        Blocks.AIR.defaultBlockState(),
                        2
                );
                currentAbove = Blocks.AIR.defaultBlockState();
            }

            if (currentAbove.isAir()) {
                level.setBlock(
                        above,
                        ModBlocks.DOMAIN_GRASS.get()
                                .defaultBlockState(),
                        2
                );
            }

        } else {

            // Remove the raised grass block when Dagon's
            // territory changes to beach/water or is lost.
            if (currentAbove.is(ModBlocks.DOMAIN_GRASS.get())) {
                level.setBlock(
                        above,
                        Blocks.AIR.defaultBlockState(),
                        2
                );
            }
        }

        // Restore vegetation only on Dagon's ground.
        if (dagonOwns) {
            updateShortGrass(
                    level,
                    floor,
                    x,
                    z,
                    winner.side
            );
        }
    }


    private static boolean shouldPlaceShortGrass(
            int x,
            int z,
            DomainClashParticipants.Side side
    ) {
        Region region = regionAt(x, z, side);

        // Grass only grows on the grassy region
        // or grass patches within the beach.
        boolean suitable = region == Region.GRASS
                || hasGrassBlend(x, z, side);

        if (!suitable) return false;

        // Deterministic distribution. The same spots
        // receive vegetation after rebuilding.
        double pattern =
                Math.sin(x * 1.73D + z * 0.91D)
                        * Math.cos(z * 2.17D - x * 0.63D);

        // More plants in the raised grass region.
        return region == Region.GRASS
                ? pattern > 0.18D
                : pattern > 0.48D;
    }

    private static void updateShortGrass(
            ServerLevel level,
            BlockPos floor,
            int x,
            int z,
            DomainClashParticipants.Side side
    ) {
        BlockPos lower = floor.above();
        BlockPos upper = floor.above(2);

        BlockPos expectedPos =
                regionAt(x, z, side) == Region.GRASS
                        ? floor.above(2)
                        : floor.above();

        boolean shouldExist =
                shouldPlaceShortGrass(x, z, side);

        boolean alreadyCorrect =
                shouldExist
                        ? level.getBlockState(expectedPos).is(ModBlocks.DOMAIN_SHORT_GRASS.get())
                        : !level.getBlockState(floor.above()).is(ModBlocks.DOMAIN_SHORT_GRASS.get())
                        && !level.getBlockState(floor.above(2)).is(ModBlocks.DOMAIN_SHORT_GRASS.get());

        if (alreadyCorrect) return;

        // Clear plants only, preserving solid terrain.
        if (level.getBlockState(lower).is(ModBlocks.DOMAIN_SHORT_GRASS.get())) {
            level.setBlock(lower, Blocks.AIR.defaultBlockState(), 2);
        }

        if (level.getBlockState(upper).is(ModBlocks.DOMAIN_SHORT_GRASS.get())) {
            level.setBlock(upper, Blocks.AIR.defaultBlockState(), 2);
        }

        if (!shouldPlaceShortGrass(x, z, side)) return;

        // Raised grass has plants at Y+2.
        // Beach grass patches have plants at Y+1.
        BlockPos plantPos = regionAt(x, z, side) == Region.GRASS
                ? upper
                : lower;

        BlockState plant = ModBlocks.DOMAIN_SHORT_GRASS.get().defaultBlockState();

        if (level.getBlockState(plantPos).isAir()
                && plant.canSurvive(level, plantPos)) {
            level.setBlock(plantPos, plant, 2);
        }
    }

    private static void removeShortGrass(
            ServerLevel level,
            BlockPos floor
    ) {
        for (int y = 1; y <= 2; y++) {
            BlockPos pos = floor.above(y);

            if (level.getBlockState(pos).is(ModBlocks.DOMAIN_SHORT_GRASS.get())) {
                level.setBlock(
                        pos,
                        Blocks.AIR.defaultBlockState(),
                        2
                );
            }
        }
    }

}
