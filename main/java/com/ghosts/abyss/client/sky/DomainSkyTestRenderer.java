
package com.ghosts.abyss.client.sky;

import com.ghosts.abyss.domain.DomainDimension;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

public final class DomainSkyTestRenderer {

    private static final int HORIZONTAL_SEGMENTS = 32;
    private static final int VERTICAL_SEGMENTS = 16;
    private static final float RADIUS = 48.0F;

    private static VertexBuffer skyBuffer;

    private DomainSkyTestRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) return;

        if (!mc.level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        render(event);
    }

    private static void render(RenderLevelStageEvent event) {
        ensureBuffer();

        ShaderInstance shader = GameRenderer.getPositionColorShader();

        if (shader == null || skyBuffer == null) return;

        // Camera-relative geometry:
        // no translation is needed.
        Matrix4f poseMatrix = new Matrix4f(
                event.getPoseStack().last().pose()
        );

        Matrix4f projectionMatrix = new Matrix4f(
                event.getProjectionMatrix()
        );

        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        RenderSystem.disableCull();
        RenderSystem.disableBlend();

        // Test sky behaves as a background.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        skyBuffer.bind();
        skyBuffer.drawWithShader(
                poseMatrix,
                projectionMatrix,
                shader
        );
        VertexBuffer.unbind();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void ensureBuffer() {
        if (skyBuffer != null) return;

        RenderSystem.assertOnRenderThread();

        BufferBuilder builder =
                Tesselator.getInstance().getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (int vertical = 0;
             vertical < VERTICAL_SEGMENTS;
             vertical++) {

            float v0 =
                    (float) vertical / VERTICAL_SEGMENTS;

            float v1 =
                    (float) (vertical + 1) / VERTICAL_SEGMENTS;

            double theta0 = Math.PI * v0;
            double theta1 = Math.PI * v1;

            for (int horizontal = 0;
                 horizontal < HORIZONTAL_SEGMENTS;
                 horizontal++) {

                float u0 =
                        (float) horizontal / HORIZONTAL_SEGMENTS;

                float u1 =
                        (float) (horizontal + 1)
                                / HORIZONTAL_SEGMENTS;

                double phi0 = Math.PI * 2.0 * u0;
                double phi1 = Math.PI * 2.0 * u1;

                addVertex(builder, theta0, phi0);
                addVertex(builder, theta1, phi0);
                addVertex(builder, theta1, phi1);
                addVertex(builder, theta0, phi1);
            }
        }

        skyBuffer = new VertexBuffer(
                VertexBuffer.Usage.STATIC
        );

        skyBuffer.bind();
        skyBuffer.upload(builder.end());
        VertexBuffer.unbind();
    }

    private static void addVertex(
            BufferBuilder builder,
            double theta,
            double phi
    ) {
        float horizontal =
                (float) Math.sin(theta);

        float x =
                (float) Math.sin(phi)
                        * horizontal * RADIUS;

        float y =
                (float) Math.cos(theta) * RADIUS;

        float z =
                (float) Math.cos(phi)
                        * horizontal * RADIUS;

        // Top = blue, bottom = pale blue.
        float blend = (1F - y / RADIUS) * 0.5F;

        float red =
                0.32F + (0.75F - 0.32F) * blend;

        float green =
                0.58F + (0.88F - 0.58F) * blend;

        float blue = 1F;

        builder.vertex(x, y, z)
                .color(red, green, blue, 1F)
                .endVertex();
    }
}
