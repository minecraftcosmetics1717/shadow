
package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DomainClashVisuals {

    private DomainClashVisuals() {}

    // Number of positions inspected per tick, per arena.
    private static final int UPDATES_PER_TICK = 768;

    private record Surface(BlockPos offset, boolean floor) {}

    private static final List<Surface> SURFACES =
            createSurfaces();

    private static final Map<Integer, Integer> CURSORS =
            new HashMap<>();

    public static void reset(int slot) {
        CURSORS.remove(slot);
    }

    public static void update(
            ServerLevel level,
            DomainInstance inst,
            List<DomainClashParticipants.Participant> active,
            Map<UUID, Double> influence
    ) {
        if (level == null || inst == null
                || active.size() < 2) return;

        int cursor = CURSORS.getOrDefault(inst.slot, 0);

        DomainDimBlockGuard.open();

        try {
            for (int i = 0; i < UPDATES_PER_TICK; i++) {

                int index = (cursor + i) % SURFACES.size();

                Surface surface = SURFACES.get(index);
                BlockPos offset = surface.offset();

                DomainClashParticipants.Participant winner =
                        chooseTerritory(
                                active,
                                influence,
                                offset.getX(),
                                offset.getZ()
                        );

                if (winner == null) continue;

                BlockState desired = surface.floor()
                        ? DomainArenaBuilder.floorState(
                        winner.skillDomain
                )
                        : DomainArenaBuilder.interiorState(
                        winner.skillDomain
                );

                BlockPos worldPos =
                        inst.arenaCenter.offset(offset);

                BlockState current =
                        level.getBlockState(worldPos);

                if (current.equals(desired)) continue;

                // Only replace blocks belonging to the
                // registered domains, not arbitrary blocks.
                boolean paletteBlock = false;

                for (DomainClashParticipants.Participant p
                        : inst.participants.all()) {

                    BlockState palette = surface.floor()
                            ? DomainArenaBuilder.floorState(
                            p.skillDomain
                    )
                            : DomainArenaBuilder.interiorState(
                            p.skillDomain
                    );

                    if (current.equals(palette)) {
                        paletteBlock = true;
                        break;
                    }
                }

                if (!paletteBlock) continue;

                level.setBlock(worldPos, desired, 2);
            }

        } finally {
            DomainDimBlockGuard.close();
        }

        CURSORS.put(
                inst.slot,
                (cursor + UPDATES_PER_TICK) % SURFACES.size()
        );
    }

    private static DomainClashParticipants.Participant
    chooseTerritory(
            List<DomainClashParticipants.Participant> active,
            Map<UUID, Double> influence,
            int x,
            int z
    ) {
        DomainClashParticipants.Participant best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (DomainClashParticipants.Participant p : active) {

            double direction = switch (p.side) {
                case NORTH -> -z;
                case SOUTH -> z;
                case WEST -> -x;
                case EAST -> x;
            };

            double strength = influence.getOrDefault(
                    p.entityId, 0.0
            );

            // A stronger domain pushes the boundary,
            // but only gradually and by a limited amount.
            double score = direction
                    + strength * DomainDimension.INNER_RADIUS;

            // A slight irregularity around the boundary.
            double distortion =
                    Math.sin(x * 0.21 + z * 0.13
                            + p.side.ordinal() * 1.7) * 1.5
                            + Math.sin(x * 0.08 - z * 0.17
                            + p.side.ordinal()) * 0.8;

            score += distortion;

            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }

        return best;
    }

    private static List<Surface> createSurfaces() {
        int r = DomainDimension.INNER_RADIUS;

        List<Surface> result = new ArrayList<>();
        Set<Long> shellPositions = new HashSet<>();

        // Complete circular floor.
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r) continue;

                result.add(new Surface(
                        new BlockPos(x, 0, z),
                        true
                ));
            }
        }

        // Generate the spherical shell using the
        // same distance band as DomainArenaBuilder.
        double minSq = (r - 0.5) * (r - 0.5);
        double maxSq = (r + 0.5) * (r + 0.5);

        for (int x = -r - 1; x <= r + 1; x++) {
            for (int y = -r - 1; y <= r + 1; y++) {

                double remaining = maxSq
                        - (double) x * x
                        - (double) y * y;

                if (remaining < 0) continue;

                int zMax = (int) Math.sqrt(remaining);

                for (int z = -zMax; z <= zMax; z++) {
                    double distSq =
                            (double) x * x
                                    + (double) y * y
                                    + (double) z * z;

                    if (distSq < minSq || distSq > maxSq) {
                        continue;
                    }

                    // Floor takes priority at y=0.
                    if (y == 0 && x * x + z * z <= r * r) {
                        continue;
                    }

                    long key = BlockPos.asLong(x, y, z);

                    if (shellPositions.add(key)) {
                        result.add(new Surface(
                                new BlockPos(x, y, z),
                                false
                        ));
                    }
                }
            }
        }

        return result;
    }
}
