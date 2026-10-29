
package com.ghosts.abyss.client.sixeyes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

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

        if (mc.player == null || mc.level == null || rendering) {
            return;
        }

        RenderTarget mask =
                SixEyesThermalTargets.getEntityMask();

        if (mask == null) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        rendering = true;

        try {
            mask.bindWrite(false);

            MultiBufferSource.BufferSource buffers =
                    MultiBufferSource.immediate(
                            new BufferBuilder(256)
                    );

            for (SixEyesMaskCapture.CapturedEntity captured
                    : SixEyesMaskCapture.getEntities()) {

                LivingEntity entity = captured.entity();

                if (!entity.isAlive() || entity.isRemoved()) {
                    continue;
                }

                double x = Mth.lerp(
                        partialTick, entity.xOld, entity.getX()
                ) - cameraPos.x;

                double y = Mth.lerp(
                        partialTick, entity.yOld, entity.getY()
                ) - cameraPos.y;

                double z = Mth.lerp(
                        partialTick, entity.zOld, entity.getZ()
                ) - cameraPos.z;

                poseStack.pushPose();

                try {
                    mc.getEntityRenderDispatcher().render(
                            entity,
                            x,
                            y,
                            z,
                            Mth.lerp(
                                    partialTick,
                                    entity.yRotO,
                                    entity.getYRot()
                            ),
                            partialTick,
                            poseStack,
                            buffers,
                            15728880
                    );
                } finally {
                    poseStack.popPose();
                }
            }

            buffers.endBatch();

        } finally {
            mc.getMainRenderTarget().bindWrite(false);
            rendering = false;
        }
    }
}
