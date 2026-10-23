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

    /** Walkable floor block for this skill_domain */
    public static BlockState floorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> block("block_red", Blocks.RED_CONCRETE.defaultBlockState());           // Sukuna
            case 2 -> universe();                                                            // Gojo
            case 5 -> block("block_gravel", Blocks.GRAVEL.defaultBlockState());              // Yuta
            case 6 -> universe();                                                            // Megumi
            case 8 -> block("domain_sand", Blocks.SAND.defaultBlockState());                 // Dagon
            case 9 -> block("domain_sand", Blocks.SAND.defaultBlockState());                 // Yuki
            case 15 -> block("domain_bone", Blocks.BONE_BLOCK.defaultBlockState());          // Mahito
            case 19 -> universe();                                                           // Naoya
            case 27 -> block("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());        // Higuruma (inner floor)
            case 29 -> block("domain_white", Blocks.WHITE_CONCRETE.defaultBlockState());     // Hakari
            // Yorozu / Uro — often universe or white; adjust ids if yours differ
            default -> domainDefault();
        };
    }

    /** Inner shell (walls/ceiling ring) for this skill_domain */
    public static BlockState interiorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> universe();                                                            // Sukuna
            case 2 -> universe();                                                            // Gojo
            case 5 -> universe();                                                            // Yuta
            case 6 -> universe();                                                            // Megumi
            case 8 -> block("domain_blue_sky", Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()); // Dagon
            case 9 -> universe();                                                            // Yuki
            case 15 -> block("domain_bone", Blocks.BONE_BLOCK.defaultBlockState());          // Mahito
            case 19 -> universe();                                                           // Naoya
            case 27 -> universe();                                                           // Higuruma
            case 29 -> block("domain_white", Blocks.WHITE_CONCRETE.defaultBlockState());     // Hakari
            default -> domainDefault();
        };
    }

    public static void build(ServerLevel level, BlockPos center, double skillDomain) {
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

        placeFloor(level, center, skillDomain);
    }

    public static void placeFloor(ServerLevel level, BlockPos center, double skillDomain) {
        BlockState under = barrier();
        BlockState floor = floorState(skillDomain);
        BlockState edgeA = block("domain_stone_bricks", Blocks.STONE_BRICKS.defaultBlockState());
        BlockState edgeB = block("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int floorR = DomainDimension.INNER_RADIUS;
        int id = (int) Math.round(skillDomain);

        // Higuruma: outer ~4-block rim stone_bricks/planks, inner planks
        int edgeWidth = 4;

        for (int x = -floorR; x <= floorR; x++) {
            for (int z = -floorR; z <= floorR; z++) {
                if (x * x + z * z > floorR * floorR) continue;

                level.setBlock(new BlockPos(cx + x, cy - 1, cz + z), under, 2);

                BlockState top = floor;
                if (id == 27) { // Higuruma courtroom rim
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
        BlockState under = barrier();
        BlockState floor = floorState(skillDomain);

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int r = Math.max(1, radius);
        int id = (int) Math.round(skillDomain);

        BlockState edgeA = block("domain_stone_bricks", Blocks.STONE_BRICKS.defaultBlockState());
        BlockState edgeB = block("domain_planks", Blocks.OAK_PLANKS.defaultBlockState());
        int floorR = DomainDimension.INNER_RADIUS;
        int edgeWidth = 4;

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;

                BlockPos underPos = new BlockPos(cx + x, cy - 1, cz + z);
                BlockPos floorPos = new BlockPos(cx + x, cy, cz + z);

                // Always keep underlayer
                if (!level.getBlockState(underPos).is(under.getBlock())) {
                    level.setBlock(underPos, under, 2);
                }

                BlockState top = floor;
                if (id == 27) {
                    int ax = Math.abs(x);
                    int az = Math.abs(z);
                    // rim relative to full floor only matters near edge; for center repair use plain floor
                    boolean onRim = (Math.abs(cx + x - cx) >= floorR - edgeWidth)
                            || (Math.abs(cz + z - cz) >= floorR - edgeWidth);
                    // For center repair radius << floorR, onRim is false — just use planks/floor
                    top = edgeB;
                }

                // FORCE restamp top — JC may have placed domain_hole or air
                level.setBlock(floorPos, top, 2);
            }
        }
    }}