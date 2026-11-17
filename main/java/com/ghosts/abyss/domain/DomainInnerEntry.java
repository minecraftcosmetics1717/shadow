
package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomainInnerEntry {

    // Crossing inward through this radius triggers entry.
    private static final double ENTRY_RADIUS =
            DomainOverworldShell.INNER_SPHERE_RADIUS + 0.5;

    // Arrival distance from the domain arena center.
    // Leaves more than 20 blocks of space before the wall.
    private static final double ARENA_ENTRY_RADIUS = 40.0;

    private DomainInnerEntry() {}

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (!entity.isAlive() || entity.isPassenger()) {
            return;
        }

        if (entity instanceof ServerPlayer player
                && player.isSpectator()) {
            return;
        }

        // Already in a domain dimension.
        if (level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        // Ignore entities in an existing dimension transfer.
        if (entity.getPersistentData().getBoolean(
                "abyss_domain_transferring")) {
            return;
        }

        ServerLevel domainLevel =
                level.getServer().getLevel(DomainDimension.ID);

        if (domainLevel == null) {
            return;
        }

        double entityHalfHeight =
                entity.getBbHeight() * 0.5;

        Vec3 current = entity.position().add(
                0.0,
                entityHalfHeight,
                0.0
        );

        Vec3 previous = new Vec3(
                entity.xo,
                entity.yo + entityHalfHeight,
                entity.zo
        );

        double entryRadiusSq =
                ENTRY_RADIUS * ENTRY_RADIUS;

        for (int slot = 0;
             slot < DomainDimension.MAX_SLOTS;
             slot++) {

            DomainInstance inst =
                    DomainInstanceManager.getBySlot(slot);

            if (inst == null) continue;

            // This sphere must belong to this dimension.
            if (!inst.returnDim.equals(level.dimension())) {
                continue;
            }

            // The barrier's block placement uses
            // integer block coordinates as its center.
            BlockPos centerBlock =
                    BlockPos.containing(inst.overworldCenter);

            Vec3 sphereCenter = new Vec3(
                    centerBlock.getX() + 0.5,
                    centerBlock.getY() + 0.5,
                    centerBlock.getZ() + 0.5
            );

            double currentDistanceSq =
                    current.distanceToSqr(sphereCenter);

            if (currentDistanceSq > entryRadiusSq) {
                continue;
            }

            double previousDistanceSq =
                    previous.distanceToSqr(sphereCenter);

            // Only trigger when crossing inward,
            // not when moving around inside the sphere.
            if (previousDistanceSq <= entryRadiusSq) {
                continue;
            }

            // Prevent entry during sphere destruction
            // or before the sphere has been created.
            if (!DomainOverworldShell.isActive(
                    level,
                    inst.overworldCenter,
                    AbyssDomainCast.OVERWORLD_RADIUS
            )) {
                continue;
            }

            // Confirm that the owner has arrived in
            // the destination domain dimension.
            LivingEntity owner =
                    DomainArenaService.findLivingIn(
                            domainLevel,
                            inst.ownerId
                    );

            if (owner == null) {
                continue;
            }

            enterDomain(
                    entity,
                    domainLevel,
                    inst,
                    sphereCenter
            );

            return;
        }
    }

    private static void enterDomain(
            LivingEntity entity,
            ServerLevel domainLevel,
            DomainInstance inst,
            Vec3 sphereCenter
    ) {
        Vec3 original = entity.position();

        // Save the overworld position as a fallback
        // for when the domain ends.
        inst.returnPositions.putIfAbsent(
                entity.getUUID(),
                original
        );

        double dx = original.x - sphereCenter.x;
        double dz = original.z - sphereCenter.z;

        double horizontalDistance =
                Math.sqrt(dx * dx + dz * dz);

        // Preserve the direction from which the
        // entity entered the sphere.
        if (horizontalDistance < 0.001) {
            dx = 0;
            dz = 1;
            horizontalDistance = 1;
        }

        dx /= horizontalDistance;
        dz /= horizontalDistance;

        double destinationX =
                inst.arenaCenter.getX() + 0.5
                        + dx * ARENA_ENTRY_RADIUS;

        double destinationY =
                inst.arenaCenter.getY() + 1.1;

        double destinationZ =
                inst.arenaCenter.getZ() + 0.5
                        + dz * ARENA_ENTRY_RADIUS;

        LivingEntity arrived = DomainTeleport.teleport(
                entity,
                domainLevel,
                destinationX,
                destinationY,
                destinationZ
        );

        if (arrived == null
                || arrived.level() != domainLevel) {

            Abyss.LOGGER.warn(
                    "[Abyss Domain Entry] Failed to enter " +
                            "domain slot={} entity={}",
                    inst.slot,
                    entity.getName().getString()
            );

            return;
        }

        Abyss.LOGGER.info(
                "[Abyss Domain Entry] {} entered slot={} at {}",
                arrived.getName().getString(),
                inst.slot,
                arrived.position()
        );
    }
}
