package com.ghosts.abyss.gojo;

import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.RedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RedBlueCollision {
    private static final double COLLISION_PADDING = 2.0D;

    private RedBlueCollision() {}

    // Called at the HEAD of JC's AIRedProcedure, BEFORE damage/destruction.
    public static boolean checkFlight(RedEntity red) {
        if (red.level().isClientSide || !red.isAlive()
                || red.getPersistentData().getInt(ControlledRedManager.MODE)
                     != ControlledRedManager.FIRED
                || !red.getEntityData().get(RedEntity.DATA_flag_start)) {
            return false;
        }

        var data = red.getPersistentData();
        // AIRed starts its first attack at its entity position, then uses
        // x_pos/y_pos/z_pos for later ticks. Each tick advances 8 * 0.25
        // times the stored directional power.
        Vec3 start = data.getDouble("cnt1") == 0.0D
                ? red.position()
                : new Vec3(data.getDouble("x_pos"),
                           data.getDouble("y_pos"),
                           data.getDouble("z_pos"));
        Vec3 end = start.add(data.getDouble("x_power") * 2.0D,
                             data.getDouble("y_power") * 2.0D,
                             data.getDouble("z_power") * 2.0D);

        ServerLevel level = (ServerLevel) red.level();
        AABB path = new AABB(start, end).inflate(COLLISION_PADDING);
        for (BlueEntity blue : level.getEntitiesOfClass(BlueEntity.class, path)) {
            if (!blue.isAlive() || blue.getPersistentData()
                    .getInt(ControlledBlueManager.MODE)
                    != ControlledBlueManager.ANCHORED) {
                continue;
            }

            AABB hitbox = blue.getBoundingBox().inflate(COLLISION_PADDING);
            if (!hitbox.contains(start) && hitbox.clip(start, end).isEmpty()) {
                continue;
            }


            Vec3 collisionPoint = blue.position();

            if (RedBluePurple.combine(
                    red,
                    blue,
                    collisionPoint
            )) {
                return true;
            }

        }
        return false;
    }
}
