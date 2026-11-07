
package com.ghosts.abyss;

import net.mcreator.jujutsucraft.procedures.LogicAttackProcedure;
import net.mcreator.jujutsucraft.procedures.LogicBetrayalProcedure;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AbyssIdleTransfiguration {

    private AbyssIdleTransfiguration() {
    }

    public static void execute(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity attacker
    ) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        if (!(attacker instanceof LivingEntity mahito)) {
            return;
        }

        double range =
                attacker.getPersistentData().getDouble("Range");

        if (range <= 0.0D) {
            return;
        }

        Vec3 center = new Vec3(x, y, z);

        AABB hitbox = new AABB(center, center)
                .inflate(range / 2.0D);

        // One application per target per attack.
        Set<UUID> hitTargets = new HashSet<>();

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        hitbox,
                        LivingEntity::isAlive
                )) {

            boolean betrayal =
                    LogicBetrayalProcedure.execute(
                            world, attacker, target
                    );

            if (target == attacker && !betrayal) {
                continue;
            }

            // Keep Jujutsucraft's normal target restrictions.
            if (!betrayal &&
                    !LogicAttackProcedure.execute(
                            world, attacker, target
                    )) {
                continue;
            }

            // Don't affect creative or spectator players.
            if (target instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }
            }

            if (!hitTargets.add(target.getUUID())) {
                continue;
            }

            applySoulDamage(mahito, target);
        }
    }

    private static void applySoulDamage(
            LivingEntity attacker,
            LivingEntity target
    ) {
        float damage =
                SoulAwareness.calculateDamage(
                        attacker, target
                );

        if (damage <= 0.0F) {
            return;
        }

        // Prototype: apply the calculated health-based
        // damage without armor or resistance reductions.
        target.hurt(
                target.damageSources().genericKill(),
                damage
        );
    }
}
