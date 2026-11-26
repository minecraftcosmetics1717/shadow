package com.ghosts.abyss.domain;

import net.mcreator.jujutsucraft.procedures.DomainActiveProcedure;
import net.mcreator.jujutsucraft.procedures.TsukumoDomainExpansionActiveProcedure;
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

            // Ryu's animated domain decorations are visual effects,
            // not a damaging sure-hit.
            // Keep them updating even during domain clashes.
            for (DomainClashParticipants.Participant participant
                    : inst.participants.all()) {

                if ((int) participant.skillDomain != 12) {
                    continue;
                }

                LivingEntity ryu = DomainArenaService.findLivingIn(
                        domainLevel,
                        participant.entityId
                );

                if (ryu == null || !ryu.isAlive()
                        || ryu.getPersistentData()
                        .getBoolean("DomainDefeated")) {
                    continue;
                }

                RyuDomainDecorations.tick(
                        domainLevel,
                        inst,
                        ryu
                );
            }

            LivingEntity owner = DomainArenaService.findLivingIn(
                    domainLevel, inst.ownerId
            );


            if (owner == null || !owner.isAlive()) continue;

            MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                    DomainEvents.DOMAIN_EFFECT_ID
            );

// Abyss fast-cast domains don't necessarily receive JC's
// original domain_expansion effect.
            boolean abyssFastCast = inst.skillDomain == 4.0
                    || inst.skillDomain == 8.0
                    || inst.skillDomain == 9.0
                    || inst.skillDomain == 15.0;

            if (!abyssFastCast
                    && (domainEffect == null || !owner.hasEffect(domainEffect))) {
                continue;
            }

            // Suppress sure-hit while another domain is challenging
            // the current owner.
            LivingEntity challenger = DomainClash.findActiveDomainInArena(
                    domainLevel, inst, inst.ownerId);


            if (challenger != null) {

                // Remove Dagon's fish when a domain clash begins.
                if ((int) inst.skillDomain == 8) {
                    DagonDomainFish.clear(
                            domainLevel,
                            inst
                    );
                }

                // Keep Megumi's existing clash cleanup.
                ChimeraShadowGardenSureHit.unsummonForClash(
                        domainLevel,
                        inst
                );

                continue;
            }


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
            // Sure-hit is active, so keep neutralization
// applied to living entities inside this domain.
            DomainNeutralization.refresh(domainLevel, inst);

            // Use Abyss's larger arena radius for sure-hit
            // while preserving Dagon's special behavior.
            var mapVars =
                    net.mcreator.jujutsucraft.network.JujutsucraftModVariables
                            .MapVariables.get(domainLevel);

            double originalRadius = mapVars.DomainExpansionRadius;

            double targetRadius =
                    skill == 8.0
                            ? 22.0
                            : DomainDimension.INNER_RADIUS;

            try {
                mapVars.DomainExpansionRadius = targetRadius;



                if (skill == 8.0D) {

                    // Dagon — autonomous Death Swarm.
                    // Fish spawn in groups and hunt independently.
                    DagonDomainFish.spawnGroup(
                            domainLevel,
                            inst,
                            owner
                    );

                } else if (skill == 6.0D) {

                    ChimeraShadowGardenSureHit.tick(
                            domainLevel,
                            inst,
                            owner);


                }

                else if (skill == 9.0D) {

                    // Yuki Tsukumo's domain.
                    // Run the original JC combat behavior
                    // once every 5 ticks.
                    //
                    // This prevents the effect from activating
                    // every tick if JC's original domain effect
                    // is missing.

                    if (domainLevel.getGameTime() % 5L == 0L) {
                        TsukumoDomainExpansionActiveProcedure.execute(
                                domainLevel,
                                owner
                        );
                    }}
                else if (skill == 12.0D) {

                    // Ryu's decorations were already updated
                    // before the sure-hit and clash checks.
                    // No additional sure-hit behavior yet.

                } else if (skill == 27.0D) {
                    // Deadly Sentencing: Abyss will manage trials.
                    // Do not run JC's original trial procedure.

                } else {



                    // All other domains continue using
                    // their existing Jujutsucraft behavior.
                    DomainActiveProcedure.execute(
                            domainLevel,
                            owner.getX(),
                            owner.getY(),
                            owner.getZ(),
                            owner);
                }


            } finally {
                // Restore Jujutsucraft's shared setting.
                mapVars.DomainExpansionRadius = originalRadius;
            }



        }
    }
}
