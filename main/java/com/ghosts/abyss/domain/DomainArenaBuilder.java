package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class DomainArenaBuilder {

    private static BlockState block(String namespace, String path, BlockState fallback) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path));
        return b != null ? b.defaultBlockState() : fallback;
    }

    private static BlockState jc(String path, BlockState fallback) {
        return block("jujutsucraft", path, fallback);
    }

    private static BlockState jcv(String path, BlockState fallback) {
        return block("jujutsucraftv", path, fallback);
    }

    private static BlockState barrier() {
        return jc("jujutsu_barrier", Blocks.OBSIDIAN.defaultBlockState());
    }

    private static BlockState universe() {
        return jc("block_universe", Blocks.BLACK_CONCRETE.defaultBlockState());
    }

    private static BlockState domainDefault() {
        return jc("domain", Blocks.BLACK_CONCRETE.defaultBlockState());
    }

    /** Walkable floor for this skill_domain */
    public static BlockState floorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> jc("block_red", Blocks.RED_CONCRETE.defaultBlockState());              // Sukuna
            case 2 -> universe();                                                             // Gojo
            case 5 -> jc("block_gravel", Blocks.GRAVEL.defaultBlockState());                 // Yuta
            case 6 -> universe();                                                             // Megumi
            case 8 -> jc("domain_sand", Blocks.SAND.defaultBlockState());                    // Dagon
            case 9 -> jc("domain_sand", Blocks.SAND.defaultBlockState());                    // Yuki
            case 14 -> jc("domain_flower", Blocks.PINK_CONCRETE.defaultBlockState());        // Hanami
            case 15 -> jc("domain_bone", Blocks.BONE_BLOCK.defaultBlockState());             // Mahito
            case 19 -> universe();                                                            // Naoya
            case 27 -> jc("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());           // Higuruma
            case 29 -> jc("domain_white", Blocks.WHITE_CONCRETE.defaultBlockState());        // Hakari
            case 613 -> jcv("domain_rock_1", Blocks.STONE.defaultBlockState());              // Whack-a-Mole
            default -> domainDefault();
        };
    }

    /** Inner shell (walls/ceiling) for this skill_domain */
    public static BlockState interiorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> universe();                                                             // Sukuna
            case 2 -> universe();                                                             // Gojo
            case 5 -> universe();                                                             // Yuta
            case 6 -> universe();                                                             // Megumi
            case 8 -> jc("domain_blue_sky", Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState());  // Dagon
            case 9 -> universe();                                                             // Yuki
            case 14 -> jc("domain_blue_sky", Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()); // Hanami
            case 15 -> jc("domain_bone", Blocks.BONE_BLOCK.defaultBlockState());             // Mahito
            case 19 -> universe();                                                            // Naoya
            case 27 -> universe();                                                            // Higuruma
            case 29 -> jc("domain_white", Blocks.WHITE_CONCRETE.defaultBlockState());        // Hakari
            case 613 -> jcv("domain_rock_1", Blocks.STONE.defaultBlockState());              // Whack-a-Mole
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
        BlockState edgeA = jc("domain_stone_bricks", Blocks.STONE_BRICKS.defaultBlockState());
        BlockState edgeB = jc("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());

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
                    top = onRim ? (((x + z) & 1) == 0 ? edgeA : edgeB) : edgeB;
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
                    level.setBlock(new BlockPos(cx + x, cy - 1, cz + z), under, 2);
                    level.setBlock(new BlockPos(cx + x, cy, cz + z), floor, 2);
                }
            }
        } finally {
            DomainDimBlockGuard.close();
        }
    }

    /**
     * Places a thin jujutsu_barrier outer shell in the OVERWORLD at JC domain radius.
     * Used for domains (e.g. Whack-a-Mole) that only place interior V blocks.
     */
    public static void ensureOverworldOuterShell(ServerLevel overworld, Vec3 center, int radius) {
        if (overworld == null || center == null) return;
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
                        // Don't overwrite solid non-domain terrain heavily; prefer air / V domain blocks
                        var state = overworld.getBlockState(pos);
                        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                        boolean replace = state.isAir()
                                || (id != null && ("jujutsucraftv".equals(id.getNamespace())
                                || "jujutsucraft".equals(id.getNamespace())));
                        if (replace) {
                            overworld.setBlock(pos, outer, 2);
                        }
                    }
                }
            }
        }
    }
}