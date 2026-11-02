
package com.ghosts.abyss.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DomainClashController {

    private DomainClashController() {}

    // Maximum influence advantage from domain strength.
    // Small enough that equally matched domains fight
    // near the center instead of overwhelming each other.
    private static final double MAX_ADVANTAGE = 0.18;

    // How quickly power advantages change.
    private static final double SMOOTHING = 0.008;

    private static final Map<Integer, ClashState> STATES =
            new HashMap<>();

    private static final class ClashState {
        final Map<UUID, Double> influence = new HashMap<>();
    }

    public static void reset(int slot) {
        STATES.remove(slot);
        DomainClashVisuals.reset(slot);
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;

        Set<Integer> present = new HashSet<>();

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            DomainInstance inst =
                    DomainInstanceManager.getBySlot(slot);

            if (inst == null) continue;

            present.add(slot);

            List<DomainClashParticipants.Participant> active =
                    new ArrayList<>();

            Map<UUID, LivingEntity> casters = new HashMap<>();

            MobEffect domainEffect =
                    ForgeRegistries.MOB_EFFECTS.getValue(
                            DomainEvents.DOMAIN_EFFECT_ID
                    );

            for (DomainClashParticipants.Participant p
                    : inst.participants.all()) {

                LivingEntity entity =
                        DomainArenaService.findLivingIn(
                                level, p.entityId
                        );

                if (entity == null || !entity.isAlive()) {
                    continue;
                }

                boolean owner = p.entityId.equals(inst.ownerId);

                if (!owner && (
                        !entity.getPersistentData().getBoolean("abyss_domain_clashing")
                                || entity.getPersistentData().getBoolean("DomainDefeated")
                                || domainEffect == null
                                || !entity.hasEffect(domainEffect)
                )) {
                    continue;
                }

                active.add(p);
                casters.put(p.entityId, entity);

            }
// Remove challengers whose domain is no longer active.
// This frees their SOUTH/WEST/EAST position for future casters.
            java.util.List<UUID> inactiveIds = new java.util.ArrayList<>();

            for (DomainClashParticipants.Participant p : inst.participants.all()) {
                if (p.entityId.equals(inst.ownerId)) continue;

                LivingEntity challenger =
                        DomainArenaService.findLivingIn(level, p.entityId);

                if (challenger == null) {
                    // Do not automatically remove unloaded entities.
                    continue;
                }

                if (!challenger.isAlive()
                        || challenger.getPersistentData().getBoolean("DomainDefeated")
                        || !challenger.getPersistentData().getBoolean("abyss_domain_clashing")
                        || domainEffect == null
                        || !challenger.hasEffect(domainEffect)) {

                    inactiveIds.add(p.entityId);
                }
            }

            for (UUID id : inactiveIds) {
                inst.participants.remove(id);
            }

            if (!inactiveIds.isEmpty()) {
                DomainDecorations.sync(level, inst);
            }


            if (active.size() < 2) {

                // A clash has finished. Restore the surviving
                // domain's appearance once, not every tick.
                if (STATES.containsKey(slot)
                        && inst.participants.size() == 1) {

                    DomainClashVisuals.reset(slot);

                    DomainDecorations.clear(level, inst);

                    DomainArenaBuilder.rebuildAfterClash(
                            level,
                            inst.arenaCenter,
                            inst.skillDomain
                    );

                    DomainDecorations.spawn(level, inst);
                }

                STATES.remove(slot);
                continue;
            }


            ClashState state = STATES.computeIfAbsent(
                    slot, key -> new ClashState()
            );

            Set<UUID> activeIds = new HashSet<>();
            double totalPower = 0.0;

            Map<UUID, Double> powers = new HashMap<>();

            for (DomainClashParticipants.Participant p : active) {
                activeIds.add(p.entityId);

                LivingEntity entity = casters.get(p.entityId);

                double power = Math.max(
                        0.0, calculatePower(entity)
                );

                powers.put(p.entityId, power);
                totalPower += power;

                entity.getPersistentData().putBoolean(
                        "abyss_domain_clash_active", true
                );
            }

            state.influence.keySet().removeIf(
                    id -> !activeIds.contains(id)
            );

            double averagePower =
                    totalPower / active.size();

            for (DomainClashParticipants.Participant p : active) {
                double power = powers.get(p.entityId);

                double advantage =
                        (power - averagePower)
                                / Math.max(10.0, averagePower);

                double target = Math.max(
                        -MAX_ADVANTAGE,
                        Math.min(MAX_ADVANTAGE, advantage * 0.25)
                );

                double current = state.influence.getOrDefault(
                        p.entityId, 0.0
                );

                double next = current
                        + (target - current) * SMOOTHING;

                state.influence.put(p.entityId, next);
            }

            DomainClashVisuals.update(
                    level,
                    inst,
                    active,
                    state.influence
            );
        }

        STATES.keySet().removeIf(
                slot -> !present.contains(slot)
        );
    }

    public static double calculatePower(LivingEntity entity) {
        if (entity == null) return 0.0;

        MobEffect domainEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        DomainEvents.DOMAIN_EFFECT_ID
                );

        if (domainEffect == null) return 0.0;

        MobEffectInstance domain =
                entity.getEffect(domainEffect);

        if (domain == null) return 0.0;

        int strengthAmplifier = 0;

        if (entity.hasEffect(MobEffects.DAMAGE_BOOST)) {
            strengthAmplifier = entity.getEffect(
                    MobEffects.DAMAGE_BOOST
            ).getAmplifier();
        }

        double power = strengthAmplifier + 10.0;

        if (domain.getAmplifier() > 0) {
            power *= 1.15;
        }

        double skill = entity.getPersistentData()
                .getDouble("skill_domain");

        if (skill == 0.0) {
            skill = entity.getPersistentData()
                    .getDouble("select");
        }

        if (skill == 27.0) {
            power *= 1.5;
        } else if (skill == 29.0) {
            power *= 2.0;
        }

        double maxHealth = Math.max(
                1.0, entity.getMaxHealth()
        );

        double totalDamage = Math.max(
                0.0,
                entity.getPersistentData()
                        .getDouble("totalDamage")
        );

        double healthFactor = Math.max(
                maxHealth - totalDamage * 2.0,
                0.0
        ) / maxHealth;

        double duration = Math.min(
                domain.getDuration(), 1200
        );

        double timeFactor = Math.min(
                duration / 2400.0 + 0.5,
                1.0
        );

        return power * healthFactor * timeFactor;
    }
}
