
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.entity.SeaSerpentEntity;
import net.mcreator.jujutsucraft.init.JujutsucraftModEntities;
import net.mcreator.jujutsucraft.procedures.LogicAttackProcedure;
import net.mcreator.jujutsucraft.procedures.RangeAttackProcedure;
import net.mcreator.jujutsucraft.procedures.SetRangedAmmoProcedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class DagonDomainFish {

    private DagonDomainFish() {}

    public static final String TAG =
            "abyss_dagon_domain_fish";

    private static final int SPAWN_INTERVAL = 90;
    private static final int MIN_GROUP = 3;
    private static final int MAX_GROUP = 6;
    private static final int MAX_FISH = 24;

    private static final int LIFETIME = 160;
    private static final double DETECTION = 24.0D;
    private static final double LOCK_RANGE = 34.0D;

    public static boolean isDomainFish(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG);
    }

    public static void spawnGroup(
            ServerLevel level,
            DomainInstance inst,
            LivingEntity owner
    ) {
        if (level == null || inst == null || owner == null) {
            return;
        }

        long tick = level.getGameTime();

        // Each arena gets its own spawn schedule.
        if ((tick + inst.slot * 17L) % SPAWN_INTERVAL != 0L) {
            return;
        }

        AABB arena = arenaBox(inst);

        int existing = level.getEntitiesOfClass(
                SeaSerpentEntity.class,
                arena,
                DagonDomainFish::isDomainFish
        ).size();

        if (existing >= MAX_FISH) return;

        int count = MIN_GROUP
                + level.random.nextInt(
                MAX_GROUP - MIN_GROUP + 1
        );

        count = Math.min(count, MAX_FISH - existing);

        // Choose one general location for the group.
        double angle = level.random.nextDouble()
                * Math.PI * 2.0D;


        double radius = Math.sqrt(level.random.nextDouble())
                * 53.0D;

        double groupX = Math.cos(angle) * radius;
        double groupZ = Math.sin(angle) * radius;

        double groupY = 5.0D
                + level.random.nextDouble() * 9.0D;

        for (int i = 0; i < count; i++) {

            double x = groupX
                    + (level.random.nextDouble() - 0.5D)
                    * 8.0D;

            double z = groupZ
                    + (level.random.nextDouble() - 0.5D)
                    * 8.0D;

            double y = groupY
                    + (level.random.nextDouble() - 0.5D)
                    * 4.0D;

// Keep fish safely within the spherical arena.
            if (x * x + z * z > 55.0D * 55.0D) {
                continue;
            }

            SeaSerpentEntity fish =
                    JujutsucraftModEntities.SEA_SERPENT
                            .get().create(level);

            if (fish == null) continue;

            double worldX =
                    inst.arenaCenter.getX() + 0.5D + x;

            double worldY =
                    inst.arenaCenter.getY() + y;

            double worldZ =
                    inst.arenaCenter.getZ() + 0.5D + z;

            float yaw = level.random.nextFloat() * 360.0F;

            fish.moveTo(
                    worldX,
                    worldY,
                    worldZ,
                    yaw,
                    0.0F
            );

            // Copy the caster's ownership and combat data.
            SetRangedAmmoProcedure.execute(owner, fish);

            fish.setNoAi(true);
            fish.setNoGravity(true);

            CompoundTag data = fish.getPersistentData();

            data.putBoolean(TAG, true);
            data.putUUID("abyss_dagon_owner", owner.getUUID());
            data.putInt("abyss_dagon_slot", inst.slot);
            data.putInt("abyss_dagon_age", 0);
            data.putInt("abyss_dagon_attack_cooldown", 0);
            data.putInt("abyss_dagon_wander_ticks", 0);

            // Keep Jujutsucraft's ownership convention.
            data.putString(
                    "OWNER_UUID",
                    owner.getUUID().toString()
            );

            // Three original Dagon fish textures.
            fish.setTexture(
                    "dagon_fish" + (1 + level.random.nextInt(3))
            );

            level.addFreshEntity(fish);
        }
    }

    public static void tickFish(
            ServerLevel level,
            SeaSerpentEntity fish
    ) {
        CompoundTag data = fish.getPersistentData();

        if (!data.getBoolean(TAG)) return;

        int slot = data.getInt("abyss_dagon_slot");
        DomainInstance inst = DomainInstanceManager.getBySlot(slot);

        if (inst == null
                || !data.hasUUID("abyss_dagon_owner")
                || !inst.ownerId.equals(
                data.getUUID("abyss_dagon_owner"))
                || (int) inst.skillDomain != 8) {
            fish.discard();
            return;
        }

        LivingEntity owner = DomainArenaService.findLivingIn(
                level,
                inst.ownerId
        );

        if (owner == null || !owner.isAlive()
                || owner.getPersistentData()
                .getBoolean("DomainDefeated")
                || DomainClash.findActiveDomainInArena(
                level, inst, inst.ownerId
        ) != null) {
            fish.discard();
            return;
        }

        int age = data.getInt("abyss_dagon_age") + 1;
        data.putInt("abyss_dagon_age", age);

        if (age >= LIFETIME) {
            fish.discard();
            return;
        }

        // Do not let fish escape their arena.
        double centerX = inst.arenaCenter.getX() + 0.5D;
        double centerZ = inst.arenaCenter.getZ() + 0.5D;

        double dx = fish.getX() - centerX;
        double dz = fish.getZ() - centerZ;

        if (dx * dx + dz * dz > 55.0D * 55.0D) {
            fish.discard();
            return;
        }

        int cooldown = Math.max(
                0,
                data.getInt("abyss_dagon_attack_cooldown") - 1
        );

        data.putInt("abyss_dagon_attack_cooldown", cooldown);

        LivingEntity target = findTarget(level, fish, inst);

        if (target != null) {
            chaseAndAttack(level, fish, target, data);
        } else {
            wander(level, fish, inst, data);
        }
    }

    private static LivingEntity findTarget(
            ServerLevel level,
            SeaSerpentEntity fish,
            DomainInstance inst
    ) {
        CompoundTag data = fish.getPersistentData();

        // Keep an existing target until it moves
        // outside the larger lock-on range.
        if (data.hasUUID("abyss_dagon_target")) {
            Entity existing = level.getEntity(
                    data.getUUID("abyss_dagon_target")
            );

            if (existing instanceof LivingEntity living
                    && isValidTarget(level, fish, living, inst)
                    && fish.distanceToSqr(living)
                    <= LOCK_RANGE * LOCK_RANGE) {
                return living;
            }
        }

        LivingEntity nearest = null;
        double best = DETECTION * DETECTION;

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                fish.getBoundingBox().inflate(DETECTION),
                e -> e != fish
        )) {
            if (!isValidTarget(
                    level, fish, candidate, inst
            )) {
                continue;
            }

            double distance = fish.distanceToSqr(candidate);

            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            data.putUUID(
                    "abyss_dagon_target",
                    nearest.getUUID()
            );
        } else {
            data.remove("abyss_dagon_target");
        }

        return nearest;
    }

    private static boolean isValidTarget(
            ServerLevel level,
            SeaSerpentEntity fish,
            LivingEntity target,
            DomainInstance inst
    ) {
        if (!target.isAlive()) return false;

        if (target.getUUID().equals(inst.ownerId)) {
            return false;
        }

        // Never attack other domain fish.
        if (isDomainFish(target)) return false;

        // Exclude creative and spectator players.
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        // Ignore entities outside this arena.
        double dx = target.getX()
                - (inst.arenaCenter.getX() + 0.5D);

        double dz = target.getZ()
                - (inst.arenaCenter.getZ() + 0.5D);

        if (dx * dx + dz * dz
                > DomainDimension.INNER_RADIUS
                * DomainDimension.INNER_RADIUS) {
            return false;
        }

        // Follow JC's friendly-fire and faction rules.
        return LogicAttackProcedure.execute(
                level,
                fish,
                target
        );
    }

    private static void chaseAndAttack(
            ServerLevel level,
            SeaSerpentEntity fish,
            LivingEntity target,
            CompoundTag data
    ) {
        Vec3 targetPos = target.position().add(
                0.0D,
                target.getBbHeight() * 0.5D,
                0.0D
        );

        Vec3 direction = targetPos.subtract(
                fish.position().add(
                        0.0D,
                        fish.getBbHeight() * 0.5D,
                        0.0D
                )
        );

        double distance = direction.length();

        if (distance > 0.001D) {
            moveFish(
                    fish,
                    direction.normalize(),
                    1D
            );
        }

        if (distance <= 2.8D
                && data.getInt(
                "abyss_dagon_attack_cooldown") == 0) {

            // Use JC's own damage and attack processing.
            data.putDouble("Damage", 13.5D);
            data.putDouble("Range", 1.7D);
            data.putDouble("knockback", 0.25D);
            data.putDouble("effect", 1.0D);
            data.putBoolean("swing", true);

            fish.swing(InteractionHand.MAIN_HAND);

            RangeAttackProcedure.execute(
                    level,
                    target.getX(),
                    target.getY()
                            + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    fish
            );

            data.putInt(
                    "abyss_dagon_attack_cooldown",
                    14
            );
        }
    }

    private static void wander(
            ServerLevel level,
            SeaSerpentEntity fish,
            DomainInstance inst,
            CompoundTag data
    ) {
        int remaining =
                data.getInt("abyss_dagon_wander_ticks");

        Vec3 current = fish.position();

        Vec3 destination = new Vec3(
                data.getDouble("abyss_dagon_wander_x"),
                data.getDouble("abyss_dagon_wander_y"),
                data.getDouble("abyss_dagon_wander_z")
        );

        if (remaining <= 0
                || current.distanceToSqr(destination) < 9.0D
                || fish.horizontalCollision
                || fish.verticalCollision) {

            double centerX = inst.arenaCenter.getX() + 0.5D;
            double centerZ = inst.arenaCenter.getZ() + 0.5D;

            double angle =
                    level.random.nextDouble() * Math.PI * 2.0D;

            double radius =
                    8.0D + level.random.nextDouble() * 41.0D;

            destination = new Vec3(
                    centerX + Math.cos(angle) * radius,
                    inst.arenaCenter.getY()
                            + 4.0D
                            + level.random.nextDouble() * 15.0D,
                    centerZ + Math.sin(angle) * radius
            );

            data.putDouble(
                    "abyss_dagon_wander_x", destination.x
            );
            data.putDouble(
                    "abyss_dagon_wander_y", destination.y
            );
            data.putDouble(
                    "abyss_dagon_wander_z", destination.z
            );

            remaining = 30 + level.random.nextInt(35);
        }

        data.putInt(
                "abyss_dagon_wander_ticks",
                remaining - 1
        );

        Vec3 direction = destination.subtract(current);

        if (direction.lengthSqr() > 0.001D) {
            moveFish(
                    fish,
                    direction.normalize(),
                    0.6D
            );
        }
    }

    private static void moveFish(
            SeaSerpentEntity fish,
            Vec3 direction,
            double speed
    ) {
        Vec3 movement = direction.scale(speed);

// Move immediately using Abyss's custom AI.
        fish.move(MoverType.SELF, movement);

// Prevent vanilla movement from adding
// another copy of this movement next tick.
        fish.setDeltaMovement(Vec3.ZERO);

        // Match the facing direction to movement.
        float yaw = (float) (
                Math.toDegrees(
                        Math.atan2(-direction.x, direction.z)
                )
        );

        float pitch = (float) (
                -Math.toDegrees(
                        Math.asin(
                                Math.max(-1.0D,
                                        Math.min(1.0D, direction.y))
                        )
                )
        );

        fish.setYRot(yaw);
        fish.setXRot(pitch);
        fish.setYHeadRot(yaw);
        fish.setYBodyRot(yaw);

        fish.hasImpulse = true;
    }

    public static void clear(
            ServerLevel level,
            DomainInstance inst
    ) {
        for (SeaSerpentEntity fish : level.getEntitiesOfClass(
                SeaSerpentEntity.class,
                arenaBox(inst),
                DagonDomainFish::isDomainFish
        )) {
            fish.discard();
        }
    }

    private static AABB arenaBox(DomainInstance inst) {
        double r = DomainDimension.OUTER_RADIUS + 4.0D;

        return new AABB(
                inst.arenaCenter.getX() - r,
                inst.arenaCenter.getY() - r,
                inst.arenaCenter.getZ() - r,
                inst.arenaCenter.getX() + r + 1.0D,
                inst.arenaCenter.getY() + r + 1.0D,
                inst.arenaCenter.getZ() + r + 1.0D
        );
    }
}
