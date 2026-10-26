package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class DomainOverworldShell {

    private DomainOverworldShell() {}

    private static BlockState barrier() {
        var b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("jujutsucraft", "jujutsu_barrier"));
        return b != null ? b.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
    }

    public static void place(ServerLevel level, Vec3 center, int radius) {
        BlockState outer = barrier();
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        double r = radius;

        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int y = -radius - 1; y <= radius + 1; y++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist <= r + 0.5 && dist >= r - 0.5) {
                        BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
                        if (level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, outer, 2);
                        }
                    }
                }
            }
        }
    }

    public static void clear(ServerLevel level, Vec3 center, int radius) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r2 = (radius + 2) * (radius + 2);

        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int y = -radius - 2; y <= radius + 2; y++) {
                for (int z = -radius - 2; z <= radius + 2; z++) {
                    if (x * x + y * y + z * z > r2) continue;
                    BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
                    var state = level.getBlockState(pos);
                    var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id != null && "jujutsucraft".equals(id.getNamespace())
                            && id.getPath().contains("barrier")) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}