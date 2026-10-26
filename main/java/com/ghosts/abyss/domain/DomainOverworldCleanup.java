package com.ghosts.abyss.domain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class DomainOverworldCleanup {

    private DomainOverworldCleanup() {}

    public static void triggerJcDomainBreak(ServerLevel overworld, Vec3 center) {
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

    public static void forceRestoreBarriers(ServerLevel level, Vec3 center, int radius) {
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
}