
package com.ghosts.abyss.gojo;

import net.mcreator.jujutsucraft.entity.BlueEntity;
import net.mcreator.jujutsucraft.entity.RedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ControlledRedProjectile {

    private static final double SPEED = 3.5D;
    private static final double MAX_TRAVEL = 48.0D;
    private static final double COLLISION_PADDING = 1.5D;

    private ControlledRedProjectile() {}

    public static void tick(RedEntity red) {
        if (!(red.level() instanceof ServerLevel level)
                || !red.isAlive()) {
            return;
        }

        CompoundTag tag = red.getPersistentData();

        if (tag.getInt(ControlledRedManager.MODE)
                != ControlledRedManager.FLYING) {
            return;
        }

        Vec3 direction = new Vec3(
                tag.getDouble("abyss_red_dir_x"),
                tag.getDouble("abyss_red_dir_y"),
                tag.getDouble("abyss_red_dir_z")
        );

        if (direction.lengthSqr() < 0.0001D) {
            red.discard();
            return;
        }

        direction = direction.normalize();

        Vec3 start = red.position();
        Vec3 end = start.add(direction.scale(SPEED));

        // Find the first solid block in the flight path.
        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                red
        ));

        Vec3 reachableEnd = blockHit.getType()
                == HitResult.Type.MISS
                ? end
                : blockHit.getLocation();

        AABB path = new AABB(start, reachableEnd)
                .inflate(COLLISION_PADDING);

        // Anchored Blue takes priority over ordinary mobs.
        for (BlueEntity blue :
                level.getEntitiesOfClass(
                        BlueEntity.class, path)) {

            if (!blue.isAlive()) {
                continue;
            }

            if (blue.getPersistentData().getInt(
                    ControlledBlueManager.MODE)
                    != ControlledBlueManager.ANCHORED) {
                continue;
            }

            AABB hitbox = blue.getBoundingBox()
                    .inflate(COLLISION_PADDING);

            if (!hitbox.contains(start)
                    && hitbox.clip(
                    start, reachableEnd
            ).isEmpty()) {
                continue;
            }

            // Future Red + Blue interaction.

            Vec3 collisionPoint = blue.position();

            if (RedBluePurple.combine(
                    red,
                    blue,
                    collisionPoint
            )) {
                return;
            }

        }

        // Damage and repel mobs Red passes through.
        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class, path)) {

            if (target == red
                    || !target.isAlive()
                    || target instanceof RedEntity
                    || target instanceof BlueEntity) {
                continue;
            }

            if (target instanceof Player player
                    && (player.isCreative()
                    || player.isSpectator())) {
                continue;
            }

            // Do not hit the caster.
            if (tag.hasUUID("abyss_red_caster")
                    && tag.getUUID("abyss_red_caster")
                    .equals(target.getUUID())) {
                continue;
            }

            AABB hitbox = target.getBoundingBox()
                    .inflate(0.5D);

            if (!hitbox.contains(start)
                    && hitbox.clip(
                    start, reachableEnd
            ).isEmpty()) {
                continue;
            }

            if (target.hurt(
                    red.damageSources().magic(),
                    18F)) {

                Vec3 away = target.position()
                        .subtract(red.position());

                Vec3 push = new Vec3(
                        away.x, 0.0D, away.z
                );

                if (push.lengthSqr() < 0.0001D) {
                    push = new Vec3(
                            direction.x, 0.0D,
                            direction.z
                    );
                }

                if (push.lengthSqr() > 0.0001D) {
                    push = push.normalize();

                    target.push(
                            push.x * 1.6D,
                            0.35D,
                            push.z * 1.6D
                    );

                    target.hurtMarked = true;
                }
            }
        }

        // Move Red visibly.
        red.setDeltaMovement(Vec3.ZERO);
        red.setPos(
                reachableEnd.x,
                reachableEnd.y,
                reachableEnd.z
        );
        red.hasImpulse = true;

        double traveled =
                tag.getDouble("abyss_red_travel")
                        + start.distanceTo(reachableEnd);

        tag.putDouble("abyss_red_travel", traveled);

        // A block collision triggers the original attack.
        if (blockHit.getType() != HitResult.Type.MISS) {
            impact(red, direction);
            return;
        }

        // Expire at maximum range.
        if (traveled >= MAX_TRAVEL) {
            red.discard();
        }
    }

    private static void impact(
            RedEntity red,
            Vec3 direction
    ) {
        CompoundTag tag = red.getPersistentData();

        // Switch back to Jujutsucraft's attack.
        tag.putInt(
                ControlledRedManager.MODE,
                ControlledRedManager.FIRED
        );

        tag.putDouble("x_power", direction.x * 3.0D);
        tag.putDouble("y_power", direction.y * 3.0D);
        tag.putDouble("z_power", direction.z * 3.0D);

        tag.putDouble("x_pos", red.getX());
        tag.putDouble("y_pos", red.getY());
        tag.putDouble("z_pos", red.getZ());

        tag.putDouble("cnt1", 0.0D);
        tag.putDouble("cnt_life", 0.0D);
        tag.putDouble("cnt6", 0.0D);

        red.setDeltaMovement(Vec3.ZERO);

        red.getEntityData().set(
                RedEntity.DATA_flag_start,
                true
        );
    }
}
