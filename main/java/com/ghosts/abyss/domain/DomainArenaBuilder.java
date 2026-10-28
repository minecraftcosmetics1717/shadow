package com.ghosts.abyss.domain;

import com.ghosts.abyss.block.ModBlocks;
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

    /** Abyss domain dim blocks */
    private static BlockState abyssBarrier() {
        return ModBlocks.DOMAIN_BARRIER.get().defaultBlockState();
    }

    private static BlockState abyssSand() {
        return ModBlocks.DOMAIN_SAND.get().defaultBlockState();
    }

    private static BlockState abyssGravel() {
        return ModBlocks.DOMAIN_GRAVEL.get().defaultBlockState();
    }

    private static BlockState abyssRed() {
        return ModBlocks.DOMAIN_RED.get().defaultBlockState();
    }

    private static BlockState abyssFlower() {
        return ModBlocks.DOMAIN_FLOWER.get().defaultBlockState();
    }

    private static BlockState abyssBone() {
        return ModBlocks.DOMAIN_BONE.get().defaultBlockState();
    }

    private static BlockState abyssBirch() {
        return ModBlocks.DOMAIN_BIRCH.get().defaultBlockState();
    }

    private static BlockState abyssWhite() {
        return ModBlocks.DOMAIN_WHITE.get().defaultBlockState();
    }

    private static BlockState abyssBlueSky() {
        return ModBlocks.DOMAIN_BLUE_SKY.get().defaultBlockState();
    }

    private static BlockState abyssBlack() {
        return ModBlocks.DOMAIN_BLACK.get().defaultBlockState();
    }

    private static BlockState abyssRock() {
        return ModBlocks.DOMAIN_ROCK.get().defaultBlockState();
    }

    /** JC barrier — overworld only */
    private static BlockState jcBarrier() {
        return jc("jujutsu_barrier", Blocks.OBSIDIAN.defaultBlockState());
    }

    /** Fallback interior when we have no dedicated block yet (universe / blue sky) */
    private static BlockState universeLook() {
        // No abyss universe block yet — dark barrier look is fine in-dim
        return abyssBarrier();
    }

    private static BlockState domainDefault() {
        return abyssBarrier();
    }

    /** Walkable floor for this skill_domain */
    public static BlockState floorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> abyssRed();           // Sukuna
            case 2 -> abyssBlack();        // Gojo
            case 5 -> abyssGravel();        // Yuta
            case 6 -> abyssBlack();        // Megumi
            case 8 -> abyssSand();          // Dagon
            case 9 -> abyssSand();          // Yuki
            case 14 -> abyssFlower();       // Hanami
            case 15 -> abyssBone();         // Mahito
            case 19 -> abyssBlack();       // Naoya
            case 27 -> abyssBirch();        // Higuruma (base; rim handled in placeFloor)
            case 29 -> abyssWhite();        // Hakari
            case 613 -> abyssRock();        // Whack-a-Mole
            default -> domainDefault();
        };
    }

    /** Inner shell (walls/ceiling) for this skill_domain */
    public static BlockState interiorState(double skillDomain) {
        int id = (int) Math.round(skillDomain);
        return switch (id) {
            case 1 -> abyssBlack();        // Sukuna
            case 2 -> abyssBlack();        // Gojo
            case 5 -> abyssBlack();        // Yuta
            case 6 -> abyssBlack();        // Megumi
            case 8 -> ModBlocks.DOMAIN_SKY_DAY.get().defaultBlockState();       // Dagon (no blue_sky block yet)
            case 9 -> abyssBlack();        // Yuki
            case 14 -> abyssBlueSky();       // Hanami (or sand/sky later)
            case 15 -> abyssBone();         // Mahito
            case 19 -> abyssBlack();       // Naoya
            case 27 -> abyssBlack();       // Higuruma
            case 29 -> abyssWhite();        // Hakari
            case 613 -> abyssRock();        // Whack-a-Mole
            default -> domainDefault();
        };
    }

    public static void build(ServerLevel level, BlockPos center, double skillDomain) {
        DomainDimBlockGuard.open();
        try {
            BlockState outer = abyssBarrier();
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
        BlockState under = abyssBarrier();
        BlockState floor = floorState(skillDomain);
        BlockState edgeA = abyssRock();
        BlockState edgeB = abyssBirch();

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
            BlockState under = abyssBarrier();
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
     * Overworld outer shell — still JC jujutsu_barrier so JC break/restore works.
     */
    public static void ensureOverworldOuterShell(ServerLevel overworld, Vec3 center, int radius) {
        if (overworld == null || center == null) return;
        BlockState outer = jcBarrier();
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