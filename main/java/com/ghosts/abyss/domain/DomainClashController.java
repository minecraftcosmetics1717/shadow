
package com.ghosts.abyss.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DomainClashController {

    private DomainClashController() {}

    private static final Map<Integer, ClashState> STATES = new HashMap<>();

    private static final class ClashState {
        double challengerSkill;
        double territory = 0.0;
        boolean active = false;
    }

    public static void tick(ServerLevel level) {
        Set<Integer> present = new HashSet<>();

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(slot);
            if (inst == null) continue;

            present.add(slot);

            LivingEntity owner = DomainArenaService.findLivingIn(
                    level, inst.ownerId
            );

            LivingEntity challenger = DomainClash.findActiveDomainInArena(
                    level, inst, inst.ownerId
            );

            ClashState state = STATES.get(slot);

            if (owner == null || challenger == null) {
                if (state != null) {
                    state.active = false;
                    state.territory = Math.max(
                            0.0, state.territory - 0.025
                    );

                    DomainClashVisuals.update(
                            level,
                            inst,
                            inst.skillDomain,
                            state.challengerSkill,
                            state.territory
                    );

                    if (state.territory <= 0.0) {
                        STATES.remove(slot);
                    }
                }
                continue;
            }

            double challengerSkill = challenger.getPersistentData()
                    .getDouble("skill_domain");

            if (challengerSkill == 0.0) {
                challengerSkill = challenger.getPersistentData()
                        .getDouble("select");
            }

            if (challengerSkill == 0.0) continue;

            if (state == null) {
                state = new ClashState();
                STATES.put(slot, state);
            }

            state.active = true;
            state.challengerSkill = challengerSkill;

            double ownerPower = calculatePower(owner);
            double challengerPower = calculatePower(challenger);

            // A balanced clash gradually approaches 50/50.
            // A stronger domain gains more territory.
            double difference = challengerPower - ownerPower;

            double target = Math.max(
                    0.05,
                    Math.min(0.95, 0.5 + difference / 40.0)
            );

            state.territory += (target - state.territory) * 0.025;

            DomainClashVisuals.update(
                    level,
                    inst,
                    inst.skillDomain,
                    challengerSkill,
                    state.territory
            );

            // Keep both casters marked as participating in a clash.
            // Automatic defeat is deliberately not enabled yet.
            owner.getPersistentData().putBoolean(
                    "abyss_domain_clash_active", true
            );
            challenger.getPersistentData().putBoolean(
                    "abyss_domain_clash_active", true
            );
        }

        STATES.keySet().removeIf(slot -> !present.contains(slot));
    }

    public static double calculatePower(LivingEntity entity) {
        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                DomainEvents.DOMAIN_EFFECT_ID
        );

        if (domainEffect == null) return 0.0;

        MobEffectInstance domain = entity.getEffect(domainEffect);
        if (domain == null) return 0.0;

        // JC's base strength uses the Strength effect amplifier.
        int strengthAmplifier = 0;

        MobEffect strength = net.minecraft.world.effect.MobEffects.DAMAGE_BOOST;

        if (entity.hasEffect(strength)) {
            strengthAmplifier = entity.getEffect(strength).getAmplifier();
        }

        double power = strengthAmplifier + 10.0;

        if (domain.getAmplifier() > 0) {
            power *= 1.15;
        }

        double skill = entity.getPersistentData()
                .getDouble("skill_domain");

        if (skill == 0.0) {
            skill = entity.getPersistentData().getDouble("select");
        }

        if (skill == 27.0) {
            power *= 1.5;
        } else if (skill == 29.0) {
            power *= 2.0;
        }

        double maxHealth = Math.max(
                1.0,
                entity.getMaxHealth()
        );

        double totalDamage = Math.max(
                0.0,
                entity.getPersistentData().getDouble("totalDamage")
        );

        double healthFactor = Math.max(
                maxHealth - totalDamage * 2.0,
                0.0
        ) / maxHealth;

        double duration = Math.min(
                domain.getDuration(),
                1200
        );

        double timeFactor = Math.min(
                duration / 2400.0 + 0.5,
                1.0
        );

        return power * healthFactor * timeFactor;
    }
}
