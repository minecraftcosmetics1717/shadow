
package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public final class DomainClashVisuals {

    private DomainClashVisuals() {}

    private static final int UPDATES_PER_TICK = 256;
    private static final Map<Integer, Integer> CURSORS = new HashMap<>();

    public static void update(
            ServerLevel level,
            DomainInstance inst,
            double ownerSkill,
            double challengerSkill,
            double territory
    ) {
        int radius = DomainDimension.INNER_RADIUS;
        int diameter = radius * 2 + 1;

        int floorCount = diameter * diameter;
        int shellCount = diameter * diameter;
        int total = floorCount + shellCount;

        int cursor = CURSORS.getOrDefault(inst.slot, 0);

        BlockState ownerFloor = DomainArenaBuilder.floorState(ownerSkill);
        BlockState challengerFloor = DomainArenaBuilder.floorState(challengerSkill);

        BlockState ownerWall = DomainArenaBuilder.interiorState(ownerSkill);
        BlockState challengerWall = DomainArenaBuilder.interiorState(challengerSkill);

        BlockPos center = inst.arenaCenter;

        DomainDimBlockGuard.open();

        try {
            for (int i = 0; i < UPDATES_PER_TICK; i++) {
                int index = (cursor + i) % total;

                if (index < floorCount) {
                    int x = index % diameter - radius;
                    int z = index / diameter - radius;

                    if (x * x + z * z > radius * radius) continue;

                    BlockPos pos = center.offset(x, 0, z);

                    boolean challengerOwns = isChallengerTerritory(
                            x, z, radius, territory
                    );

                    BlockState desired = challengerOwns
                            ? challengerFloor : ownerFloor;

                    replacePaletteBlock(
                            level, pos, ownerFloor, challengerFloor, desired
                    );

                } else {
                    int shellIndex = index - floorCount;

                    int latitudeIndex = shellIndex / diameter;
                    int longitudeIndex = shellIndex % diameter;

                    double latitude = -Math.PI / 2.0
                            + Math.PI * latitudeIndex / (diameter - 1);

                    double longitude = 2.0 * Math.PI
                            * longitudeIndex / (diameter - 1);

                    int x = (int) Math.round(
                            radius * Math.cos(latitude) * Math.cos(longitude)
                    );
                    int y = (int) Math.round(
                            radius * Math.sin(latitude)
                    );
                    int z = (int) Math.round(
                            radius * Math.cos(latitude) * Math.sin(longitude)
                    );

                    BlockPos pos = center.offset(x, y, z);

                    boolean challengerOwns = isChallengerTerritory(
                            x, z, radius, territory
                    );

                    BlockState desired = challengerOwns
                            ? challengerWall : ownerWall;

                    replacePaletteBlock(
                            level, pos, ownerWall, challengerWall, desired
                    );
                }
            }
        } finally {
            DomainDimBlockGuard.close();
        }

        CURSORS.put(inst.slot, (cursor + UPDATES_PER_TICK) % total);
    }

    private static boolean isChallengerTerritory(
            int x,
            int z,
            int radius,
            double territory
    ) {
        // Slightly irregular boundary so it does not look
        // like an unnaturally straight dividing line.
        double distortion = Math.sin(z * 0.18) * 3.0
                + Math.sin(z * 0.41) * 1.5;

        double boundary = radius * (1.0 - 2.0 * territory)
                + distortion;

        return x > boundary;
    }

    private static void replacePaletteBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState ownerState,
            BlockState challengerState,
            BlockState desired
    ) {
        BlockState current = level.getBlockState(pos);

        if (current.equals(desired)) return;

        if (!current.equals(ownerState)
                && !current.equals(challengerState)) {
            return;
        }

        level.setBlock(pos, desired, 2);
    }
}
