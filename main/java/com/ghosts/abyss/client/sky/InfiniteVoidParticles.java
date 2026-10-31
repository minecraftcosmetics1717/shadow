
package com.ghosts.abyss.client.sky;

import com.ghosts.abyss.domain.DomainDimension;
import com.ghosts.abyss.domain.DomainInstance;
import com.ghosts.abyss.domain.DomainInstanceManager;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class InfiniteVoidParticles {

    private static final ResourceLocation PARTICLE_ID =
            new ResourceLocation(
                    "jujutsucraft",
                    "particle_unlimited_void"
            );

    // Approximately 5 spawning events per second.
    private static final int SPAWN_INTERVAL = 4;

    // Number of particles per event.
    private static final int PARTICLES_PER_EVENT = 1;

    private static final double MIN_DISTANCE = 8.0;
    private static final double MAX_DISTANCE = 24.0;

    private InfiniteVoidParticles() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Only run inside Abyss's domain dimension.
        if (!level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        if (player.tickCount % SPAWN_INTERVAL != 0) {
            return;
        }

        // Find the arena slot containing this player.
        int slot = (int) Math.round(
                player.getX() / DomainDimension.SLOT_SPACING
        );

        if (slot < 0 || slot >= DomainDimension.MAX_SLOTS) {
            return;
        }

        DomainInstance domain =
                DomainInstanceManager.getBySlot(slot);

        if (domain == null) return;

        // Gojo's Infinite Void has skillDomain ID 2.
        if ((int) Math.round(domain.skillDomain) != 2) {
            return;
        }

        // Make sure the player is actually inside the arena.
        if (player.position().distanceToSqr(
                domain.arenaCenter.getCenter()
        ) > DomainDimension.INNER_RADIUS
                * DomainDimension.INNER_RADIUS) {
            return;
        }

        ParticleType<?> registered =
                ForgeRegistries.PARTICLE_TYPES.getValue(PARTICLE_ID);

        // This implementation expects a simple particle type.
        if (!(registered instanceof SimpleParticleType particle)) {
            return;
        }

        RandomSource random = level.random;

        for (int i = 0; i < PARTICLES_PER_EVENT; i++) {

            double angle = random.nextDouble() * Math.PI * 2.0;
            double elevation = random.nextDouble() * 2.0 - 1.0;

            double horizontal = Math.sqrt(
                    1.0 - elevation * elevation
            );

            double distance = MIN_DISTANCE
                    + random.nextDouble()
                    * (MAX_DISTANCE - MIN_DISTANCE);

            double x = player.getX()
                    + Math.cos(angle) * horizontal * distance;

            double y = player.getEyeY()
                    + elevation * distance;

            double z = player.getZ()
                    + Math.sin(angle) * horizontal * distance;

            // Keep the particle inside the arena.
            double dx = x - domain.arenaCenter.getX();
            double dy = y - domain.arenaCenter.getY();
            double dz = z - domain.arenaCenter.getZ();

            double maxRadius = DomainDimension.INNER_RADIUS - 3.0;

            if (dx * dx + dy * dy + dz * dz
                    > maxRadius * maxRadius) {
                continue;
            }

            // Spawn one JujutsuCraft particle.
            level.sendParticles(
                    particle,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }
}
