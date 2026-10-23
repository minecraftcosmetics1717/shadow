package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class DomainArenaBuilder {

    private static BlockState block(String path, BlockState fallback) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft", path));
        return b != null ? b.defaultBlockState() : fallback;
    }

    private static BlockState barrier() {
        return block("jujutsu_barrier", Blocks.OBSIDIAN.defaultBlockState());
    }

    private static BlockState universe() {
        return block("block_universe", Blocks.BLACK_CONCRETE.defaultBlockState());
    }

    private static BlockState domainDefault() {
        return block("domain", Blocks.BLACK_CONCRETE.defaultBlockState());
    }

    public static BlockState floorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> block("block_red", Blocks.RED_CONCRETE.defaultBlockState());
            case 2 -> universe();
            case 5 -> block("block_gravel", Blocks.GRAVEL.defaultBlockState());
            case 6 -> universe();
            case 8 -> block("domain_sand", Blocks.SAND.defaultBlockState());
            case 9 -> block("domain_sand", Blocks.SAND.defaultBlockState());
            case 15 -> block("domain_bone", Blocks.BONE_BLOCK.defaultBlockState());
            case 19 -> universe();
            case 27 -> block("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());
            case 29 -> block("domain_white", Blocks.WHITE_CONCRETE.defaultBlockState());
            default -> domainDefault();
        };
    }

    public static BlockState interiorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> universe();
            case 2 -> universe();
            case 5 -> universe();
            case 6 -> universe();
            case 8 -> block("domain_blue_sky", Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());
            case 9 -> universe();
            case 15 -> block("domain_bone", Blocks.BONE_BLOCK.defaultBlockState());
            case 19 -> universe();
            case 27 -> universe();
            case 29 -> block("domain_white", Blocks.WHITE_CONCRETE.defaultBlockState());
            default -> domainDefault();
        };
    }

    public static void build(ServerLevel level, BlockPos center, double skillDomain) {
        DomainDimBlockGuard.open();
        try {
            BlockState outer = barrier();
            BlockState interior = interiorState(skillDomain);

            int cx = center.getX();
            int cy = center.getY();
            int cz = center.getZ();
            int rOut = DomainDimension.OUTER_RADIUS;
            int rIn = DomainDimension.INNER_RADIUS;

            for (int x = -rOut; x <= rOut; x++) {
                for (int y = -rOut; y <= rOut; y++) {
                    for (int z = -rOut; z <= rOut; z++) {
                        double dist = Math.sqrt(x * x + y * y + z * z);
                        if (dist <= rOut + 0.5 && dist >= rOut - 0.5) {
                            level.setBlock(new BlockPos(cx + x, cy + y, cz + z), outer, 2);
                        } else if (dist <= rIn + 0.5 && dist >= rIn - 0.5) {
                            level.setBlock(new BlockPos(cx + x, cy + y, cz + z), interior, 2);
                        }
                    }
                }
            }

            placeFloorInternal(level, center, skillDomain);
        } finally {
            DomainDimBlockGuard.close();
        }
    }

    public static void placeFloor(ServerLevel level, BlockPos center, double skillDomain) {
        DomainDimBlockGuard.open();
        try {
            placeFloorInternal(level, center, skillDomain);
        } finally {
            DomainDimBlockGuard.close();
        }
    }

    private static void placeFloorInternal(ServerLevel level, BlockPos center, double skillDomain) {
        BlockState under = barrier();
        BlockState floor = floorState(skillDomain);
        BlockState edgeA = block("domain_stone_bricks", Blocks.STONE_BRICKS.defaultBlockState());
        BlockState edgeB = block("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int floorR = DomainDimension.INNER_RADIUS;
        int id = (int) Math.round(skillDomain);
        int edgeWidth = 4;

        for (int x = -floorR; x <= floorR; x++) {
            for (int z = -floorR; z <= floorR; z++) {
                if (x * x + z * z > floorR * floorR) continue;

                level.setBlock(new BlockPos(cx + x, cy - 1, cz + z), under, 2);

                BlockState top = floor;
                if (id == 27) {
                    int ax = Math.abs(x);
                    int az = Math.abs(z);
                    boolean onRim = ax >= floorR - edgeWidth || az >= floorR - edgeWidth;
                    if (onRim) {
                        top = ((x + z) & 1) == 0 ? edgeA : edgeB;
                    } else {
                        top = edgeB;
                    }
                }

                level.setBlock(new BlockPos(cx + x, cy, cz + z), top, 2);
            }
        }
    }

    public static void repairFloorCenter(ServerLevel level, BlockPos center, double skillDomain, int radius) {
        DomainDimBlockGuard.open();
        try {
            BlockState under = barrier();
            BlockState floor = floorState(skillDomain);
            int cx = center.getX();
            int cy = center.getY();
            int cz = center.getZ();
            int r = Math.max(1, radius);

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z > r * r) continue;
                    BlockPos underPos = new BlockPos(cx + x, cy - 1, cz + z);
                    BlockPos floorPos = new BlockPos(cx + x, cy, cz + z);
                    level.setBlock(underPos, under, 2);
                    level.setBlock(floorPos, floor, 2);
                }
            }
        } finally {
            DomainDimBlockGuard.close();
        }
    }
}