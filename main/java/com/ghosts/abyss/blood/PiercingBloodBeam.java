
package com.ghosts.abyss.blood;

import com.ghosts.abyss.entity.PiercingBloodEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PiercingBloodBeam {

    public static final double MAX_RANGE = 48.0D;

    private static final double HIT_RADIUS = 0.20D;
    private static final float DAMAGE = 4.0F;

    private static final Map<UUID, PiercingBloodEntity>
            ACTIVE_BEAMS = new HashMap<>();

    private PiercingBloodBeam() {}

    public static void update(ServerPlayer player) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // =========================================
        // SPAWN OR UPDATE VISUAL ENTITY
        // =========================================

        PiercingBloodEntity beam =
                ACTIVE_BEAMS.get(player.getUUID());

        if (beam == null
                || beam.isRemoved()
                || beam.level() != level) {

            beam = new PiercingBloodEntity(level, player);

            level.addFreshEntity(beam);

            ACTIVE_BEAMS.put(
                    player.getUUID(),
                    beam
            );
        }

        beam.followOwner(player);

        // =========================================
        // EXISTING DAMAGE RAY
        // =========================================

        Vec3 direction = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();

        Vec3 start = eye
                .add(direction.scale(0.85D))
                .add(0.0D, -0.28D, 0.0D);

        Vec3 desiredEnd = eye.add(
                direction.scale(MAX_RANGE)
        );

        BlockHitResult aimHit = level.clip(
                new ClipContext(
                        eye,
                        desiredEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 aimedEnd =
                aimHit.getType() == HitResult.Type.MISS
                        ? desiredEnd
                        : aimHit.getLocation();

        BlockHitResult beamHit = level.clip(
                new ClipContext(
                        start,
                        aimedEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 end =
                beamHit.getType() == HitResult.Type.MISS
                        ? aimedEnd
                        : beamHit.getLocation();

        if (player.tickCount % 4 == 0) {
            damageEntities(player, start, end);
        }
    }

    private static void damageEntities(
            ServerPlayer player,
            Vec3 start,
            Vec3 end
    ) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB searchBox = new AABB(start, end)
                .inflate(HIT_RADIUS + 1.0D);

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity.isAlive()
                        && entity != player
                        && !entity.isSpectator()
        )) {

            AABB hitbox = target.getBoundingBox()
                    .inflate(HIT_RADIUS);

            if (hitbox.contains(start)
                    || hitbox.clip(start, end).isPresent()) {

                target.hurt(
                        level.damageSources().magic(),
                        DAMAGE
                );
            }
        }
    }

    public static void stop(ServerPlayer player) {

        PiercingBloodEntity beam =
                ACTIVE_BEAMS.remove(player.getUUID());

        if (beam != null && !beam.isRemoved()) {
            beam.discard();
        }
    }
}
