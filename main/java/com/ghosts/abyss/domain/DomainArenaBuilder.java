package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class DomainArenaBuilder {

    private static BlockState outerState() {
        // OUTSIDE black sphere
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft", "jujutsu_barrier"));
        return b != null ? b.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
    }

    private static BlockState innerState() {
        // inner shell
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft", "domain_blue_sky"));
        return b != null ? b.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
    }

    private static BlockState floorState(double skillDomain) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft", "domain_grass"));
        return b != null ? b.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
    }

    /** Hollow outer r=64 (barrier), hollow inner r=63 (domain), floor disk at center Y. */
    public static void build(ServerLevel level, BlockPos center, double skillDomain) {
        BlockState outer = outerState();
        BlockState inner = innerState();
        BlockState floor = floorState(skillDomain);

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
                        level.setBlock(new BlockPos(cx + x, cy + y, cz + z), inner, 2);
                    }
                }
            }
        }

        int floorR = rIn;
        for (int x = -floorR; x <= floorR; x++) {
            for (int z = -floorR; z <= floorR; z++) {
                if (x * x + z * z <= floorR * floorR) {
                    level.setBlock(new BlockPos(cx + x, cy, cz + z), floor, 2);
                }
            }
        }
    }
}