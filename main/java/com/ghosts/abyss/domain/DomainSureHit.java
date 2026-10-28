
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

// Abyss fast-cast domains don't necessarily receive JC's
// original domain_expansion effect.
            boolean abyssFastCast = inst.skillDomain == 8.0
                    || inst.skillDomain == 15.0;

            if (!abyssFastCast
                    && (domainEffect == null || !owner.hasEffect(domainEffect))) {
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

// Allow JC's domain dispatcher to select the
// active owner's domain-specific effect.
            if (skill <= 0.0) continue;

            var data = owner.getPersistentData();

            data.putDouble("skill_domain", skill);
            data.putDouble("x_pos_doma", inst.arenaCenter.getX() + 0.5);
            data.putDouble("y_pos_doma", inst.arenaCenter.getY() + 2);
            data.putDouble("z_pos_doma", inst.arenaCenter.getZ() + 0.5);
            if (skill == 8.0 && owner.tickCount % 100 == 0) {
                double jcRadius =
                        net.mcreator.jujutsucraft.network.JujutsucraftModVariables
                                .MapVariables.get(domainLevel)
                                .DomainExpansionRadius;

                com.ghosts.abyss.Abyss.LOGGER.info(
                        "[Abyss Dagon] owner={}, slot={}, ownerPos={}, " +
                                "arenaCenter={}, jcRadius={}, abyssRadius={}, " +
                                "domainX={}, domainY={}, domainZ={}",
                        owner.getName().getString(),
                        inst.slot,
                        owner.position(),
                        inst.arenaCenter,
                        jcRadius,
                        DomainDimension.INNER_RADIUS,
                        data.getDouble("x_pos_doma"),
                        data.getDouble("y_pos_doma"),
                        data.getDouble("z_pos_doma")
                );
            }
            // Run JC's original domain-specific attack logic.
            // Do not run its full domain lifecycle procedure.

// Run JC's domain attack logic with a controlled radius for Dagon.
            if (skill == 8.0) {
                var mapVars =
                        net.mcreator.jujutsucraft.network.JujutsucraftModVariables
                                .MapVariables.get(domainLevel);

                double originalRadius = mapVars.DomainExpansionRadius;
                double dagonRadius = 22.0;

                try {
                    mapVars.DomainExpansionRadius = dagonRadius;

                    // Log whenever the original radius differs.
                    if (originalRadius != dagonRadius) {
                        com.ghosts.abyss.Abyss.LOGGER.info(
                                "[Abyss DagonRadius] Correcting radius {} -> {} " +
                                        "for owner={} slot={}",
                                originalRadius,
                                dagonRadius,
                                owner.getName().getString(),
                                inst.slot
                        );
                    }

                    DomainActiveProcedure.execute(
                            domainLevel,
                            owner.getX(),
                            owner.getY(),
                            owner.getZ(),
                            owner
                    );
                } finally {
                    // Restore JC's original value immediately afterward.
                    mapVars.DomainExpansionRadius = originalRadius;
                }
            } else {
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
}
