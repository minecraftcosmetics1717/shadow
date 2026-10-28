
package com.ghosts.abyss.domain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DomainFishDebug {

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static final Set<UUID> SEEN = new HashSet<>();

    private DomainFishDebug() {}

    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(DomainDimension.ID)) return;

        Set<UUID> active = new HashSet<>();

        for (int slot = 0; slot < DomainDimension.MAX_SLOTS; slot++) {
            DomainInstance inst = DomainInstanceManager.getBySlot(slot);
            if (inst == null || inst.skillDomain != 8.0) continue;

            Vec3 center = Vec3.atCenterOf(inst.arenaCenter);
            double radius = DomainDimension.INNER_RADIUS;

            // Search beyond the wall to detect escaped fish.
            AABB area = new AABB(
                    center.x - 110, center.y - 110, center.z - 110,
                    center.x + 110, center.y + 110, center.z + 110
            );

            for (Entity fish : level.getEntities(
                    (Entity) null, area,
                    e -> {
                        var id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
                        return id != null
                                && "jujutsucraft".equals(id.getNamespace())
                                && id.getPath().contains("sea_serpent");
                    }
            )) {
                UUID id = fish.getUUID();
                active.add(id);

                Vec3 pos = fish.position();
                double distance = pos.distanceTo(center);
                Vec3 previous = LAST_POS.put(id, pos);

                if (SEEN.add(id)) {
                    log("FIRST_SEEN", inst, fish, pos, distance, previous);
                }

                if (previous != null) {
                    double previousDistance = previous.distanceTo(center);

                    if (previousDistance <= radius && distance > radius) {
                        log("CROSSED_WALL", inst, fish, pos, distance, previous);
                    }
                }

                // Periodic position samples while outside.
                if (distance > radius && fish.tickCount % 20 == 0) {
                    log("OUTSIDE", inst, fish, pos, distance, previous);
                }
            }
        }

        LAST_POS.keySet().retainAll(active);
        SEEN.retainAll(active);
    }

    private static void log(
            String type,
            DomainInstance inst,
            Entity fish,
            Vec3 pos,
            double distance,
            Vec3 previous
    ) {
        com.ghosts.abyss.Abyss.LOGGER.info(
                "[Abyss FishDebug] type={} slot={} owner={} " +
                        "fish={} age={} pos={} previous={} " +
                        "distance={} radius={} velocity={} " +
                        "fishOwner={}",
                type,
                inst.slot,
                inst.ownerId,
                fish.getUUID(),
                fish.tickCount,
                pos,
                previous,
                String.format(java.util.Locale.ROOT, "%.2f", distance),
                DomainDimension.INNER_RADIUS,
                fish.getDeltaMovement(),
                fish.getPersistentData().getString("OWNER_UUID")
        );
    }
}
