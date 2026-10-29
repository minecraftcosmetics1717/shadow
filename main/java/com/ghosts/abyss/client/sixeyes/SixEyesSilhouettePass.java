
package com.ghosts.abyss.client.sixeyes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;

public final class SixEyesSilhouettePass {

    private static boolean rendering = false;

    private SixEyesSilhouettePass() {}

    public static boolean isRendering() {
        return rendering;
    }

    public static void render(
            PoseStack poseStack,
            float partialTick
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        RenderTarget mask =
                SixEyesThermalTargets.getEntityMask();

        if (mask == null || rendering) {
            return;
        }

        rendering = true;

        try {
            mask.bindWrite(false);

            MultiBufferSource.BufferSource buffers =
                    MultiBufferSource.immediate(
                            new com.mojang.blaze3d.vertex.BufferBuilder(256)
                    );

            for (SixEyesMaskCapture.CapturedEntity captured
                    : SixEyesMaskCapture.getEntities()) {

                LivingEntity entity = captured.entity();

                if (!entity.isAlive() || entity.isRemoved()) {
                    continue;
                }

                // Geometry rendering will be connected here.
                // We must supply correct camera-relative transforms
                // and isolate the models' RenderTypes before drawing.
            }

            buffers.endBatch();

        } finally {
            mc.getMainRenderTarget().bindWrite(false);
            rendering = false;
        }
    }
}
