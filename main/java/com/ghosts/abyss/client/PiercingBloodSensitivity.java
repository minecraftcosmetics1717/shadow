
package com.ghosts.abyss.client;

import com.ghosts.abyss.entity.PiercingBloodEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class PiercingBloodSensitivity {

    // Keep Piercing Blood's existing slowdown.
    private static final double PIERCING_MULTIPLIER = 0.35D;

    // Blue is slowed less than Piercing Blood.
    private static final double BLUE_MULTIPLIER = 0.65D;

    private static Double originalSensitivity = null;
    private static double appliedMultiplier = 1.0D;

    private PiercingBloodSensitivity() {}

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            BlueControlClientState.setControlling(false);
        }

        boolean piercingFiring = false;

        if (mc.level != null && mc.player != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {

                if (entity instanceof PiercingBloodEntity beam
                        && beam.getOwnerPlayer() == mc.player) {

                    piercingFiring = true;
                    break;
                }
            }
        }

        boolean blueControlling =
                BlueControlClientState.isControlling();

        double wantedMultiplier;

        if (piercingFiring) {
            wantedMultiplier = PIERCING_MULTIPLIER;
        } else if (blueControlling) {
            wantedMultiplier = BLUE_MULTIPLIER;
        } else {
            wantedMultiplier = 1.0D;
        }

        if (wantedMultiplier == 1.0D) {

            if (originalSensitivity != null) {
                mc.options.sensitivity().set(
                        originalSensitivity
                );

                originalSensitivity = null;
            }

            appliedMultiplier = 1.0D;
            return;
        }

        if (originalSensitivity == null) {
            originalSensitivity =
                    mc.options.sensitivity().get();
        }

        if (appliedMultiplier != wantedMultiplier) {
            mc.options.sensitivity().set(
                    originalSensitivity * wantedMultiplier
            );

            appliedMultiplier = wantedMultiplier;
        }
    }
}
