package com.ghosts.abyss.domain;

import com.ghosts.abyss.RefinementPoints;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public final class DomainClash {

    private DomainClash() {}

    public static void tryStartClashInDimension(LivingEntity challenger) {
        if (!(challenger.level() instanceof ServerLevel domainLevel)) return;

        DomainInstance inst = findInstanceContaining(challenger.position());
        if (inst == null) {
            abortFailedDomainStart(challenger);
            return;
        }

        // Already the owner of this arena — don't restart domain; keep current domain
        if (inst.ownerId.equals(challenger.getUUID())) {
            challenger.getPersistentData().putBoolean("abyss_domain_handled", true);
            challenger.getPersistentData().putBoolean("abyss_domain_pending", false);
            // Clear soft-lock flags so abilities keep working while you own the domain
            challenger.getPersistentData().putBoolean("Failed", false);
            challenger.getPersistentData().putBoolean("Cover", false);
            challenger.getPersistentData().putBoolean("DomainDefeated", false);
            return;
        }

        var data = challenger.getPersistentData();
        double skillDomain = data.getDouble("skill_domain");
        if (skillDomain == 0) {
            skillDomain = data.getDouble("select");
            if (skillDomain != 0) {
                data.putDouble("skill_domain", skillDomain);
            }
        }

        // Must have a real domain id or JC will sit in a broken expand
        if (skillDomain == 0) {
            abortFailedDomainStart(challenger);
            return;
        }

        double cx = inst.arenaCenter.getX() + 0.5;
        double cy = inst.arenaCenter.getY();
        double cz = inst.arenaCenter.getZ() + 0.5;
        data.putDouble("x_pos_doma", cx);
        data.putDouble("y_pos_doma", cy);
        data.putDouble("z_pos_doma", cz);

        inst.returnPositions.putIfAbsent(
                challenger.getUUID(),
                inst.returnPositions.getOrDefault(challenger.getUUID(), challenger.position())
        );

        data.putBoolean("abyss_domain_handled", true);
        data.putBoolean("abyss_domain_pending", false);
        data.putBoolean("abyss_domain_transferring", false);
        data.putBoolean("abyss_domain_clashing", true);
        data.putBoolean("Failed", false);
        data.putBoolean("Cover", false);
        data.putBoolean("DomainDefeated", false);

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        if (domainEffect != null) {
            MobEffectInstance cur = challenger.getEffect(domainEffect);
            int amp = cur != null ? Math.max(cur.getAmplifier(), 1) : 1;
            int dur = cur != null ? Math.max(cur.getDuration(), 20 * 20) : DomainEvents.DOMAIN_DURATION_TICKS;
            // Do not constantly reset to full 2:40 every call — keep remaining time
            challenger.addEffect(new MobEffectInstance(
                    domainEffect, dur, amp, false, false, true));
        }

        LivingEntity owner = DomainArenaService.findLivingIn(domainLevel, inst.ownerId);
        if (owner != null && domainEffect != null) {
            MobEffectInstance oCur = owner.getEffect(domainEffect);
            if (oCur != null && oCur.getAmplifier() < 1) {
                owner.addEffect(new MobEffectInstance(
                        domainEffect, oCur.getDuration(), 1, false, false, true));
            }
            owner.getPersistentData().putBoolean("Failed", false);
        }
    }

    /** Undo a half-started domain so JC abilities work again */
    public static void abortFailedDomainStart(LivingEntity entity) {
        var data = entity.getPersistentData();
        data.putBoolean("abyss_domain_handled", false);
        data.putBoolean("abyss_domain_pending", false);
        data.putBoolean("abyss_domain_clashing", false);
        data.putBoolean("abyss_domain_transferring", true);

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        if (domainEffect != null) {
            entity.removeEffect(domainEffect);
        }

        data.putBoolean("Failed", false);
        data.putBoolean("Cover", false);
        data.putBoolean("DomainDefeated", false);
        // JC combat/technique gate
        data.putDouble("skill", 0);
        // Don't wipe skill_domain permanently if they already had a domain; only clear if we never joined
        // data.putDouble("skill_domain", 0);

        data.putBoolean("abyss_domain_transferring", false);
    }

    public static void joinAsClashParticipant(LivingEntity challenger, ServerLevel overworld,
                                              DomainInstance inst) {
        MinecraftServer server = overworld.getServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        if (domainLevel == null) return;

        DomainArenaService.forceLoadArena(domainLevel, inst);

        var data = challenger.getPersistentData();
        double skillDomain = data.getDouble("skill_domain");
        if (skillDomain == 0) {
            skillDomain = data.getDouble("select");
            if (skillDomain != 0) {
                data.putDouble("skill_domain", skillDomain);
            }
        }

        double cx = inst.arenaCenter.getX() + 0.5;
        double cy = inst.arenaCenter.getY();
        double cz = inst.arenaCenter.getZ() + 0.5;
        data.putDouble("x_pos_doma", cx);
        data.putDouble("y_pos_doma", cy);
        data.putDouble("z_pos_doma", cz);

        inst.returnPositions.putIfAbsent(challenger.getUUID(), challenger.position());

        data.putBoolean("abyss_domain_handled", true);
        data.putBoolean("abyss_domain_pending", false);
        data.putBoolean("abyss_domain_clashing", true);

        if (challenger instanceof Mob mob) {
            mob.getNavigation().stop(); // path only; AI can retarget next tick
        }


        double floorY = inst.arenaCenter.getY() + 1.1;
        data.putBoolean("abyss_domain_transferring", true);

        LivingEntity arrived = DomainTeleport.teleport(
                challenger, domainLevel, cx, floorY, cz
        );

        if (arrived == null || arrived.level() != domainLevel) {
            data.putBoolean("abyss_domain_transferring", false);
            abortFailedDomainStart(challenger);
            return;
        }

        arrived.getPersistentData().putBoolean(
                "abyss_domain_transferring", false
        );

        arrived.getPersistentData().putBoolean(
                "abyss_domain_clashing", true
        );

        arrived.getPersistentData().putBoolean(
                "abyss_domain_handled", true
        );

        arrived.getPersistentData().putBoolean(
                "abyss_domain_pending", false
        );

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                DomainEvents.DOMAIN_EFFECT_ID
        );

        if (domainEffect != null && !arrived.hasEffect(domainEffect)) {
            arrived.addEffect(new MobEffectInstance(
                    domainEffect,
                    DomainEvents.DOMAIN_DURATION_TICKS,
                    1,
                    false,
                    false,
                    true
            ));
        }

        LivingEntity owner = DomainArenaService.findLivingIn(domainLevel, inst.ownerId);
        if (owner != null && domainEffect != null) {
            MobEffectInstance oCur = owner.getEffect(domainEffect);
            if (oCur != null && oCur.getDuration() < DomainEvents.DOMAIN_DURATION_TICKS) {
                owner.addEffect(new MobEffectInstance(
                        domainEffect, DomainEvents.DOMAIN_DURATION_TICKS,
                        Math.max(oCur.getAmplifier(), 1),
                        false, false, true));
            }
        }
    }

    public static DomainInstance findInstanceContaining(Vec3 pos) {
        for (int i = 0; i < DomainDimension.MAX_SLOTS; i++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(i);
            if (inst == null) continue;

            double dx = pos.x - (inst.arenaCenter.getX() + 0.5);
            double dy = pos.y - inst.arenaCenter.getY();
            double dz = pos.z - (inst.arenaCenter.getZ() + 0.5);
            double r = DomainDimension.OUTER_RADIUS + 2;

            if (dx * dx + dy * dy + dz * dz <= r * r) {
                return inst;
            }
        }
        return null;
    }


    public static LivingEntity findActiveDomainInArena(
            ServerLevel domainLevel,
            DomainInstance inst,
            UUID excluding
    ) {
        if (domainLevel == null || inst == null) {
            return null;
        }

        MobEffect domainEffect =
                ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);

        if (domainEffect == null) {
            return null;
        }

        int r = DomainDimension.OUTER_RADIUS + 4;
        BlockPos c = inst.arenaCenter;

        AABB box = new AABB(
                c.getX() - r, c.getY() - r, c.getZ() - r,
                c.getX() + r, c.getY() + r, c.getZ() + r
        );

        for (LivingEntity candidate :
                domainLevel.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        entity -> true
                )) {

            // Never allow the current owner to be selected as their own challenger.
            if (candidate.getUUID().equals(excluding)) {
                continue;
            }

            if (candidate.isDeadOrDying()) {
                continue;
            }

            var data = candidate.getPersistentData();

            // This flag is set when an entity actually attempts to expand its domain
            // while another domain is already active.
            if (!data.getBoolean("abyss_domain_clashing")) {
                continue;
            }

            // A defeated domain user must not inherit the arena.
            if (data.getBoolean("DomainDefeated")) {
                continue;
            }

            // The entity must still have an active domain effect.
            if (!candidate.hasEffect(domainEffect)) {
                continue;
            }

            // Require a valid domain technique.
            double skillDomain = data.getDouble("skill_domain");

            if (skillDomain == 0) {
                skillDomain = data.getDouble("select");
            }

            if (skillDomain == 0) {
                continue;
            }

            // This entity has passed every check and is a valid active challenger.
            return candidate;
        }

        // No valid active challenger exists.
        return null;
    }


    public static void transferOwnership(DomainInstance inst, LivingEntity loser,
                                         LivingEntity winner, ServerLevel domainLevel) {
        UUID oldOwner = inst.ownerId;
        UUID newOwner = winner.getUUID();

        DomainInstanceManager.reassignOwner(oldOwner, newOwner);

        double skill = winner.getPersistentData().getDouble("skill_domain");
        if (skill == 0) {
            skill = winner.getPersistentData().getDouble("select");
        }
        if (skill != 0) {
            inst.skillDomain = skill;
        }

        winner.getPersistentData().putBoolean("abyss_domain_clashing", false);
        winner.getPersistentData().putBoolean("abyss_domain_handled", true);
        winner.getPersistentData().putDouble("x_pos_doma", inst.arenaCenter.getX() + 0.5);
        winner.getPersistentData().putDouble("y_pos_doma", inst.arenaCenter.getY());
        winner.getPersistentData().putDouble("z_pos_doma", inst.arenaCenter.getZ() + 0.5);
        winner.getPersistentData().putBoolean("Failed", false);
        winner.getPersistentData().putBoolean("Cover", false);
        winner.getPersistentData().putBoolean("DomainDefeated", false);
        if (winner instanceof ServerPlayer sp) {
            // Available points to spend in the phone — adjust amount as you like
            RefinementPoints.grantRefinementPoints(sp, 1);
        }
        loser.getPersistentData().putBoolean("abyss_domain_handled", false);
        loser.getPersistentData().putBoolean("abyss_domain_pending", false);
        loser.getPersistentData().putBoolean("abyss_domain_clashing", false);

        if (domainLevel != null) {
            DomainArenaBuilder.placeFloor(domainLevel, inst.arenaCenter, inst.skillDomain);
            DomainArenaBuilder.repairFloorCenter(domainLevel, inst.arenaCenter, inst.skillDomain, 12);
        }
    }
}