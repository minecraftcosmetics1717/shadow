
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.procedures.ChimeraShadowGardenSetProcedure;
import net.mcreator.jujutsucraft.procedures.LogicAttackDomainProcedure;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.registries.ForgeRegistries;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.ArrayList;
import java.util.List;

public final class ChimeraShadowGardenSureHit {

    private static final int MAX_SUMMONS = 15;
    private static final int SPAWN_INTERVAL = 80;

    private record Summon(
            String entityId,
            int nbtNumber,
            String advancement,
            int perTypeLimit
    ) {}

    private static final List<Summon> POOL = List.of(
            new Summon("divine_dog_white", 1,
                    "skill_divine_dogs", 3),
            new Summon("divine_dog_black", 2,
                    "skill_divine_dogs", 3),
            new Summon("divine_dog_totality", 3,
                    "skill_divine_dog_totality", 2),
            new Summon("nue", 4, "skill_nue", 3),
            new Summon("great_serpent", 5,
                    "skill_great_serpent", 2),
            new Summon("toad_2", 6, "skill_toad", 5),
            new Summon("max_elephant", 7,
                    "skill_max_elephant", 2),
            new Summon("round_deer", 9,
                    "skill_round_deer", 2),
            new Summon("piercing_ox", 10,
                    "skill_piercing_ox", 2),
            new Summon("tiger_funeral", 11,
                    "skill_tiger_funeral", 2),
            new Summon(
                    "eight_handled_sword_divergent_sila_divine_general_mahoraga",
                    14,
                    "skill_mahoraga",
                    3)
    );

    private ChimeraShadowGardenSureHit() {}

