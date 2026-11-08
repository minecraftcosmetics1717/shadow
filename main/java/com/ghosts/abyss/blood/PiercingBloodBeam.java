
package com.ghosts.abyss.blood;

import com.ghosts.abyss.entity.PiercingBloodEntity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

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

// Calculate the visible length of Piercing Blood.
// Project the collision point onto the firing direction
// so the length matches the beam's forward axis.
        double visibleLength = end.subtract(start).dot(direction);

// Keep the length within the beam's maximum range.
        visibleLength = Math.max(
                0.0D,
                Math.min(MAX_RANGE, visibleLength)
        );

// Synchronize the length to all clients.
        beam.setBeamLength((float) visibleLength);
// Only create impact particles when a block was hit.
        boolean hitBlock =
                beamHit.getType() == HitResult.Type.BLOCK
                        || aimHit.getType() == HitResult.Type.BLOCK;

        if (hitBlock && player.tickCount % 2 == 0) {
            spawnImpactParticles(level, end, direction);
        }
// Existing damage logic remains unchanged.
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

    private static void spawnImpactParticles(
            ServerLevel level,
            Vec3 impact,
            Vec3 direction
    ) {

        // Dark crimson, matching the Piercing Blood beam.
        ParticleOptions crimson = new DustParticleOptions(
                new Vector3f(0.47F, 0.0F, 0.05F),
                1.0F
        );

        // A few particles spreading from the impact point.
        level.sendParticles(
                crimson,
                impact.x,
                impact.y,
                impact.z,
                5,
                0.10D,
                0.10D,
                0.10D,
                0.015D
        );

        // A smaller, brighter center.
        ParticleOptions brightRed = new DustParticleOptions(
                new Vector3f(0.85F, 0.08F, 0.12F),
                0.7F
        );

        level.sendParticles(
                brightRed,
                impact.x,
                impact.y,
                impact.z,
                2,
                0.035D,
                0.035D,
                0.035D,
                0.005D
        );
    }

    public static void stop(ServerPlayer player) {

        PiercingBloodEntity beam =
                ACTIVE_BEAMS.remove(player.getUUID());

        if (beam != null && !beam.isRemoved()) {
            beam.discard();
        }
    }
}
