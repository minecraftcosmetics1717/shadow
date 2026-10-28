
package com.ghosts.abyss.client.sky;

import com.ghosts.abyss.block.DomainSkyBlock;
import com.ghosts.abyss.block.DomainSkyBlock.SkyType;
import com.ghosts.abyss.domain.DomainDimension;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomainSkyMaskRenderer {

    // Small test radius for now. This is NOT the
    // final domain-wide cache.
    private static final int TEST_RADIUS = 16;

    private static final EnumMap<SkyType, VertexBuffer> MASKS =
            new EnumMap<>(SkyType.class);

    private static final EnumMap<SkyType, VertexBuffer> SKIES =
            new EnumMap<>(SkyType.class);

    private static long lastScanTick = -1;
    private static BlockPos lastScanCenter;
    private static ClientLevel lastLevel;

    private DomainSkyMaskRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) return;
        if (!mc.level.dimension().equals(DomainDimension.ID)) return;

        ClientLevel level = mc.level;
        BlockPos center = mc.player.blockPosition();

        if (level != lastLevel) {
            clearBuffers();
            lastLevel = level;
            lastScanTick = -1;
        }

        if (lastScanCenter == null
                || lastScanTick < 0
                || level.getGameTime() - lastScanTick >= 10
                || center.distSqr(lastScanCenter) > 16) {

            rebuildMasks(level, center);
            lastScanCenter = center.immutable();
            lastScanTick = level.getGameTime();
        }

        if (MASKS.isEmpty()) return;

        mc.getMainRenderTarget().enableStencil();

        Vec3 camera = event.getCamera().getPosition();

        Matrix4f worldMatrix =
                new Matrix4f(event.getPoseStack().last().pose());

        worldMatrix.translate(
                (float) -camera.x,
                (float) -camera.y,
                (float) -camera.z
        );

        Matrix4f skyMatrix =
                new Matrix4f(event.getPoseStack().last().pose());

        Matrix4f projection = event.getProjectionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);

        try {
            for (SkyType type : SkyType.values()) {
                VertexBuffer mask = MASKS.get(type);
                if (mask == null) continue;

                GL11.glClearStencil(0);
                GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

                // Write sky-block faces into stencil.
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilOp(
                        GL11.GL_KEEP,
                        GL11.GL_KEEP,
                        GL11.GL_REPLACE
                );

                RenderSystem.colorMask(false, false, false, false);

                mask.bind();
                mask.drawWithShader(
                        worldMatrix,
                        projection,
                        GameRenderer.getPositionColorShader()
                );
                VertexBuffer.unbind();

                // Draw sky only where mask equals 1.
                RenderSystem.colorMask(true, true, true, true);

                GL11.glStencilMask(0x00);
                GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                GL11.glStencilOp(
                        GL11.GL_KEEP,
                        GL11.GL_KEEP,
                        GL11.GL_KEEP
                );

                VertexBuffer sky = SKIES.get(type);
                if (sky == null) {
                    sky = createSky(type);
                    SKIES.put(type, sky);
                }

                sky.bind();
                sky.drawWithShader(
                        skyMatrix,
                        projection,
                        GameRenderer.getPositionColorShader()
                );
                VertexBuffer.unbind();
            }
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            GL11.glStencilMask(0xFF);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
        }
    }

    private static void rebuildMasks(
            ClientLevel level,
            BlockPos center
    ) {
        for (VertexBuffer buffer : MASKS.values()) {
            buffer.close();
        }
        MASKS.clear();

        for (SkyType type : SkyType.values()) {
            BufferBuilder builder =
                    Tesselator.getInstance().getBuilder();

            builder.begin(
                    VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION_COLOR
            );

            int faces = 0;
            BlockPos.MutableBlockPos pos =
                    new BlockPos.MutableBlockPos();

            for (int x = -TEST_RADIUS; x <= TEST_RADIUS; x++) {
                for (int y = -TEST_RADIUS; y <= TEST_RADIUS; y++) {
                    for (int z = -TEST_RADIUS; z <= TEST_RADIUS; z++) {
                        pos.set(
                                center.getX() + x,
                                center.getY() + y,
                                center.getZ() + z
                        );

                        if (!(level.getBlockState(pos).getBlock()
                                instanceof DomainSkyBlock skyBlock)) {
                            continue;
                        }

                        if (skyBlock.getSkyType() != type) continue;

                        for (Direction face : Direction.values()) {
                            BlockPos neighbor = pos.relative(face);

                            // Only expose sky toward air.
                            if (!level.getBlockState(neighbor).isAir()) {
                                continue;
                            }

                            addFace(builder, pos, face);
                            faces++;
                        }
                    }
                }
            }

            var built = builder.end();

            if (faces == 0) {
                continue;
            }

            VertexBuffer buffer =
                    new VertexBuffer(VertexBuffer.Usage.STATIC);

            buffer.bind();
            buffer.upload(built);
            VertexBuffer.unbind();

            MASKS.put(type, buffer);
        }
    }

    private static void addFace(
            BufferBuilder b,
            BlockPos p,
            Direction direction
    ) {
        float x = p.getX();
        float y = p.getY();
        float z = p.getZ();

        float[][] vertices = switch (direction) {
            case UP -> new float[][]{
                    {0,1,0},{0,1,1},{1,1,1},{1,1,0}
            };
            case DOWN -> new float[][]{
                    {0,0,0},{1,0,0},{1,0,1},{0,0,1}
            };
            case NORTH -> new float[][]{
                    {0,0,0},{0,1,0},{1,1,0},{1,0,0}
            };
            case SOUTH -> new float[][]{
                    {1,0,1},{1,1,1},{0,1,1},{0,0,1}
            };
            case WEST -> new float[][]{
                    {0,0,1},{0,1,1},{0,1,0},{0,0,0}
            };
            case EAST -> new float[][]{
                    {1,0,0},{1,1,0},{1,1,1},{1,0,1}
            };
        };

        for (float[] v : vertices) {
            b.vertex(x + v[0], y + v[1], z + v[2])
                    .color(255, 255, 255, 255)
                    .endVertex();
        }
    }

    private static VertexBuffer createSky(SkyType type) {
        BufferBuilder b = Tesselator.getInstance().getBuilder();

        b.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        int horizontal = 32;
        int vertical = 16;
        float radius = 48F;

        for (int iy = 0; iy < vertical; iy++) {
            double t0 = Math.PI * iy / vertical;
            double t1 = Math.PI * (iy + 1) / vertical;

            for (int ix = 0; ix < horizontal; ix++) {
                double p0 = Math.PI * 2 * ix / horizontal;
                double p1 = Math.PI * 2 * (ix + 1) / horizontal;

                skyVertex(b, type, radius, t0, p0);
                skyVertex(b, type, radius, t1, p0);
                skyVertex(b, type, radius, t1, p1);
                skyVertex(b, type, radius, t0, p1);
            }
        }

        VertexBuffer buffer =
                new VertexBuffer(VertexBuffer.Usage.STATIC);

        buffer.bind();
        buffer.upload(b.end());
        VertexBuffer.unbind();

        return buffer;
    }

    private static void skyVertex(
            BufferBuilder b,
            SkyType type,
            float radius,
            double theta,
            double phi
    ) {
        float x = (float) (
                Math.sin(theta) * Math.cos(phi) * radius
        );
        float y = (float) (Math.cos(theta) * radius);
        float z = (float) (
                Math.sin(theta) * Math.sin(phi) * radius
        );

        float blend = (1F - y / radius) * 0.5F;

        int red;
        int green;
        int blue;

        if (type == SkyType.DAY) {
            red = (int) (55 + 145 * blend);
            green = (int) (125 + 95 * blend);
            blue = 255;
        } else {
            red = (int) (55 + 65 * blend);
            green = (int) (20 + 25 * blend);
            blue = (int) (75 + 55 * blend);
        }

        b.vertex(x, y, z)
                .color(red, green, blue, 255)
                .endVertex();
    }

    private static void clearBuffers() {
        for (VertexBuffer buffer : MASKS.values()) {
            buffer.close();
        }
        for (VertexBuffer buffer : SKIES.values()) {
            buffer.close();
        }

        MASKS.clear();
        SKIES.clear();
        lastScanCenter = null;
    }
}
