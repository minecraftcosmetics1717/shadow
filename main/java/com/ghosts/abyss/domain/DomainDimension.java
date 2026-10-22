package com.ghosts.abyss.domain;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DomainDimension {
    public static final ResourceKey<Level> ID = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation("abyss", "domain")
    );

    public static final int OUTER_RADIUS = 64;
    public static final int INNER_RADIUS = 63;
    public static final int SLOT_SPACING = 1000;
    public static final int MAX_SLOTS = 16;
    public static final int CENTER_Y = 64;
}