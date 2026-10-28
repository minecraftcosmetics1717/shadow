
package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.procedures.DomainActiveProcedure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class DomainSureHit {

    private DomainSureHit() {}

    public static void tick(ServerLevel domainLevel) {
        if (!domainLevel.dimension().equals(DomainDimension.ID)) {
            return;
        }

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(slot);
            if (inst == null) continue;

            LivingEntity owner = DomainArenaService.findLivingIn(
                    domainLevel, inst.ownerId
            );

            if (owner == null || !owner.isAlive()) continue;

            MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                    DomainEvents.DOMAIN_EFFECT_ID
            );

            if (domainEffect == null || !owner.hasEffect(domainEffect)) {
                continue;
            }

            // Suppress sure-hit while another domain is challenging
            // the current owner.
            LivingEntity challenger = DomainClash.findActiveDomainInArena(
                    domainLevel, inst, inst.ownerId
            );

            if (challenger != null) continue;

            // Respect JC's failed-domain state.
            if (owner.getPersistentData().getBoolean("Failed")
                    || owner.getPersistentData().getBoolean("DomainDefeated")) {
                continue;
            }

            double skill = inst.skillDomain;

            // First implementation: only enable Dagon's domain.
            if (skill != 8.0) continue;

            var data = owner.getPersistentData();

            data.putDouble("skill_domain", skill);
            data.putDouble("x_pos_doma", inst.arenaCenter.getX() + 0.5);
            data.putDouble("y_pos_doma", inst.arenaCenter.getY()+1.5);
            data.putDouble("z_pos_doma", inst.arenaCenter.getZ() + 0.5);

            // Run JC's original domain-specific attack logic.
            // Do not run its full domain lifecycle procedure.
            DomainActiveProcedure.execute(
                    domainLevel,
                    owner.getX(),
                    owner.getY(),
                    owner.getZ(),
                    owner
            );
        }
    }
}
