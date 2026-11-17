
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

    // Radius of the intangible overworld sphere.
    private static final double ENTRY_RADIUS =
            DomainOverworldShell.INNER_SPHERE_RADIUS + 0.5;

    // How far from the arena center an entering
    // entity will appear.
    private static final double ARENA_ENTRY_RADIUS = 40.0;

    private DomainInnerEntry() {}

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity entity = event.getEntity();

        // Only execute on the server.
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

        // Already inside the domain dimension.
        if (level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        // Don't interfere with another ongoing transfer.
        if (entity.getPersistentData().getBoolean(
                "abyss_domain_transferring")) {
            return;
        }

        ServerLevel domainLevel =
                level.getServer().getLevel(DomainDimension.ID);

        if (domainLevel == null) {
            return;
        }

        // Use the middle of the entity's hitbox.
        Vec3 current = entity.position().add(
                0.0,
                entity.getBbHeight() * 0.5,
                0.0
        );

        double radiusSq = ENTRY_RADIUS * ENTRY_RADIUS;

        // Check all existing domain sessions.
        for (int slot = 0;
             slot < DomainDimension.MAX_SLOTS;
             slot++) {

            DomainInstance inst =
                    DomainInstanceManager.getBySlot(slot);

            if (inst == null) {
                continue;
            }

            // Only match spheres in this dimension.
            if (!inst.returnDim.equals(level.dimension())) {
                continue;
            }

            // Match the block-aligned sphere created
            // by DomainOverworldShell.
            BlockPos blockCenter =
                    BlockPos.containing(inst.overworldCenter);

            Vec3 sphereCenter = new Vec3(
                    blockCenter.getX() + 0.5,
                    blockCenter.getY() + 0.5,
                    blockCenter.getZ() + 0.5
            );

            // Check whether the entity is inside.
            double distanceSq =
                    current.distanceToSqr(sphereCenter);

            if (distanceSq > radiusSq) {
                continue;
            }

            // Only enter if the sphere still exists.
            if (!DomainOverworldShell.isActive(
                    level,
                    inst.overworldCenter,
                    AbyssDomainCast.OVERWORLD_RADIUS
            )) {
                continue;
            }

            // The entity is inside an active
            // overworld domain. Transfer it.
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

        // Remember its original overworld position.
        inst.returnPositions.putIfAbsent(
                entity.getUUID(),
                original
        );

        double dx = original.x - sphereCenter.x;
        double dz = original.z - sphereCenter.z;

        double horizontalDistance =
                Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance < 0.001) {
            dx = 0.0;
            dz = 1.0;
            horizontalDistance = 1.0;
        }

        // Normalize the entry direction.
        dx /= horizontalDistance;
        dz /= horizontalDistance;

        // Preserve the side from which the entity
        // entered the overworld sphere.
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
                    "[Abyss Domain Entry] Teleport failed: " +
                            "entity={} slot={}",
                    entity.getName().getString(),
                    inst.slot
            );

            return;
        }

        Abyss.LOGGER.info(
                "[Abyss Domain Entry] {} entered domain " +
                        "slot={} position={}",
                arrived.getName().getString(),
                inst.slot,
                arrived.position()
        );
    }
}
