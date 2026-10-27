package com.ghosts.abyss.domain;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

import static com.ghosts.abyss.domain.AbyssDomainCast.NBT_CANCEL_CHARGE;
import static com.ghosts.abyss.domain.AbyssDomainCast.NBT_CANCEL_CHARGING;

public class DomainLifecycle {

    private static final int OWNER_VERIFY_DELAY = 5;
    private static final int MAX_ENTER_RETRIES = 3;

    public static void tryStart(LivingEntity owner) {
        if (owner.level().isClientSide) return;

        if (owner.level().dimension().equals(DomainDimension.ID)) {
            DomainClash.tryStartClashInDimension(owner);
            return;
        }

        if (!(owner.level() instanceof ServerLevel overworld)) return;

        if (owner.getPersistentData().getBoolean("abyss_domain_handled")) return;
        if (DomainInstanceManager.getByOwner(owner.getUUID()) != null) return;

        var data = owner.getPersistentData();
        double skillDomain = data.getDouble("skill_domain");
        double ox = data.getDouble("x_pos_doma");
        double oy = data.getDouble("y_pos_doma");
        double oz = data.getDouble("z_pos_doma");

        boolean hasCenter = (ox != 0 || oy != 0 || oz != 0);
        if (!hasCenter) {
            owner.getPersistentData().putBoolean("abyss_domain_pending", true);
            return;
        }

        owner.getPersistentData().putBoolean("abyss_domain_handled", true);
        owner.getPersistentData().putBoolean("abyss_domain_pending", false);

        Vec3 overCenter = new Vec3(ox, oy, oz);
        double catchRadius = 22.0;

        DomainInstance near = findNearbyDomain(overCenter, catchRadius * 2.0);
        if (near != null) {
            DomainClash.joinAsClashParticipant(owner, overworld, near);
            return;
        }

        DomainInstance inst = DomainInstanceManager.claim(
                owner.getUUID(), skillDomain, overworld.dimension(), overCenter);
        if (inst == null) return;

        DomainInstance raced = findNearbyDomainExcluding(overCenter, catchRadius * 2.0, inst.slot);
        if (raced != null) {
            DomainInstanceManager.release(owner.getUUID());
            DomainClash.joinAsClashParticipant(owner, overworld, raced);
            return;
        }

        MinecraftServer server = overworld.getServer();
        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        if (domainLevel == null) {
            DomainInstanceManager.release(owner.getUUID());
            return;
        }

        DomainArenaService.forceLoadArena(domainLevel, inst);
        DomainArenaBuilder.build(domainLevel, inst.arenaCenter, skillDomain);

        if (owner instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        inst.returnPositions.put(owner.getUUID(), owner.position());

        double floorY = inst.arenaCenter.getY() + 1.1;
        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        owner.getPersistentData().putBoolean("abyss_domain_transferring", true);
        DomainTeleport.teleport(owner, domainLevel, cx, floorY, cz);

        scheduleFinishEnter(server, owner.getUUID(), inst.slot, overCenter, catchRadius, 0);
    }
    public static void startAbyssDomain(LivingEntity owner, double skillDomain,
                                        Vec3 overCenter, double catchRadius) {
        if (owner.level().isClientSide) return;
        if (!(owner.level() instanceof ServerLevel overworld)) return;
        if (DomainInstanceManager.getByOwner(owner.getUUID()) != null) return;

        owner.getPersistentData().putBoolean("abyss_domain_handled", true);
        owner.getPersistentData().putBoolean("abyss_domain_pending", false);

        DomainInstance near = findNearbyDomain(overCenter, catchRadius * 2.0);
        if (near != null) {
            DomainClash.joinAsClashParticipant(owner, overworld, near);
            return;
        }

        DomainInstance inst = DomainInstanceManager.claim(
                owner.getUUID(), skillDomain, overworld.dimension(), overCenter);
        if (inst == null) return;

        DomainInstance raced = findNearbyDomainExcluding(overCenter, catchRadius * 2.0, inst.slot);
        if (raced != null) {
            DomainInstanceManager.release(owner.getUUID());
            DomainClash.joinAsClashParticipant(owner, overworld, raced);
            return;
        }

        var server = overworld.getServer();
        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        if (domainLevel == null) {
            DomainInstanceManager.release(owner.getUUID());
            return;
        }

        DomainArenaService.forceLoadArena(domainLevel, inst);
        DomainArenaBuilder.build(domainLevel, inst.arenaCenter, skillDomain);

        inst.returnPositions.put(owner.getUUID(), owner.position());

        double floorY = inst.arenaCenter.getY() + 1.1;
        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        owner.getPersistentData().putBoolean("abyss_domain_transferring", true);
        DomainTeleport.teleport(owner, domainLevel, cx, floorY, cz);

        scheduleFinishEnter(server, owner.getUUID(), inst.slot, overCenter, catchRadius, 0);
    }
    private static void scheduleFinishEnter(MinecraftServer server, UUID ownerId, int slot,
                                            Vec3 overCenter, double catchRadius, int attempt) {
        int when = server.getTickCount() + OWNER_VERIFY_DELAY;
        server.tell(new TickTask(when, () ->
                finishEnter(ownerId, slot, overCenter, catchRadius, attempt)));
    }

    private static void finishEnter(UUID ownerId, int slot, Vec3 overCenter,
                                    double catchRadius, int attempt) {
        DomainInstance inst = DomainInstanceManager.getBySlot(slot);
        if (inst == null) return;
        if (!ownerId.equals(inst.ownerId)) return;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        ServerLevel overworld = server.getLevel(inst.returnDim);
        if (overworld == null) overworld = server.overworld();


        LivingEntity ownerInDim = DomainArenaService.findLivingIn(domainLevel, ownerId);

        if (ownerInDim == null) {
            LivingEntity ownerOver = DomainArenaService.findLivingIn(overworld, ownerId);
            if (ownerOver != null && attempt < MAX_ENTER_RETRIES) {
                double floorY = inst.arenaCenter.getY() + 1.1;
                double cx = inst.arenaCenter.getX() + 0.5;
                double cz = inst.arenaCenter.getZ() + 0.5;

                if (ownerOver instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }

                ownerOver.getPersistentData().putBoolean("abyss_domain_transferring", true);
                DomainTeleport.teleport(ownerOver, domainLevel, cx, floorY, cz);
                scheduleFinishEnter(server, ownerId, slot, overCenter, catchRadius, attempt + 1);
                return;
            }

            softAbort(inst, domainLevel, overworld, ownerId);
            return;
        }

        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        ownerInDim.getPersistentData().putDouble("x_pos_doma", cx);
        ownerInDim.getPersistentData().putDouble("y_pos_doma", inst.arenaCenter.getY());
        ownerInDim.getPersistentData().putDouble("z_pos_doma", cz);
        ownerInDim.getPersistentData().putDouble("skill_domain", inst.skillDomain);
        ownerInDim.getPersistentData().putBoolean("abyss_domain_handled", true);
        ownerInDim.getPersistentData().putBoolean("abyss_domain_transferring", false);
        var jcDomain = ForgeRegistries.MOB_EFFECTS.getValue(
                DomainEvents.DOMAIN_EFFECT_ID
        );

        if (jcDomain != null && !ownerInDim.hasEffect(jcDomain)) {
            ownerInDim.addEffect(
                    new net.minecraft.world.effect.MobEffectInstance(
                            jcDomain,
                            DomainEvents.DOMAIN_DURATION_TICKS,
                            0,
                            false,
                            false,
                            true
                    )
            );
        }

        if (ownerInDim instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        pullOthers(overworld, domainLevel, inst, overCenter, catchRadius, ownerInDim);
    }

    private static void softAbort(DomainInstance inst, ServerLevel domainLevel,
                                  ServerLevel overworld, UUID ownerId) {
        if (domainLevel != null) {
            // Find the owner if they were successfully transferred into the domain.
            LivingEntity inDim = DomainArenaService.findLivingIn(domainLevel, ownerId);

            // If the owner is stranded in the domain dimension, send them back.
            if (inDim != null && overworld != null) {
                Vec3 back = inst.returnPositions.getOrDefault(
                        ownerId,
                        inst.overworldCenter
                );

                inDim.getPersistentData().putBoolean(
                        "abyss_domain_transferring", true
                );

                DomainTeleport.sendBack(inDim, overworld, back);

                inDim.getPersistentData().putBoolean(
                        "abyss_domain_transferring", false
                );
            }

            // Clean up the failed arena.
            DomainInstanceManager.clearArena(domainLevel, inst);
            DomainArenaService.unloadArena(domainLevel, inst);
        }

        // Release the owner's reserved domain slot.
        DomainInstanceManager.release(ownerId);

        // Clear the cast state wherever the owner currently exists.
        LivingEntity owner = null;

        if (overworld != null) {
            owner = DomainArenaService.findLivingIn(overworld, ownerId);
        }

        if (owner == null && domainLevel != null) {
            owner = DomainArenaService.findLivingIn(domainLevel, ownerId);
        }

        if (owner != null) {
            clearCastState(owner);
        }
    }

    private static DomainInstance findNearbyDomain(Vec3 center, double maxDist) {
        for (int i = 0; i < DomainDimension.MAX_SLOTS; i++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(i);
            if (inst == null) continue;
            if (inst.overworldCenter.distanceTo(center) < maxDist) {
                return inst;
            }
        }
        return null;
    }

    private static DomainInstance findNearbyDomainExcluding(Vec3 center, double maxDist, int excludeSlot) {
        for (int i = 0; i < DomainDimension.MAX_SLOTS; i++) {
            if (i == excludeSlot) continue;
            DomainInstance inst = DomainInstanceManager.getBySlot(i);
            if (inst == null) continue;
            if (inst.overworldCenter.distanceTo(center) < maxDist) {
                return inst;
            }
        }
        return null;
    }

    private static void pullOthers(ServerLevel overworld, ServerLevel domainLevel,
                                   DomainInstance inst, Vec3 center, double radius,
                                   LivingEntity ownerInDim) {
        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<LivingEntity> list = overworld.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e.distanceToSqr(center) <= radius * radius
                        && !e.getUUID().equals(ownerInDim.getUUID())
        );

        Vec3 ownerReturn = inst.returnPositions.getOrDefault(ownerInDim.getUUID(), center);
        double maxSpawnR = DomainDimension.INNER_RADIUS - 3;
        double floorY = inst.arenaCenter.getY() + 1.1;
        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        for (LivingEntity e : list) {
            UUID id = e.getUUID();
            inst.returnPositions.putIfAbsent(id, e.position());

            double ox = e.getX() - ownerReturn.x;
            double oz = e.getZ() - ownerReturn.z;
            double horiz = Math.sqrt(ox * ox + oz * oz);
            if (horiz > maxSpawnR && horiz > 0.001) {
                double scale = maxSpawnR / horiz;
                ox *= scale;
                oz *= scale;
            }
            DomainTeleport.teleport(e, domainLevel, cx + ox, floorY, cz + oz);
        }
    }

    public static void endByOwnerId(UUID ownerId) {
        DomainInstance inst = DomainInstanceManager.getByOwner(ownerId);
        if (inst == null) return;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        ServerLevel returnLevel = server.getLevel(inst.returnDim);
        if (returnLevel == null) returnLevel = server.overworld();

        final ServerLevel over = returnLevel;
        final Vec3 overCenter = inst.overworldCenter;

        DomainOverworldShell.clear(returnLevel, overCenter, AbyssDomainCast.OVERWORLD_RADIUS);
        over.getChunkAt(BlockPos.containing(overCenter));

        if (domainLevel != null) {
            DomainArenaService.evacuateArena(domainLevel, over, inst);
            DomainArenaService.sweepArenaItems(domainLevel, over, inst);
            DomainArenaService.cleanupDomainMobs(domainLevel, inst);
            DomainInstanceManager.clearArena(domainLevel, inst);
            DomainArenaService.unloadArena(domainLevel, inst);
        }

        DomainOverworldCleanup.triggerJcDomainBreak(over, overCenter);
        server.tell(new TickTask(server.getTickCount() + 20,
                () -> DomainOverworldCleanup.triggerJcDomainBreak(over, overCenter)));
        server.tell(new TickTask(server.getTickCount() + 40,
                () -> DomainOverworldCleanup.triggerJcDomainBreak(over, overCenter)));

        DomainInstanceManager.release(ownerId);

        // Unlock techniques after death-end
        LivingEntity owner = DomainArenaService.findLivingIn(over, ownerId);
        if (owner == null && domainLevel != null) {
            owner = DomainArenaService.findLivingIn(domainLevel, ownerId);
        }
        if (owner != null) {
            clearCastState(owner);
        }
    }

    public static void clearCastState(LivingEntity entity) {
        if (entity == null) return;
        var data = entity.getPersistentData();
        data.putBoolean("abyss_domain_active", false);
        data.putBoolean("abyss_domain_committed", false);
        data.putBoolean("abyss_domain_handled", false);
        data.putBoolean("abyss_domain_pending", false);
        data.putBoolean("abyss_domain_clashing", false);
        data.putBoolean("abyss_domain_transferring", false);
        data.putBoolean("abyss_dagon_domain_auth", false);
        data.putBoolean("abyss_cancel_domain_auth", false);
        data.putBoolean("abyss_domain_cancel_charging", false);
        data.putInt("abyss_domain_cancel_charge", 0);
        data.putLong("abyss_domain_end_tick", 0L);

        data.putDouble("cnt1", 0);
        data.putDouble("cnt2", 0);
        data.putDouble("cnt3", 0);
        data.putDouble("skill", 0);
        data.putDouble("select", 0);

        data.putBoolean("Failed", false);
        data.putBoolean("Cover", false);
        data.putBoolean("DomainDefeated", false);

        var ct = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "cursed_technique"));
        if (ct != null) entity.removeEffect(ct);

