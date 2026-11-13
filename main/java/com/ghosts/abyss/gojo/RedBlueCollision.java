
package com.ghosts.abyss.gojo;

import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.RedEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public final class RedBlueCollision {

    private static final double COLLISION_RADIUS = 2.0D;

    private RedBlueCollision() {}

    public static boolean check(RedEntity red) {
        if (red.level().isClientSide) {
            return false;
        }

        if (!red.isAlive()) {
            return false;
        }

        ServerLevel level = (ServerLevel) red.level();

        AABB area = red.getBoundingBox()
                .inflate(COLLISION_RADIUS);

        for (BlueEntity blue : level.getEntitiesOfClass(
                BlueEntity.class,
                area
        )) {
            if (!blue.isAlive()) {
                continue;
            }

            int mode = blue.getPersistentData()
                    .getInt(ControlledBlueManager.MODE);

            if (mode != ControlledBlueManager.ANCHORED) {
                continue;
            }

            // Both techniques disappear.
            red.discard();
            blue.discard();

            // Reserved for our future interaction.
            return true;
        }

        return false;
    }
}
