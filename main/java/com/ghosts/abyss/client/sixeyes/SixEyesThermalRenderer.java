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

    private static boolean ourShaderLoaded = false;
    private static boolean loadFailed = false;

    private SixEyesThermalRenderer() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (true) {
            Minecraft mc = Minecraft.getInstance();

            if (ourShaderLoaded) {
                mc.gameRenderer.shutdownEffect();
                ourShaderLoaded = false;
            }

            return;
        }
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        boolean shouldEnable =
                mc.player != null
                        && mc.level != null
                        && SixEyesVisionHandler.shouldRender(mc.player);

        if (!shouldEnable) {
            if (ourShaderLoaded) {
                mc.gameRenderer.shutdownEffect();
                ourShaderLoaded = false;
            }

            loadFailed = false;
            return;
        }

        if (ourShaderLoaded) {
            if (mc.gameRenderer.currentEffect() != null) {
                return;
            }
            ourShaderLoaded = false;
        }

        if (loadFailed) return;

        if (mc.gameRenderer.currentEffect() != null) return;

        try {
            mc.gameRenderer.loadEffect(SHADER);

            if (mc.gameRenderer.currentEffect() == null) {
                loadFailed = true;
                System.err.println(
                        "[Abyss] Six Eyes post-processing effect failed to load."
                );
                return;
            }

            // Enable the loaded post-processing effect.
// loadEffect() already activates the post effect.
            ourShaderLoaded = true;

            System.out.println(
                    "[Abyss] Six Eyes post-processing effect loaded."
            );

        } catch (Exception ex) {
            loadFailed = true;
            System.err.println("[Abyss] Failed to load Six Eyes shader:");
            ex.printStackTrace();
        }
    }
}