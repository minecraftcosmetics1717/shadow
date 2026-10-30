
package com.ghosts.abyss.client.sixeyes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class SixEyesThermalLayer<
        T extends LivingEntity,
        M extends EntityModel<T>
        > extends RenderLayer<T, M> {

    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/misc/white.png"
            );

    private static final int FULL_BRIGHT = 15728880;

    private static final float MODEL_SCALE = 1.03F;

    public SixEyesThermalLayer(
            LivingEntityRenderer<T, M> parent
    ) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!SixEyesGlowRules.shouldGlow(entity)) {
            return;
        }

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.01D, 0.0D);
        poseStack.scale(
                MODEL_SCALE,
                MODEL_SCALE,
                MODEL_SCALE
        );

        M model = getParentModel();

        model.prepareMobModel(
                entity,
                limbSwing,
                limbSwingAmount,
                partialTicks
        );

        model.setupAnim(
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch
        );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.eyes(WHITE_TEXTURE)
                );

        // Diagnostic cyan, matching Vision Goggles.
        model.renderToBuffer(
                poseStack,
                consumer,
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0.0F,
                1.0F,
                1.0F,
                1.0F
        );

        poseStack.popPose();
    }
}
