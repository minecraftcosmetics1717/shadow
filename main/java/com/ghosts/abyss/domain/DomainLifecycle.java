package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class DomainLifecycle {

    private static final TicketType<ChunkPos> DOMAIN_TICKET =
            TicketType.create("abyss:domain", Comparator.comparingLong(ChunkPos::toLong), 300);

    private static final int TICKET_RADIUS = 5;
    private static final int OWNER_VERIFY_DELAY = 5;
    private static final int MAX_ENTER_RETRIES = 3;
    private static final int DOMAIN_DURATION_TICKS = 20 * 160;
    private static final ResourceLocation DOMAIN_EFFECT_ID =
            new ResourceLocation("jujutsucraft", "domain_expansion");
    public static void tryStart(LivingEntity owner) {
        if (owner.level().isClientSide) return;

        // Already inside domain dimension → clash, don't build a new arena
        if (owner.level().dimension().equals(DomainDimension.ID)) {
            tryStartClashInDimension(owner);
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
            joinAsClashParticipant(owner, overworld, near);
            return;
        }


        DomainInstance inst = DomainInstanceManager.claim(
                owner.getUUID(), skillDomain, overworld.dimension(), overCenter);
        if (inst == null) return;

// Someone else claimed a nearby domain in the same tick
        DomainInstance raced = findNearbyDomainExcluding(
                overCenter, catchRadius * 2.0, inst.slot);
        if (raced != null) {
            DomainInstanceManager.release(owner.getUUID());
            joinAsClashParticipant(owner, overworld, raced);
            return;
        }

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
        if (domainLevel == null) {
            softAbort(inst, null, overworld, ownerId);
            return;
        }

        LivingEntity ownerInDim = findLivingIn(domainLevel, ownerId);

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

    private static DomainInstance findNearbyDomainExcluding(
            Vec3 center, double maxDist, int excludeSlot) {
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

    /** Pull into an existing arena and treat this expander as a domain clasher. */
    private static void joinAsClashParticipant(
            LivingEntity challenger, ServerLevel overworld, DomainInstance inst) {
        MinecraftServer server = overworld.getServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        if (domainLevel == null) return;

        forceLoadArena(domainLevel, inst);

        var data = challenger.getPersistentData();
        double skillDomain = data.getDouble("skill_domain");
        if (skillDomain == 0) {
            skillDomain = data.getDouble("select");
            if (skillDomain != 0) {
                data.putDouble("skill_domain", skillDomain);
            }
        }

        // Same arena center as defender — JC clash / sure-hit use this
        double cx = inst.arenaCenter.getX() + 0.5;
        double cy = inst.arenaCenter.getY();
        double cz = inst.arenaCenter.getZ() + 0.5;
        data.putDouble("x_pos_doma", cx);
        data.putDouble("y_pos_doma", cy);
        data.putDouble("z_pos_doma", cz);

        inst.returnPositions.putIfAbsent(
                challenger.getUUID(), challenger.position());

        data.putBoolean("abyss_domain_handled", true);
        data.putBoolean("abyss_domain_pending", false);
        data.putBoolean("abyss_domain_clashing", true);

        if (challenger instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        // Keep / refresh domain effect so they stay in the clash
        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "domain_expansion"));
        if (domainEffect != null) {
            MobEffectInstance cur = challenger.getEffect(domainEffect);
            int amp = cur != null ? Math.max(cur.getAmplifier(), 1) : 1;
            challenger.addEffect(new MobEffectInstance(
                    domainEffect, DOMAIN_DURATION_TICKS,
                    amp, false, false, true));
        }

        double floorY = inst.arenaCenter.getY() + 1.1;
        data.putBoolean("abyss_domain_transferring", true);
        DomainTeleport.teleport(challenger, domainLevel, cx, floorY, cz);
        data.putBoolean("abyss_domain_transferring", false);

        // Extend defender duration too
        LivingEntity owner = findLivingIn(domainLevel, inst.ownerId);
        if (owner != null && domainEffect != null) {
            MobEffectInstance oCur = owner.getEffect(domainEffect);
            if (oCur != null && oCur.getDuration() < DOMAIN_DURATION_TICKS) {
                owner.addEffect(new MobEffectInstance(
                        domainEffect, DOMAIN_DURATION_TICKS,
                        Math.max(oCur.getAmplifier(), 1),
                        false, false, true));
            }
        }
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

    private static void cleanupDomainMobs(ServerLevel domainLevel, DomainInstance inst) {
        if (domainLevel == null) return;

        int r = DomainDimension.OUTER_RADIUS + 4;
        BlockPos c = inst.arenaCenter;
        AABB box = new AABB(
                c.getX() - r, c.getY() - r, c.getZ() - r,
                c.getX() + r, c.getY() + r, c.getZ() + r
        );

        for (Entity e : domainLevel.getEntities((Entity) null, box, ent -> true)) {
            if (e instanceof Player) continue;

            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (id == null || !"jujutsucraft".equals(id.getNamespace())) continue;

            String path = id.getPath();
            if (path.contains("shikigami")
                    || path.contains("divine_dog")
                    || path.contains("nue")
                    || path.contains("toad")
                    || path.contains("elephant")
                    || path.contains("rabbit")
                    || path.contains("max_elephant")
                    || path.contains("mahoraga")
                    || path.contains("piercing_ox")
                    || path.contains("round_deer")
                    || path.contains("tiger")
                    || path.contains("domain_expansion_entity")
                    || path.contains("deer")
                    || path.contains("ox")
                    || path.contains("wolf")
                    || path.contains("dog")
                    || path.contains("serpent")
                    || path.contains("snake")) {
                e.discard();
            }
        }
    }

    private static void evacuateArena(ServerLevel domainLevel, ServerLevel returnLevel,
                                      DomainInstance inst) {
        if (domainLevel == null || returnLevel == null) return;

        int r = DomainDimension.OUTER_RADIUS + 4;
        BlockPos c = inst.arenaCenter;
        AABB box = new AABB(
                c.getX() - r, c.getY() - r, c.getZ() - r,
                c.getX() + r, c.getY() + r, c.getZ() + r
        );

        List<LivingEntity> toSend = new ArrayList<>(
                domainLevel.getEntitiesOfClass(LivingEntity.class, box, e -> true)
        );

        for (LivingEntity living : toSend) {
            // Don't yank corpses mid death-anim — lets drops spawn first
            if (living.isDeadOrDying()) continue;

            Vec3 back = inst.returnPositions.get(living.getUUID());
            if (back == null) back = inst.overworldCenter;

            living.getPersistentData().putBoolean("abyss_domain_transferring", true);
            DomainTeleport.sendBack(living, returnLevel, back);
            living.getPersistentData().putBoolean("abyss_domain_transferring", false);
        }
    }

    private static void triggerJcDomainBreak(ServerLevel overworld, Vec3 center) {
        if (overworld == null || center == null) return;

        overworld.getChunkAt(BlockPos.containing(center));

        double r = DomainDimension.OUTER_RADIUS + 16;
        AABB box = new AABB(
                center.x - r, center.y - r, center.z - r,
                center.x + r, center.y + r, center.z + r
        );

        for (Entity e : overworld.getEntities((Entity) null, box, ent -> true)) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (id == null) continue;
            if (!"jujutsucraft".equals(id.getNamespace())) continue;
            if (!id.getPath().contains("domain_expansion")) continue;
            e.getPersistentData().putBoolean("Break", true);
        }

        forceRestoreBarriers(overworld, center, DomainDimension.OUTER_RADIUS + 2);
    }

    private static void forceRestoreBarriers(ServerLevel level, Vec3 center, int radius) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        int r2 = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > r2) continue;

                    BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
                    var state = level.getBlockState(pos);
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id == null) continue;
                    if (!"jujutsucraft".equals(id.getNamespace())
                            && !"jujutsucraftv".equals(id.getNamespace())) {
                        continue;
                    }

                    var be = level.getBlockEntity(pos);
                    String old = be != null ? be.getPersistentData().getString("old_block") : "";
                    if (old != null && !old.isEmpty()) {
                        try {
                            String name = old.contains("[") ? old.substring(0, old.indexOf('[')) : old;
                            ResourceLocation oldId = name.contains(":")
                                    ? new ResourceLocation(name)
                                    : new ResourceLocation("minecraft", name);
                            var block = ForgeRegistries.BLOCKS.getValue(oldId);
                            if (block != null) {
                                level.setBlock(pos, block.defaultBlockState(), 3);
                                continue;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    /** Move any item entities still in the arena to the overworld cast position */
    private static void sweepArenaItems(ServerLevel domainLevel, ServerLevel returnLevel,
                                        DomainInstance inst) {
        if (domainLevel == null || returnLevel == null) return;

        int r = DomainDimension.OUTER_RADIUS + 4;
        BlockPos c = inst.arenaCenter;
        AABB box = new AABB(
                c.getX() - r, c.getY() - r, c.getZ() - r,
                c.getX() + r, c.getY() + r, c.getZ() + r
        );

        Vec3 dropAt = inst.overworldCenter;
        List<ItemEntity> items = new ArrayList<>(
                domainLevel.getEntitiesOfClass(ItemEntity.class, box, e -> true)
        );

        for (ItemEntity item : items) {
            ItemStack stack = item.getItem().copy();
            if (stack.isEmpty()) {
                item.discard();
                continue;
            }
            ItemEntity over = new ItemEntity(
                    returnLevel, dropAt.x, dropAt.y + 0.5, dropAt.z, stack);
            over.setDefaultPickUpDelay();
            returnLevel.addFreshEntity(over);
            item.discard();
        }
    }

    private static void tryStartClashInDimension(LivingEntity challenger) {
        if (!(challenger.level() instanceof ServerLevel domainLevel)) return;

        DomainInstance inst = findInstanceContaining(challenger.position());
        if (inst == null) {
            // Not inside a known arena — ignore / fail soft
            challenger.getPersistentData().putBoolean("abyss_domain_handled", false);
            challenger.getPersistentData().putBoolean("abyss_domain_pending", false);
            return;
        }

        // Already the owner of this instance
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

        // Point JC at this arena center so clash/sure-hit logic uses the right sphere
        double cx = inst.arenaCenter.getX() + 0.5;
        double cy = inst.arenaCenter.getY();
        double cz = inst.arenaCenter.getZ() + 0.5;
        data.putDouble("x_pos_doma", cx);
        data.putDouble("y_pos_doma", cy);
        data.putDouble("z_pos_doma", cz);

        // Stay in same arena; record for evacuate/return
        inst.returnPositions.putIfAbsent(
                challenger.getUUID(),
                inst.returnPositions.getOrDefault(challenger.getUUID(), challenger.position())
        );

        challenger.getPersistentData().putBoolean("abyss_domain_handled", true);
        challenger.getPersistentData().putBoolean("abyss_domain_pending", false);
        challenger.getPersistentData().putBoolean("abyss_domain_transferring", false);
        challenger.getPersistentData().putBoolean("abyss_domain_clashing", true);

        // Ensure domain effect lasts 2:40
        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                new ResourceLocation("jujutsucraft", "domain_expansion"));
        if (domainEffect != null) {
            MobEffectInstance cur = challenger.getEffect(domainEffect);
            int amp = cur != null ? Math.max(cur.getAmplifier(), 1) : 1;
            challenger.addEffect(new MobEffectInstance(
                    domainEffect, DOMAIN_DURATION_TICKS, amp, false, false, true));
        }

        // Also extend the defending owner's effect
        LivingEntity owner = findLivingIn(domainLevel, inst.ownerId);
        if (owner != null && domainEffect != null) {
            MobEffectInstance oCur = owner.getEffect(domainEffect);
            if (oCur != null && oCur.getDuration() < DOMAIN_DURATION_TICKS) {
                owner.addEffect(new MobEffectInstance(
                        domainEffect, DOMAIN_DURATION_TICKS,
                        Math.max(oCur.getAmplifier(), 1),
                        false, false, true));
            }
        }
    }

    private static DomainInstance findInstanceContaining(Vec3 pos) {
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

    private static LivingEntity findActiveDomainInArena(ServerLevel domainLevel,
                                                        DomainInstance inst,
                                                        UUID excluding) {
        if (domainLevel == null) return null;

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DOMAIN_EFFECT_ID);
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

    private static void transferOwnership(DomainInstance inst, LivingEntity loser,
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

        loser.getPersistentData().putBoolean("abyss_domain_handled", false);
        loser.getPersistentData().putBoolean("abyss_domain_pending", false);
        loser.getPersistentData().putBoolean("abyss_domain_clashing", false);

        if (domainLevel != null) {
            DomainArenaBuilder.placeFloor(domainLevel, inst.arenaCenter, inst.skillDomain);
        }
    }

    /** Used when owner entity is already gone after death delay */
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

        over.getChunkAt(BlockPos.containing(overCenter));

        if (domainLevel != null) {
            evacuateArena(domainLevel, over, inst);
            sweepArenaItems(domainLevel, over, inst);
            cleanupDomainMobs(domainLevel, inst);
            DomainInstanceManager.clearArena(domainLevel, inst);
            unloadArena(domainLevel, inst);
        }

        triggerJcDomainBreak(over, overCenter);
        server.tell(new TickTask(server.getTickCount() + 20,
                () -> triggerJcDomainBreak(over, overCenter)));
        server.tell(new TickTask(server.getTickCount() + 40,
                () -> triggerJcDomainBreak(over, overCenter)));

        DomainInstanceManager.release(ownerId);
    }

    public static void end(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (entity.getPersistentData().getBoolean("abyss_domain_transferring")) return;

        // Challenger lost — do not tear down the arena
        DomainInstance asOwner = DomainInstanceManager.getByOwner(entity.getUUID());
        if (asOwner == null) {
            entity.getPersistentData().putBoolean("abyss_domain_handled", false);
            entity.getPersistentData().putBoolean("abyss_domain_pending", false);
            entity.getPersistentData().putBoolean("abyss_domain_clashing", false);
            return;
        }

        MinecraftServer server = entity.level().getServer();
        if (server == null) return;

        ServerLevel domainLevel = server.getLevel(DomainDimension.ID);
        ServerLevel returnLevel = server.getLevel(asOwner.returnDim);
        if (returnLevel == null) returnLevel = server.overworld();

        // Owner lost clash — try to hand arena to another active domain
        LivingEntity winner = findActiveDomainInArena(domainLevel, asOwner, entity.getUUID());
        if (winner != null) {
            transferOwnership(asOwner, entity, winner, domainLevel);
            return;
        }

        // Nobody else holding a domain — fully end the arena
        entity.getPersistentData().putDouble("x_pos_doma", asOwner.overworldCenter.x);
        entity.getPersistentData().putDouble("y_pos_doma", asOwner.overworldCenter.y);
        entity.getPersistentData().putDouble("z_pos_doma", asOwner.overworldCenter.z);

        returnLevel.getChunkAt(BlockPos.containing(asOwner.overworldCenter));

        if (domainLevel != null) {
            evacuateArena(domainLevel, returnLevel, asOwner);
            sweepArenaItems(domainLevel, returnLevel, asOwner);
            cleanupDomainMobs(domainLevel, asOwner);
            DomainInstanceManager.clearArena(domainLevel, asOwner);
            unloadArena(domainLevel, asOwner);
        }

        triggerJcDomainBreak(returnLevel, asOwner.overworldCenter);

        final Vec3 overCenter = asOwner.overworldCenter;
        final ServerLevel over = returnLevel;
        server.tell(new TickTask(server.getTickCount() + 20,
                () -> triggerJcDomainBreak(over, overCenter)));
        server.tell(new TickTask(server.getTickCount() + 40,
                () -> triggerJcDomainBreak(over, overCenter)));

        DomainInstanceManager.release(entity.getUUID());

        entity.getPersistentData().putBoolean("abyss_domain_handled", false);
        entity.getPersistentData().putBoolean("abyss_domain_pending", false);
        entity.getPersistentData().putBoolean("abyss_domain_transferring", false);
        entity.getPersistentData().putBoolean("abyss_domain_clashing", false);
    }
}