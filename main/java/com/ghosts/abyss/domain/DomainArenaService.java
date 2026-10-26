package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DomainArenaService {

    private static final TicketType<ChunkPos> DOMAIN_TICKET =
            TicketType.create("abyss:domain", Comparator.comparingLong(ChunkPos::toLong), 300);
    private static final int TICKET_RADIUS = 5;

    private DomainArenaService() {}

    public static void forceLoadArena(ServerLevel level, DomainInstance inst) {
        ChunkPos cp = new ChunkPos(inst.arenaCenter);
        level.getChunkSource().addRegionTicket(DOMAIN_TICKET, cp, TICKET_RADIUS, cp);
        level.getChunk(cp.x, cp.z);
    }

    public static void unloadArena(ServerLevel level, DomainInstance inst) {
        ChunkPos cp = new ChunkPos(inst.arenaCenter);
        level.getChunkSource().removeRegionTicket(DOMAIN_TICKET, cp, TICKET_RADIUS, cp);
    }

    public static LivingEntity findLivingIn(ServerLevel level, UUID id) {
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

    public static void evacuateArena(ServerLevel domainLevel, ServerLevel returnLevel,
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
            if (living.isDeadOrDying()) continue;

            Vec3 back = inst.returnPositions.get(living.getUUID());
            if (back == null) back = inst.overworldCenter;

            living.getPersistentData().putBoolean("abyss_domain_transferring", true);
            DomainTeleport.sendBack(living, returnLevel, back);
            living.getPersistentData().putBoolean("abyss_domain_transferring", false);

            // Full arena end: no one should still be "in domain" in the overworld
            stripDomainFrom(living);
        }
    }
    public static void stripDomainFrom(LivingEntity living) {
        if (living == null || living.isDeadOrDying()) return;

        MobEffect domainEffect = ForgeRegistries.MOB_EFFECTS.getValue(DomainEvents.DOMAIN_EFFECT_ID);
        if (domainEffect != null && living.hasEffect(domainEffect)) {
            living.getPersistentData().putBoolean("abyss_domain_transferring", true);
            living.removeEffect(domainEffect);
            living.getPersistentData().putBoolean("abyss_domain_transferring", false);
        }

        var data = living.getPersistentData();
        data.putBoolean("abyss_domain_handled", false);
        data.putBoolean("abyss_domain_pending", false);
        data.putBoolean("abyss_domain_clashing", false);
        data.putBoolean("Failed", false);
        data.putBoolean("Cover", false);
        data.putBoolean("DomainDefeated", false);
        // Stops JC treating them as still domain-expanded in the overworld
        if (data.getDouble("skill_domain") != 0) {
            data.putDouble("skill_domain", 0);
        }
    }
    public static void sweepArenaItems(ServerLevel domainLevel, ServerLevel returnLevel,
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

    public static void cleanupDomainMobs(ServerLevel domainLevel, DomainInstance inst) {
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
}