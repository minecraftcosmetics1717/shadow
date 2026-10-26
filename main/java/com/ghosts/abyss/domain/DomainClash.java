package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
            challenger.getPersistentData().putBoolean("abyss_domain_handled", false);
            challenger.getPersistentData().putBoolean("abyss_domain_pending", false);
            return;
        }

        if (inst.ownerId.equals(challenger.getUUID())) {
            challenger.getPersistentData().putBoolean("abyss_domain_handled", true);
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

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        if (domainEffect != null) {
            MobEffectInstance cur = challenger.getEffect(domainEffect);
            int amp = cur != null ? Math.max(cur.getAmplifier(), 1) : 1;
            challenger.addEffect(new MobEffectInstance(
                    domainEffect, DomainEvents.DOMAIN_DURATION_TICKS, amp, false, false, true));
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

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        if (domainEffect != null) {
            MobEffectInstance cur = challenger.getEffect(domainEffect);
            int amp = cur != null ? Math.max(cur.getAmplifier(), 1) : 1;
            challenger.addEffect(new MobEffectInstance(
                    domainEffect, DomainEvents.DOMAIN_DURATION_TICKS,
                    amp, false, false, true));
        }

        double floorY = inst.arenaCenter.getY() + 1.1;
        data.putBoolean("abyss_domain_transferring", true);
        DomainTeleport.teleport(challenger, domainLevel, cx, floorY, cz);
        data.putBoolean("abyss_domain_transferring", false);

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

    public static LivingEntity findActiveDomainInArena(ServerLevel domainLevel,
                                                       DomainInstance inst,
                                                       UUID excluding) {
        if (domainLevel == null) return null;

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        if (domainEffect == null) return null;

        int r = DomainDimension.OUTER_RADIUS + 4;
        BlockPos c = inst.arenaCenter;
        AABB box = new AABB(
                c.getX() - r, c.getY() - r, c.getZ() - r,
                c.getX() + r, c.getY() + r, c.getZ() + r
        );

        LivingEntity best = null;
        for (LivingEntity e : domainLevel.getEntitiesOfClass(LivingEntity.class, box, ent -> true)) {
            if (e.getUUID().equals(excluding)) continue;
            if (e.isDeadOrDying()) continue;
            if (!e.hasEffect(domainEffect)) continue;

            if (e.getPersistentData().getBoolean("abyss_domain_clashing")) {
                return e;
            }
            if (best == null) {
                best = e;
            }
        }
        return best;
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

        loser.getPersistentData().putBoolean("abyss_domain_handled", false);
        loser.getPersistentData().putBoolean("abyss_domain_pending", false);
        loser.getPersistentData().putBoolean("abyss_domain_clashing", false);

        if (domainLevel != null) {
            DomainArenaBuilder.placeFloor(domainLevel, inst.arenaCenter, inst.skillDomain);
        }
    }
}