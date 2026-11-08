
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

    // 35% of the player's normal sensitivity.
    private static final double FIRING_MULTIPLIER = 0.35D;

    private static Double originalSensitivity = null;

    private PiercingBloodSensitivity() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        boolean firing = false;

        if (mc.level != null && mc.player != null) {

            for (Entity entity : mc.level.entitiesForRendering()) {

                if (entity instanceof PiercingBloodEntity beam
                        && beam.getOwnerPlayer() == mc.player) {

                    firing = true;
                    break;
                }
            }
        }

        if (firing) {

            if (originalSensitivity == null) {

                originalSensitivity =
                        mc.options.sensitivity().get();

                mc.options.sensitivity().set(
                        originalSensitivity * FIRING_MULTIPLIER
                );
            }

        } else if (originalSensitivity != null) {

            mc.options.sensitivity().set(originalSensitivity);

            originalSensitivity = null;
        }
    }
}
