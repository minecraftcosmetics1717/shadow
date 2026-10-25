package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DomainInstanceManager {
    private static final Map<Integer, DomainInstance> BY_SLOT = new ConcurrentHashMap<>();
    private static final Map<UUID, DomainInstance> BY_OWNER = new ConcurrentHashMap<>();

    public static DomainInstance claim(UUID ownerId, double skillDomain,
                                       ResourceKey<Level> returnDim, Vec3 overworldCenter) {
        if (BY_OWNER.containsKey(ownerId)) {
            return BY_OWNER.get(ownerId);
        }
        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            if (!BY_SLOT.containsKey(slot)) {
                BlockPos center = new BlockPos(
                        slot * DomainDimension.SLOT_SPACING,
                        DomainDimension.CENTER_Y,
                        0
                );
                DomainInstance inst = new DomainInstance(
                        slot, ownerId, skillDomain, center, returnDim, overworldCenter
                );
                BY_SLOT.put(slot, inst);
                BY_OWNER.put(ownerId, inst);
                return inst;
            }
        }
        return null;
    }

    public static DomainInstance getByOwner(UUID ownerId) {
        return BY_OWNER.get(ownerId);
    }

    public static DomainInstance getBySlot(int slot) {
        return BY_SLOT.get(slot);
    }
    public static void reassignOwner(UUID oldOwner, UUID newOwner) {
        DomainInstance inst = BY_OWNER.remove(oldOwner);
        if (inst == null) return;
        inst.ownerId = newOwner;
        BY_OWNER.put(newOwner, inst);
    }
    public static void release(UUID ownerId) {
        DomainInstance inst = BY_OWNER.remove(ownerId);
        if (inst != null) {
            BY_SLOT.remove(inst.slot);
        }
    }

    public static void clearArena(ServerLevel domainLevel, DomainInstance inst) {
        DomainDimBlockGuard.open();
        try {
            int r = DomainDimension.OUTER_RADIUS + 2;
            BlockPos c = inst.arenaCenter;
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        domainLevel.setBlock(c.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        } finally {
            DomainDimBlockGuard.close();
        }
    }
}