    public static void tick(
            ServerLevel level,
            DomainInstance inst,
            LivingEntity owner
    ) {
        if (level == null || inst == null || owner == null) {
            return;
        }

        // Keep the original domain's movement suppression.
        AABB arena = new AABB(
                inst.arenaCenter
        ).inflate(DomainDimension.INNER_RADIUS);

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        arena,
                        LivingEntity::isAlive
                )) {

            if (!LogicAttackDomainProcedure.execute(
                    level, owner, target)) {
                continue;
            }

            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    10,
                    5,
                    false,
                    false
            ));

            target.removeEffect(MobEffects.MOVEMENT_SPEED);
            target.removeEffect(MobEffects.JUMP);
        }

        // Allow only one spawn attempt every 80 ticks.
        long now = level.getGameTime();

        if (now < inst.nextChimeraSpawnTick) {
            return;
        }

        inst.nextChimeraSpawnTick =
                now + SPAWN_INTERVAL;

        // Find this caster's existing domain summons.
        List<LivingEntity> active = new ArrayList<>();

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        arena,
                        LivingEntity::isAlive
                )) {

            if (!entity.getPersistentData()
                    .getBoolean("domain_entity")) {
                continue;
            }

            if (!owner.getStringUUID().equals(
                    entity.getPersistentData()
                            .getString("OWNER_UUID"))) {
                continue;
            }

            active.add(entity);
        }


        int regularSummonCount = 0;
        EntityType<?> toadType = getEntityType("toad_2");
        for (LivingEntity summoned : active) {
            // Toads have their own separate limit.
            if (summoned.getType() == toadType) {
                continue;
            }
            regularSummonCount++;
        }


        // Find shikigami that are unlocked and under cap.
        List<Summon> eligible = new ArrayList<>();

        for (Summon summon : POOL) {

            if (!isUnlocked(owner, summon)) {
                continue;
            }

            EntityType<?> type =
                    getEntityType(summon.entityId());

            if (type == null) continue;

            int existing = 0;

            for (LivingEntity entity : active) {
                if (entity.getType() == type) {
                    existing++;
                }
            }

            boolean isToad = summon.entityId().equals("toad_2");
            boolean canSpawn =
                    existing < summon.perTypeLimit()
                            && (isToad || regularSummonCount < MAX_SUMMONS);

            if (canSpawn) {
                eligible.add(summon);
            }

        }

        if (eligible.isEmpty()) return;

        // Pick one available shikigami.
        Summon selected = eligible.get(
                level.random.nextInt(eligible.size())
        );

        spawn(level, inst, owner, selected);
    }


    private static boolean isUnlocked(
            LivingEntity owner,
            Summon summon
    ) {
        double state = owner.getPersistentData()
                .getDouble(
                        "TenShadowsTechnique"
                                + summon.nbtNumber()
                );

        // Permanently destroyed shikigami
        // cannot appear in the domain.
        if (state <= -2.0D) {
            return false;
        }

        // Starting Divine Dogs are innate to Ten Shadows.
        if (summon.nbtNumber() == 1
                || summon.nbtNumber() == 2) {
            return true;
        }

        // Explicitly tamed/unlocked.
        if (state == 1.0D) {
            return true;
        }

        // -1 means currently summoned.
        // This can include an untamed shikigami
        // undergoing its taming ritual.
        //
        // Do not treat this as proof of taming.
        if (state == -1.0D) {
            return false;
        }

        // For players, JC's advancement can also
        // establish that the shikigami was unlocked.
        if (owner instanceof ServerPlayer player) {

            Advancement adv = player.getServer()
                    .getAdvancements()
                    .getAdvancement(
                            new ResourceLocation(
                                    "jujutsucraft",
                                    summon.advancement()
                            )
                    );

            return adv != null
                    && player.getAdvancements()
                    .getOrStartProgress(adv)
                    .isDone();
        }

        return false;
    }


    private static EntityType<?> getEntityType(String name) {
        return ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation("jujutsucraft", name)
        );
    }

    private static void spawn(
            ServerLevel level,
            DomainInstance inst,
            LivingEntity owner,
            Summon summon
    ) {
        EntityType<?> type = getEntityType(
                summon.entityId()
        );

        if (type == null) return;

        // Try positions inside the central arena area.
        for (int attempt = 0; attempt < 20; attempt++) {

            int dx = level.random.nextInt(97) - 48;
            int dz = level.random.nextInt(97) - 48;

            if (dx * dx + dz * dz > 48 * 48) {
                continue;
            }

            BlockPos pos = inst.arenaCenter.offset(
                    dx, 1, dz
            );

            if (!level.getBlockState(pos).isAir()
                    || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }

            Entity summoned = type.create(level);
            if (summoned == null) return;

            summoned.moveTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );

            // Preserve Jujutsucraft's ownership,
            // scaling, and domain-entity safeguards.
            if (owner.getPersistentData()
                    .getDouble("friend_num") == 0.0D) {
                owner.getPersistentData().putDouble(
                        "friend_num",
                        level.random.nextDouble()
                );
            }

            ChimeraShadowGardenSetProcedure.execute(
                    owner,
                    summoned);

            if (summon.entityId().equals(
                    "eight_handled_sword_divergent_sila_divine_general_mahoraga")) {
                ScaleTypes.BASE.getScaleData(summoned).setScale(2.0F);}
            summoned.getPersistentData().putBoolean(
                    "abyss_chimera_summon",
                    true);

            // A domain shikigami should still have AI.
            if (summoned instanceof Mob mob) {
                mob.setNoAi(false);}

            level.addFreshEntity(summoned);
            return;
        }
    }

    public static void unsummonForClash(
            ServerLevel level,
            DomainInstance inst
    ) {
        if (level == null || inst == null) return;

        AABB arena = new AABB(
                inst.arenaCenter
        ).inflate(DomainDimension.OUTER_RADIUS + 4);

        for (Entity entity : level.getEntities(
                (Entity) null,
                arena,
                e -> e.getPersistentData()
                        .getBoolean("abyss_chimera_summon")
        )) {
            // Only remove this domain's generated copies.
            // Never remove ordinary Ten Shadows summons.
            entity.discard();
        }
    }

}
