
package com.ghosts.abyss.client.sixeyes;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SixEyesMaskFrameHandler {

    private SixEyesMaskFrameHandler() {}

    @SubscribeEvent
    public static void onRenderLevel(
            RenderLevelStageEvent event
    ) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null
                || mc.level == null
                || !SixEyesVisionHandler.shouldRender(mc.player)) {
            return;
        }

        SixEyesMaskCapture.beginFrame();

        try {
            SixEyesThermalTargets.ensureCreated();
            SixEyesThermalTargets.clear();
        } finally {
            mc.getMainRenderTarget().bindWrite(false);
        }
    }
}
