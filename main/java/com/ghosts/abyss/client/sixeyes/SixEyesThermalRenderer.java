package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesThermalRenderer {

    private static final ResourceLocation SHADER =
            new ResourceLocation("abyss", "shaders/post/six_eyes.json");

    private static boolean ourShaderActive = false;
    private static boolean loadFailed = false;

    private SixEyesThermalRenderer() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        boolean shouldEnable =
                mc.player != null
                        && mc.level != null
                        && SixEyesVisionHandler.shouldRender(mc.player);

        if (!shouldEnable) {
            if (ourShaderActive) {
                mc.gameRenderer.shutdownEffect();
                ourShaderActive = false;
            }
            loadFailed = false;
            return;
        }

        if (ourShaderActive) {
            // A resource reload or another renderer might remove our effect.
            if (mc.gameRenderer.currentEffect() == null) {
                ourShaderActive = false;
            } else {
                return;
            }
        }

        if (loadFailed) return;

        // Do not replace an existing post-processing effect.
        if (mc.gameRenderer.currentEffect() != null) return;

        try {
            mc.gameRenderer.loadEffect(SHADER);
            ourShaderActive = mc.gameRenderer.currentEffect() != null;
            loadFailed = !ourShaderActive;
        } catch (Exception ex) {
            loadFailed = true;
            System.err.println("[Abyss] Failed to load Six Eyes shader:");
            ex.printStackTrace();
        }
    }
}