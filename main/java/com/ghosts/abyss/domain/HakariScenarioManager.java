package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.entity.HakariScenarioActor;
import com.ghosts.abyss.init.JujutsuSlopEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Scripted, server-authoritative Riichi scenes for the 42 x 13 x 101 subway. */
public final class HakariScenarioManager {
    private HakariScenarioManager() {}
    // Texture 2 is the short-haired male character. Change to 0 if the skins were swapped.
    private static final int MALE_SKIN = 1;
    private static final double RUN_SPEED = 0.23D;
    private static final int START_PAUSE = 12;
    private static final int MAX_TICKS = 20 * 35;
    private static final int VENDING_PAUSE_TICKS = 20 * 3;
    public enum Type { TRAIN_TO_RESTROOM, STAIRS_TO_TRAIN }

    static Scene start(ServerLevel level, BlockPos origin, boolean jackpot) {
        Type type = level.random.nextBoolean() ? Type.TRAIN_TO_RESTROOM : Type.STAIRS_TO_TRAIN;
        Scene scene = new Scene(origin, type, jackpot);
        if (!scene.spawn(level)) { scene.clear(level); return null; }
        return scene;
    }

    static final class Scene {
        final BlockPos origin;
        final Type type;
        final boolean jackpot;
        final List<Vec3> route = new ArrayList<>();
        final Map<BlockPos, BlockState> savedDoors = new LinkedHashMap<>();
        UUID actorId;
        int waypoint;
        int elapsed;
        boolean finished;
        boolean arrivalDoorOpened;
        int vendingWaypoint = -1;
        int vendingPauseRemaining;
        boolean vendingPauseStarted;

        Scene(BlockPos origin, Type type, boolean jackpot) {
            this.origin = origin;
            this.type = type;
            this.jackpot = jackpot;
            if (type == Type.TRAIN_TO_RESTROOM) {
                // Exit the train, stop in front of the east-facing vending machine
                // at (2,4,36), then return to the clear lane toward the restroom.
                add(12, 4, 37); add(10, 4.0625, 37); add(7, 4, 37);
                add(7, 4, 36); add(3, 4, 36);
                vendingWaypoint = route.size() - 1;
                add(7, 4, 36); add(7, 4, 47);
                add(3, 4, 47);
                if (jackpot) route.add(local(1.32, 4, 47.5));
            } else {
                // Start at the opposite (south) staircase, just inside its barrier.
                // Both outcomes share the long run along the platform.
                add(6, 4, 50);
                if (jackpot) {
                    add(6, 4, 29); add(9, 4, 29);
                    add(10, 4.0625, 29); add(12, 4, 29); add(13, 4, 29);
                }
            }
        }
        Vec3 local(double x, double y, double z) { return new Vec3(origin.getX()+x, origin.getY()+y, origin.getZ()+z); }
        void add(int x, double y, int z) { route.add(local(x+0.5, y, z+0.5)); }
        // Prefer the same platform as the actor, near the scene's start.
        // Two clear lanes, 3-block separation, then extend down the platform.
        // Use the opposite platform only if the near platform is full.
        List<Vec3> participantSpawns() {
            int anchor = type == Type.TRAIN_TO_RESTROOM ? 41 : 80;
            List<Vec3> positions = new ArrayList<>();
            for (int side = 0; side < 2; side++) {
                int[] xs = side == 0 ? new int[]{4, 8} : new int[]{37, 33};
                for (int distance = 0; distance <= 90; distance += 3) {
                    int[] zs = distance == 0 ? new int[]{anchor}
                            : new int[]{anchor + distance, anchor - distance};
                    for (int z : zs) {
                        if (z < 6 || z > 93) continue;
                        for (int x : xs) positions.add(local(x + 0.5, 4, z + 0.5));
                    }
                }
            }
            return positions;
        }
        boolean spawn(ServerLevel level) {
            HakariScenarioActor actor = JujutsuSlopEntities.HAKARI_SCENARIO_ACTOR.get().create(level);
            if (actor == null) return false;
            Vec3 start = type == Type.TRAIN_TO_RESTROOM ? local(13.5,4,37.5) : local(6.5,4,94.5);
            if (type == Type.TRAIN_TO_RESTROOM) openDoor(level, 37);
            actor.setSkin(MALE_SKIN);
            actor.moveTo(start.x, start.y, start.z, type == Type.TRAIN_TO_RESTROOM ? 90 : 180, 0);
            actor.getPersistentData().putBoolean("abyss_domain_decoration", true);
            if (!level.addFreshEntity(actor)) return false;
            actorId = actor.getUUID();
            return true;
        }
        boolean tick(ServerLevel level) {
            if (finished) return true;
            Entity entity = actorId == null ? null : level.getEntity(actorId);
            if (!(entity instanceof HakariScenarioActor actor) || ++elapsed > MAX_TICKS) {
                Abyss.LOGGER.warn("[Hakari Scenario] Actor missing or timed out; resolving predetermined outcome");
                clear(level); finished = true; return true;
            }
            if (elapsed <= START_PAUSE) return false;
            if (type == Type.TRAIN_TO_RESTROOM && waypoint >= 3) restoreDoors(level);
            if (type == Type.STAIRS_TO_TRAIN && jackpot && waypoint >= 3 && !arrivalDoorOpened) {
                openDoor(level, 29); arrivalDoorOpened = true;
            }
            if (vendingPauseRemaining > 0) {
                actor.setWalking(false);
                actor.setYRot(90.0F); // Face west, toward the vending machine.
                vendingPauseRemaining--;
                return false;
            }
            if (waypoint < route.size() && actor.walkToward(route.get(waypoint), RUN_SPEED)) {
                int arrived = waypoint++;
                if (arrived == vendingWaypoint && !vendingPauseStarted) {
                    vendingPauseStarted = true;
                    vendingPauseRemaining = VENDING_PAUSE_TICKS;
                    actor.setWalking(false);
                    actor.setYRot(90.0F);
                    return false;
                }
            }
            if (waypoint < route.size()) return false;
            actor.setWalking(false);
            if (jackpot) {
                // Reach the restroom entrance or board the train, then disappear.
                actor.discard(); actorId = null;
            } else {
                // Sit on the platform; remain visible through the result delay.
                actor.setSitting(true);
            }
            restoreDoors(level);
            finished = true;
            return true;
        }
        private void openDoor(ServerLevel level, int z) {
            // Only open the known doorway, and only if it is still the original iron door.
            // Store both halves so the shared structure is restored after every scene.
            DomainDimBlockGuard.open();
            try {
                for (int y=4; y<=5; y++) {
                    BlockPos p = origin.offset(13,y,z);
                    BlockState state = level.getBlockState(p);
                    if (state.is(Blocks.IRON_DOOR) && state.hasProperty(DoorBlock.OPEN)) {
                        savedDoors.putIfAbsent(p, state);
                        level.setBlock(p, state.setValue(DoorBlock.OPEN, true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            } finally { DomainDimBlockGuard.close(); }
        }
        private void restoreDoors(ServerLevel level) {
            if (savedDoors.isEmpty()) return;
            DomainDimBlockGuard.open();
            try {
                for (var entry : savedDoors.entrySet()) {
                    if (level.getBlockState(entry.getKey()).is(Blocks.IRON_DOOR))
                        level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            } finally { DomainDimBlockGuard.close(); savedDoors.clear(); }
        }
        void clear(ServerLevel level) {
            if (actorId != null) {
                Entity entity = level.getEntity(actorId);
                if (entity instanceof HakariScenarioActor) entity.discard();
                actorId = null;
            }
            restoreDoors(level);
        }
    }
}
