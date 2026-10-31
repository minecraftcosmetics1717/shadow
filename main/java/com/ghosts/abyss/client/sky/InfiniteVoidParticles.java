
package com.ghosts.abyss.client.sky;

import com.ghosts.abyss.domain.DomainDimension;
import net.mcreator.jujutsucraft.init.JujutsucraftModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class InfiniteVoidParticles {

    private static final Random RANDOM = new Random();

    private static final double RADIUS = 38.0;
    private static final int PARTICLES_PER_TICK = 1;

    private InfiniteVoidParticles() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.isPaused() || mc.level == null || mc.player == null) {
            return;
        }

        ClientLevel level = mc.level;

        if (!level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        // Only spawn these when the player is near Infinite Void
        // sky blocks, rather than in every domain.
        if (!hasInfiniteVoidSky(level, camera)) {
            return;
        }

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {

            double y = RANDOM.nextDouble() * 1.6 - 0.8;
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;

            double horizontal = Math.sqrt(1.0 - y * y);

            double x = camera.x
                    + Math.cos(angle) * horizontal * RADIUS;

            double py = camera.y + y * RADIUS;

            double z = camera.z
                    + Math.sin(angle) * horizontal * RADIUS;

            level.addParticle(
                    JujutsucraftModParticleTypes
                            .PARTICLE_UNLIMITED_VOID.get(),
                    x, py, z,
                    0.0, 0.0, 0.0
            );
        }
    }

    private static boolean hasInfiniteVoidSky(
            ClientLevel level,
            Vec3 camera
    ) {
        BlockPos center = BlockPos.containing(camera);

        // Cheap proximity check for Infinite Void sky blocks.
        for (int x = -48; x <= 48; x += 8) {
            for (int y = -48; y <= 48; y += 8) {
                for (int z = -48; z <= 48; z += 8) {

                    var state = level.getBlockState(
                            center.offset(x, y, z)
                    );

                    if (state.getBlock()
                            instanceof com.ghosts.abyss.block.DomainSkyBlock sky
                            && sky.getSkyType()
                            == com.ghosts.abyss.block.DomainSkyBlock
                            .SkyType.INFINITE_VOID) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
