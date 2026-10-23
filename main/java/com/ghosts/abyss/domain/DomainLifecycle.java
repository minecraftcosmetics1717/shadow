package com.ghosts.abyss.domain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class DomainLifecycle {

    private static final TicketType<ChunkPos> DOMAIN_TICKET =
            TicketType.create("abyss:domain", Comparator.comparingLong(ChunkPos::toLong), 300);

    private static final int TICKET_RADIUS = 5;
    private static final int OWNER_VERIFY_DELAY = 5;
    private static final int MAX_ENTER_RETRIES = 3;

    public static void tryStart(LivingEntity owner) {
        if (owner.level().isClientSide) return;
        if (!(owner.level() instanceof ServerLevel overworld)) return;

        if (owner.getPersistentData().getBoolean("abyss_domain_handled")) return;
        if (DomainInstanceManager.getByOwner(owner.getUUID()) != null) return;

        var data = owner.getPersistentData();
        double skillDomain = data.getDouble("skill_domain");
        double ox = data.getDouble("x_pos_doma");
        double oy = data.getDouble("y_pos_doma");
        double oz = data.getDouble("z_pos_doma");

        // Need real domain center — not just keys existing at 0
        boolean hasCenter = (ox != 0 || oy != 0 || oz != 0);
        boolean hasSkill = skillDomain != 0;
        if (!hasCenter && !hasSkill) {
            owner.getPersistentData().putBoolean("abyss_domain_pending", true);
            return;
        }
        // Prefer waiting until center exists (JC writes it in CreateBarrier)
        if (!hasCenter) {
            owner.getPersistentData().putBoolean("abyss_domain_pending", true);
            return;
        }

        owner.getPersistentData().putBoolean("abyss_domain_handled", true);
        owner.getPersistentData().putBoolean("abyss_domain_pending", false);

        Vec3 overCenter = new Vec3(ox, oy, oz);
        double catchRadius = DomainDimension.OUTER_RADIUS;

        DomainInstance near = findNearbyDomain(overCenter, catchRadius * 2.0);
        if (near != null) {
            joinExisting(owner, overworld, near, catchRadius);
            return;
        }

        DomainInstance inst = DomainInstanceManager.claim(
                owner.getUUID(), skillDomain, overworld.dimension(), overCenter);
        if (inst == null) return;

        MinecraftServer server = overworld.getServer();
        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        if (domainLevel == null) {
            DomainInstanceManager.release(owner.getUUID());
            return;
        }

        forceLoadArena(domainLevel, inst);
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
        LivingEntity arrived = DomainTeleport.teleport(owner, domainLevel, cx, floorY, cz);

        UUID ownerId = owner.getUUID();
        // If clone returned a living entity, prefer that UUID (same id if we preserved it)
        if (arrived != null) {
            ownerId = arrived.getUUID();
            // Re-key instance if needed (should be same UUID)
            if (DomainInstanceManager.getByOwner(ownerId) == null
                    && DomainInstanceManager.getBySlot(inst.slot) != null) {
                // already keyed by original owner UUID
            }
        }

        final UUID finalOwnerId = owner.getUUID();
        final int slot = inst.slot;
        final Vec3 center = overCenter;
        final double radius = catchRadius;

        scheduleFinishEnter(server, finalOwnerId, slot, center, radius, 0);
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
        if (domainLevel == null) {
            softAbort(inst, null, overworld, ownerId);
            return;
        }

        LivingEntity ownerInDim = findLivingIn(domainLevel, ownerId);

        // Still in overworld with effect? retry teleport
        if (ownerInDim == null) {
            LivingEntity ownerOver = findLivingIn(overworld, ownerId);
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

        if (ownerInDim instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        pullOthers(overworld, domainLevel, inst, overCenter, catchRadius, ownerInDim);
    }

    private static void softAbort(DomainInstance inst, ServerLevel domainLevel,
                                  ServerLevel overworld, UUID ownerId) {
        if (domainLevel != null) {
            LivingEntity inDim = findLivingIn(domainLevel, ownerId);
            if (inDim != null && overworld != null) {
                Vec3 back = inst.returnPositions.getOrDefault(ownerId, inst.overworldCenter);
                inDim.getPersistentData().putBoolean("abyss_domain_transferring", true);
                DomainTeleport.sendBack(inDim, overworld, back);
                inDim.getPersistentData().putBoolean("abyss_domain_transferring", false);
            }
            DomainInstanceManager.clearArena(domainLevel, inst);
            unloadArena(domainLevel, inst);
        }

        DomainInstanceManager.release(ownerId);

        if (overworld != null) {
            LivingEntity owner = findLivingIn(overworld, ownerId);
            if (owner != null) {
                owner.getPersistentData().putBoolean("abyss_domain_handled", false);
                owner.getPersistentData().putBoolean("abyss_domain_pending", false);
                owner.getPersistentData().putBoolean("abyss_domain_transferring", false);
            }
        }
    }

    private static void forceLoadArena(ServerLevel level, DomainInstance inst) {
        ChunkPos cp = new ChunkPos(inst.arenaCenter);
        level.getChunkSource().addRegionTicket(DOMAIN_TICKET, cp, TICKET_RADIUS, cp);
        level.getChunk(cp.x, cp.z);
    }

    private static void unloadArena(ServerLevel level, DomainInstance inst) {
        ChunkPos cp = new ChunkPos(inst.arenaCenter);
        level.getChunkSource().removeRegionTicket(DOMAIN_TICKET, cp, TICKET_RADIUS, cp);
    }

    private static LivingEntity findLivingIn(ServerLevel level, UUID id) {
        if (level == null) return null;
        Entity e = level.getEntity(id);
        if (e instanceof LivingEntity living) return living;
        for (Entity ent : level.getAllEntities()) {
            if (id.equals(ent.getUUID()) && ent instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
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

    private static void joinExisting(LivingEntity owner, ServerLevel overworld,
                                     DomainInstance inst, double catchRadius) {
        MinecraftServer server = overworld.getServer();
        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        if (domainLevel == null) return;

        forceLoadArena(domainLevel, inst);

        double floorY = inst.arenaCenter.getY() + 1.1;
        double cx = inst.arenaCenter.getX() + 0.5;
        double cz = inst.arenaCenter.getZ() + 0.5;

        inst.returnPositions.putIfAbsent(owner.getUUID(), owner.position());
        if (owner instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        owner.getPersistentData().putBoolean("abyss_domain_transferring", true);
        DomainTeleport.teleport(owner, domainLevel, cx, floorY, cz);

        scheduleFinishEnter(server, owner.getUUID(), inst.slot, inst.overworldCenter, catchRadius, 0);
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

    private static void triggerJcDomainBreak(ServerLevel overworld, Vec3 center) {
        AABB box = new AABB(
                center.x - 2, center.y - 2, center.z - 2,
                center.x + 2, center.y + 2, center.z + 2
        );
        for (Entity e : overworld.getEntities((Entity) null, box, ent -> true)) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (id != null && "jujutsucraft".equals(id.getNamespace())
                    && id.getPath().contains("domain_expansion")) {
                e.getPersistentData().putBoolean("Break", true);
            }
        }
    }

    public static void end(LivingEntity owner) {
        if (owner.level().isClientSide) return;
        if (owner.getPersistentData().getBoolean("abyss_domain_transferring")) return;

        DomainInstance inst = DomainInstanceManager.getByOwner(owner.getUUID());
        if (inst == null) {
            owner.getPersistentData().putBoolean("abyss_domain_handled", false);
            owner.getPersistentData().putBoolean("abyss_domain_pending", false);
            return;
        }

        MinecraftServer server = owner.level().getServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        ServerLevel returnLevel = server.getLevel(inst.returnDim);
        if (returnLevel == null) returnLevel = server.overworld();

        owner.getPersistentData().putDouble("x_pos_doma", inst.overworldCenter.x);
        owner.getPersistentData().putDouble("y_pos_doma", inst.overworldCenter.y);
        owner.getPersistentData().putDouble("z_pos_doma", inst.overworldCenter.z);

        if (domainLevel != null) {
            for (var entry : inst.returnPositions.entrySet()) {
                Entity e = domainLevel.getEntity(entry.getKey());
                if (e == null) e = returnLevel.getEntity(entry.getKey());
                if (e instanceof LivingEntity living) {
                    DomainTeleport.sendBack(living, returnLevel, entry.getValue());
                }
            }
            DomainInstanceManager.clearArena(domainLevel, inst);
            unloadArena(domainLevel, inst);
        }

        triggerJcDomainBreak(returnLevel, inst.overworldCenter);
        DomainInstanceManager.release(owner.getUUID());

        owner.getPersistentData().putBoolean("abyss_domain_handled", false);
        owner.getPersistentData().putBoolean("abyss_domain_pending", false);
        owner.getPersistentData().putBoolean("abyss_domain_transferring", false);
    }
}