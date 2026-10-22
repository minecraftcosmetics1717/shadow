package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DomainInstance {
    public final int slot;
    public final UUID ownerId;
    public final double skillDomain;
    public final BlockPos arenaCenter;          // in domain dim
    public final ResourceKey<Level> returnDim;  // usually overworld
    public final Vec3 overworldCenter;          // JC sphere center
    public final Map<UUID, Vec3> returnPositions = new HashMap<>();

    public DomainInstance(int slot, UUID ownerId, double skillDomain,
                          BlockPos arenaCenter, ResourceKey<Level> returnDim,
                          Vec3 overworldCenter) {
        this.slot = slot;
        this.ownerId = ownerId;
        this.skillDomain = skillDomain;
        this.arenaCenter = arenaCenter;
        this.returnDim = returnDim;
        this.overworldCenter = overworldCenter;
    }
}