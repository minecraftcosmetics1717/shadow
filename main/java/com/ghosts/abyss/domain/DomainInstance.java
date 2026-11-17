package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class DomainInstance {
    public final int slot;
    public UUID ownerId;
    public boolean ownerEntryConfirmed = false;
    public double skillDomain;
    public final BlockPos arenaCenter;          // in domain dim
    public final ResourceKey<Level> returnDim;  // usually overworld
    public final Vec3 overworldCenter;          // JC sphere center
    public final Map<UUID, Vec3> returnPositions = new HashMap<>();
    public final DomainClashParticipants participants;
    public final Set<UUID> yutaDecorationsSpawned = new HashSet<>();
    // Remember every domain palette used in this arena,
// including domains whose casters have already lost.
    public final Set<Double> territorySkills = new HashSet<>();
    public DomainInstance(
            int slot,
            UUID ownerId,
            double skillDomain,
            BlockPos arenaCenter,
            ResourceKey<Level> returnDim,
            Vec3 overworldCenter
    ) {
        this.slot = slot;
        this.ownerId = ownerId;
        this.skillDomain = skillDomain;
        this.arenaCenter = arenaCenter;
        this.returnDim = returnDim;
        this.overworldCenter = overworldCenter;

        // The original caster always starts north.
        this.participants = new DomainClashParticipants(
                ownerId,
                skillDomain
        );
        // Remember the original owner's block palette.
        this.territorySkills.add(skillDomain);
    }

}