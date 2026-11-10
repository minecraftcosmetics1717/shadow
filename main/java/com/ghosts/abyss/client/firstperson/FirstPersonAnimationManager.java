
package com.ghosts.abyss.client.firstperson;

import com.ghosts.abyss.Abyss;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Abyss.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FirstPersonAnimationManager {

    private static FirstPersonAnimationType current =
            FirstPersonAnimationType.NONE;

    private static float progress = 0.0F;
    private static float previousProgress = 0.0F;

    private FirstPersonAnimationManager() {}


    public static void setAnimation(
            FirstPersonAnimationType animation
    ) {
        if (animation == FirstPersonAnimationType.NONE) {
            current = FirstPersonAnimationType.NONE;
            progress = 0.0F;
            previousProgress = 0.0F;
            return;
        }

        // Start a new animation from the beginning.
        if (current != animation) {
            progress = 0.0F;
            previousProgress = 0.0F;
        }

        current = animation;
    }


    public static FirstPersonAnimationType getAnimation() {
        return current;
    }

    public static float getProgress(float partialTick) {
        return previousProgress
                + (progress - previousProgress) * partialTick;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            current = FirstPersonAnimationType.NONE;
            progress = 0.0F;
            previousProgress = 0.0F;
            return;
        }

        previousProgress = progress;

        if (current != FirstPersonAnimationType.NONE) {
            progress = Math.min(1.0F, progress + (1.0F / 6.0F));
        } else {
            progress = 0.0F;
            previousProgress = 0.0F;
        }
    }
}
