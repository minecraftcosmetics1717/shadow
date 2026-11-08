
package com.ghosts.abyss.blood;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSyncPiercingBlood;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.PacketDistributor;

public final class PiercingBloodBeam {

    public static final double MAX_RANGE = 48.0D;
    private static final double HIT_RADIUS = 0.20D;
    private static final float DAMAGE = 4.0F;

    private PiercingBloodBeam() {}

    public static void update(ServerPlayer player) {
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();

// Position the beam near the player's extended hands.
        Vec3 visualStart = eye
                .add(direction.scale(0.85D))
                .add(0.0D, -0.28D, 0.0D);

// Keep the beam's actual trajectory aligned with the camera.
        Vec3 start = visualStart;

        // Aim at the center of the crosshair.
        Vec3 desiredEnd = eye.add(
                direction.scale(MAX_RANGE)
        );

        // First determine where the player's aim hits a block.
        BlockHitResult aimHit = player.level().clip(
                new ClipContext(
                        eye,
                        desiredEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 aimedEnd = aimHit.getType() == HitResult.Type.MISS
                ? desiredEnd
                : aimHit.getLocation();

        // Check the actual beam path from the hands,
        // preventing it from going through nearby walls.
        BlockHitResult beamHit = player.level().clip(
                new ClipContext(
                        start,
                        aimedEnd,
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 end = beamHit.getType() == HitResult.Type.MISS
                ? aimedEnd
                : beamHit.getLocation();

        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(
                        () -> player
                ),
                new PacketSyncPiercingBlood(
                        player.getUUID(),
                        true,
                        start,
                        end
                )
        );

        // Apply damage five times per second.
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
            // Expand the target's hitbox by beam radius.
            AABB hitbox = target.getBoundingBox()
                    .inflate(HIT_RADIUS);

            if (hitbox.contains(start)
                    || hitbox.clip(start, end).isPresent()) {

                target.hurt(
                        level.damageSources().playerAttack(player),
                        DAMAGE
                );
            }
        }
    }

    public static void stop(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(
                        () -> player
                ),
                new PacketSyncPiercingBlood(
                        player.getUUID(),
                        false,
                        Vec3.ZERO,
                        Vec3.ZERO
                )
        );
    }
}