        var domain = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "domain_expansion"));
        if (domain != null && entity.hasEffect(domain)) {
            data.putBoolean("abyss_domain_transferring", true);
            data.putBoolean("abyss_domain_suppress_jc_expire", true);

            try {
                entity.removeEffect(domain);
            } finally {
                data.putBoolean("abyss_domain_suppress_jc_expire", false);
                data.putBoolean("abyss_domain_transferring", false);
            }
        }
    }

    public static void end(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (entity.getPersistentData().getBoolean("abyss_domain_transferring")) return;

        DomainInstance asOwner = DomainInstanceManager.getByOwner(entity.getUUID());
        if (asOwner == null) {
            // Still unlock even if instance already gone (JC cancel race)
            clearCastState(entity);
            return;
        }

        MinecraftServer server = entity.level().getServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        ServerLevel returnLevel = server.getLevel(asOwner.returnDim);
        if (returnLevel == null) returnLevel = server.overworld();

        LivingEntity winner = null;
        if (domainLevel != null) {
            winner = DomainClash.findActiveDomainInArena(domainLevel, asOwner, entity.getUUID());
        }
        if (winner != null) {
            DomainClash.transferOwnership(asOwner, entity, winner, domainLevel);
            clearCastState(entity); // loser must not stay soft-locked
            return;
        }

        entity.getPersistentData().putDouble("x_pos_doma", asOwner.overworldCenter.x);
        entity.getPersistentData().putDouble("y_pos_doma", asOwner.overworldCenter.y);
        entity.getPersistentData().putDouble("z_pos_doma", asOwner.overworldCenter.z);

        returnLevel.getChunkAt(BlockPos.containing(asOwner.overworldCenter));
        DomainOverworldShell.clear(returnLevel, asOwner.overworldCenter, AbyssDomainCast.OVERWORLD_RADIUS);

        if (domainLevel != null) {
            DomainArenaService.evacuateArena(domainLevel, returnLevel, asOwner);
            DomainArenaService.sweepArenaItems(domainLevel, returnLevel, asOwner);
            DomainArenaService.cleanupDomainMobs(domainLevel, asOwner);
            DomainInstanceManager.clearArena(domainLevel, asOwner);
            DomainArenaService.unloadArena(domainLevel, asOwner);
        }

        DomainOverworldCleanup.triggerJcDomainBreak(returnLevel, asOwner.overworldCenter);

        final Vec3 overCenter = asOwner.overworldCenter;
        final ServerLevel over = returnLevel;
        server.tell(new TickTask(server.getTickCount() + 20,
                () -> DomainOverworldCleanup.triggerJcDomainBreak(over, overCenter)));
        server.tell(new TickTask(server.getTickCount() + 40,
                () -> DomainOverworldCleanup.triggerJcDomainBreak(over, overCenter)));

        DomainInstanceManager.release(entity.getUUID());
        clearCastState(entity);
    }}