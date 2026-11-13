
package com.ghosts.abyss.gojo;

import net.mcreator.jujutsucraft.entity.RedEntity;
import net.mcreator.jujutsucraft.procedures.BlockDestroyAllDirectionProcedure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ControlledRedEffects {

    private static final double ATTACK_RADIUS = 3.0D;
    private static final float DAMAGE = 4.0F;
    private static final double KNOCKBACK = 1.6D;

    private static final int ATTACK_INTERVAL = 10;
    private static final int BLOCK_INTERVAL = 2;

    private ControlledRedEffects() {}

    public static void tick(
            ServerPlayer owner,
            RedEntity red
    ) {
        if (!red.isAlive() || red.level().isClientSide) {
            return;
        }

        if (red.getPersistentData().getInt(
                ControlledRedManager.MODE
        ) != ControlledRedManager.CONTROLLED) {
            return;
        }

        if (red.tickCount % ATTACK_INTERVAL == 0) {
            repelEntities(owner, red);
        }

        if (red.tickCount % BLOCK_INTERVAL == 0) {
            destroyBlocks(red);
        }
    }

    private static void repelEntities(
            ServerPlayer owner,
            RedEntity red
    ) {
        AABB area = new AABB(
                red.position(),
                red.position()
        ).inflate(ATTACK_RADIUS);

        for (LivingEntity target :
                owner.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        area
                )) {

            if (target == owner
                    || target == red
                    || !target.isAlive()
                    || owner.isAlliedTo(target)) {
                continue;
            }

            if (target instanceof Player player
                    && (player.isCreative()
                    || player.isSpectator())) {
                continue;
            }

            Vec3 away = target.position()
                    .subtract(red.position());

            if (away.horizontalDistanceSqr() < 0.0001D) {
                away = owner.getLookAngle();
            }

            Vec3 direction = new Vec3(
                    away.x,
                    0.0D,
                    away.z
            ).normalize();

            target.hurt(
                    red.damageSources().indirectMagic(
                            red,
                            owner
                    ),
                    DAMAGE
            );

            target.push(
                    direction.x * KNOCKBACK,
                    0.35D,
                    direction.z * KNOCKBACK
            );

            target.hurtMarked = true;
        }
    }

    private static void destroyBlocks(RedEntity red) {
        CompoundTag data = red.getPersistentData();

        // Use Blue-style block interactions.
        // Negative knockback draws debris inward.
        data.putDouble("knockback", -1.0D);
        data.putDouble("BlockDamage", 4.0D);
        data.putBoolean("noParticle", true);

        data.putDouble("BlockRange", 1.5D);

        BlockDestroyAllDirectionProcedure.execute(
                red.level(),
                red.getX(),
                red.getY(),
                red.getZ(),
                red
        );

        data.putDouble("BlockRange", 3.0D);

        BlockDestroyAllDirectionProcedure.execute(
                red.level(),
                red.getX(),
                red.getY(),
                red.getZ(),
                red
        );
    }
}
