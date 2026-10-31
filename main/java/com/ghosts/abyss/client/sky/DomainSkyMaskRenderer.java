
package com.ghosts.abyss.client.sky;

import com.ghosts.abyss.block.DomainSkyBlock;
import com.ghosts.abyss.block.DomainSkyBlock.SkyType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomainSkyMaskRenderer {

    // Number of chunk sections to search around the player.
    private static final int HORIZONTAL_RANGE = 7;
    private static final int VERTICAL_RANGE = 4;

    // Maximum section checks per client tick.
    private static final int CHECKS_PER_TICK = 12;

    private record SectionKey(int x, int y, int z) {}

    private static class CachedSection {
        long signature;
        EnumMap<SkyType, VertexBuffer> masks =
                new EnumMap<>(SkyType.class);

        CachedSection(long signature) {
            this.signature = signature;
        }

        void close() {
            for (VertexBuffer buffer : masks.values()) {
                buffer.close();
            }
            masks.clear();
        }
    }

    private static final Map<SectionKey, CachedSection> CACHE =
            new HashMap<>();

    private static final EnumMap<SkyType, VertexBuffer> SKIES =
            new EnumMap<>(SkyType.class);

    private static final ArrayDeque<SectionKey> QUEUE =
            new ArrayDeque<>();
    private static final ResourceLocation INFINITE_VOID_SPLASH_TEXTURE =
            new ResourceLocation(
                    "abyss",
                    "textures/sky/infinite_void_splash.png"
            );

    private static VertexBuffer infiniteVoidSplashes;
    private static ClientLevel lastLevel;
    private static SectionKey lastCenter;

    private DomainSkyMaskRenderer() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            if (lastLevel != null) clearAll();
            return;
        }

        ClientLevel level = mc.level;

        if (lastLevel != level) {
            clearAll();
            lastLevel = level;
        }

        BlockPos playerPos = mc.player.blockPosition();

        SectionKey center = new SectionKey(
                playerPos.getX() >> 4,
                playerPos.getY() >> 4,
                playerPos.getZ() >> 4
        );

        if (!center.equals(lastCenter)) {
            lastCenter = center;
            rebuildQueue(center);
            removeDistantSections(center);
        }

        // Continue checking sections in the background
        // of normal client ticks, not all at once.
        if (QUEUE.isEmpty()) {
            rebuildQueue(center);
        }

        for (int i = 0; i < CHECKS_PER_TICK; i++) {
            SectionKey key = QUEUE.poll();

            if (key == null) break;

            checkSection(level, key);
        }
    }

    private static void rebuildQueue(SectionKey center) {
        QUEUE.clear();

        List<SectionKey> keys = new ArrayList<>();

        for (int x = -HORIZONTAL_RANGE;
             x <= HORIZONTAL_RANGE; x++) {

            for (int y = -VERTICAL_RANGE;
                 y <= VERTICAL_RANGE; y++) {

                for (int z = -HORIZONTAL_RANGE;
                     z <= HORIZONTAL_RANGE; z++) {

                    keys.add(new SectionKey(
                            center.x() + x,
                            center.y() + y,
                            center.z() + z
                    ));
                }
            }
        }

        // Check nearby sections first.
        keys.sort(Comparator.comparingInt(key -> {
            int dx = key.x() - center.x();
            int dy = key.y() - center.y();
            int dz = key.z() - center.z();

            return dx * dx + dy * dy + dz * dz;
        }));

        QUEUE.addAll(keys);
    }

    private static void removeDistantSections(SectionKey center) {
        Iterator<Map.Entry<SectionKey, CachedSection>> it =
                CACHE.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<SectionKey, CachedSection> entry =
                    it.next();

            SectionKey key = entry.getKey();

            if (Math.abs(key.x() - center.x()) > HORIZONTAL_RANGE
                    || Math.abs(key.y() - center.y()) > VERTICAL_RANGE
                    || Math.abs(key.z() - center.z()) > HORIZONTAL_RANGE) {

                entry.getValue().close();
                it.remove();
            }
        }
    }

    private static void checkSection(
            ClientLevel level,
            SectionKey key
    ) {
        if (!level.hasChunk(key.x(), key.z())) {
            CachedSection old = CACHE.remove(key);

            if (old != null) old.close();

            return;
        }

        long signature = calculateSignature(level, key);

        CachedSection existing = CACHE.get(key);

        if (existing != null
                && existing.signature == signature) {
            return;
        }

        if (existing != null) existing.close();

        CachedSection replacement =
                new CachedSection(signature);

        for (SkyType type : SkyType.values()) {
            VertexBuffer buffer =
                    buildSectionMask(level, key, type);

            if (buffer != null) {
                replacement.masks.put(type, buffer);
            }
        }

        CACHE.put(key, replacement);
    }

    private static long calculateSignature(
            ClientLevel level,
            SectionKey key
    ) {
        long hash = 1469598103934665603L;

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        int baseX = key.x() << 4;
        int baseY = key.y() << 4;
        int baseZ = key.z() << 4;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    pos.set(baseX + x, baseY + y, baseZ + z);

                    BlockState state = level.getBlockState(pos);

                    if (!(state.getBlock()
                            instanceof DomainSkyBlock sky)) {
                        continue;
                    }

                    long value =
                            ((long) sky.getSkyType().ordinal() + 1L);

                    value = value * 4099L
                            + (x << 8) + (y << 4) + z;

                    for (Direction direction : Direction.values()) {
                        if (level.getBlockState(
                                pos.relative(direction)).isAir()) {
                            value = value * 31L
                                    + direction.ordinal() + 1;
                        }
                    }

                    hash ^= value;
                    hash *= 1099511628211L;
                }
            }
        }

        return hash;
    }

    private static VertexBuffer buildSectionMask(
            ClientLevel level,
            SectionKey key,
            SkyType type
    ) {
        BufferBuilder builder =
                Tesselator.getInstance().getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        int faces = 0;

        int baseX = key.x() << 4;
        int baseY = key.y() << 4;
        int baseZ = key.z() << 4;

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    pos.set(
                            baseX + x,
                            baseY + y,
                            baseZ + z
                    );

                    BlockState state = level.getBlockState(pos);

                    if (!(state.getBlock()
                            instanceof DomainSkyBlock sky)) {
                        continue;
                    }

                    if (sky.getSkyType() != type) continue;

                    for (Direction direction : Direction.values()) {
                        if (!level.getBlockState(
                                pos.relative(direction)).isAir()) {
                            continue;
                        }

                        addFace(builder, pos, direction);
                        faces++;
                    }
                }
            }
        }

        var built = builder.end();

        if (faces == 0) return null;

        VertexBuffer buffer =
                new VertexBuffer(VertexBuffer.Usage.STATIC);

        buffer.bind();
        buffer.upload(built);
        VertexBuffer.unbind();

        return buffer;
    }

    private static void addFace(
            BufferBuilder builder,
            BlockPos pos,
            Direction direction
    ) {
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();

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
            builder.vertex(
                    x + v[0],
                    y + v[1],
                    z + v[2]
            ).color(255, 255, 255, 255).endVertex();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() !=
                RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || CACHE.isEmpty()) return;

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

// Respect the depth buffer so foreground entities
// and blocks can occlude the sky.
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);

        try {
            for (SkyType type : SkyType.values()) {
                GL11.glClearStencil(0);
                GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

// Only mark sky faces that are not hidden
// behind existing world or entity geometry.
                RenderSystem.enableDepthTest();

                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilOp(
                        GL11.GL_KEEP,
                        GL11.GL_KEEP,
                        GL11.GL_REPLACE
                );

                RenderSystem.colorMask(
                        false, false, false, false
                );

                boolean hasMasks = false;

                for (CachedSection section : CACHE.values()) {
                    VertexBuffer mask = section.masks.get(type);

                    if (mask == null) continue;

                    hasMasks = true;

                    mask.bind();
                    mask.drawWithShader(
                            worldMatrix,
                            projection,
                            GameRenderer.getPositionColorShader()
                    );
                }

                VertexBuffer.unbind();

                RenderSystem.colorMask(
                        true, true, true, true
                );

                if (!hasMasks) continue;

// The mask has already been checked against
// the world's depth buffer.
//
// Draw the sky only inside that visible mask.
// Do not depth-test the sky sphere itself.
                RenderSystem.disableDepthTest();

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

                if (type == SkyType.INFINITE_VOID) {
                    if (infiniteVoidSplashes == null) {
                        infiniteVoidSplashes = createInfiniteVoidSplashes();
                    }

                    RenderSystem.setShader(
                            GameRenderer::getPositionTexShader
                    );

                    RenderSystem.setShaderTexture(
                            0,
                            INFINITE_VOID_SPLASH_TEXTURE
                    );

                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableCull();

                    infiniteVoidSplashes.bind();
                    infiniteVoidSplashes.drawWithShader(
                            skyMatrix,
                            projection,
                            GameRenderer.getPositionTexShader()
                    );
                    VertexBuffer.unbind();

                    RenderSystem.disableBlend();

                    RenderSystem.setShader(
                            GameRenderer::getPositionColorShader
                    );
                }


                GL11.glStencilMask(0xFF);
                RenderSystem.enableDepthTest();
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


    private static VertexBuffer createSky(SkyType type) {
        BufferBuilder builder =
                Tesselator.getInstance().getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR
        );

        // Higher resolution for Infinite Void's gradient.
        int horizontal = type == SkyType.INFINITE_VOID ? 64 : 32;
        int vertical = type == SkyType.INFINITE_VOID ? 64 : 16;

        float radius = 48F;

        for (int iy = 0; iy < vertical; iy++) {
            double t0 = Math.PI * iy / vertical;
            double t1 = Math.PI * (iy + 1) / vertical;

            for (int ix = 0; ix < horizontal; ix++) {
                double p0 = Math.PI * 2 * ix / horizontal;
                double p1 = Math.PI * 2 * (ix + 1) / horizontal;

                skyVertex(builder, type, radius, t0, p0);
                skyVertex(builder, type, radius, t1, p0);
                skyVertex(builder, type, radius, t1, p1);
                skyVertex(builder, type, radius, t0, p1);
            }
        }

        // Add stars only to Infinite Void.
        if (type == SkyType.INFINITE_VOID) {
            addInfiniteVoidStars(builder);
        }

        VertexBuffer buffer =
                new VertexBuffer(VertexBuffer.Usage.STATIC);

        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();

        return buffer;
    }

    private static void addInfiniteVoidStars(BufferBuilder builder) {
        // Fixed seed keeps the stars in the same positions.
        Random random = new Random(108L);

        final int starCount = 350;
        final float radius = 47.5F;

        for (int i = 0; i < starCount; i++) {

            // Random position on the sky sphere.
            double y = random.nextDouble() * 2.0 - 1.0;
            double angle = random.nextDouble() * Math.PI * 2.0;

            double horizontal = Math.sqrt(1.0 - y * y);

            float nx = (float) (horizontal * Math.cos(angle));
            float ny = (float) y;
            float nz = (float) (horizontal * Math.sin(angle));

            // Tangent direction.
            float sideX = -nz;
            float sideZ = nx;

            float sideLength = (float) Math.sqrt(
                    sideX * sideX + sideZ * sideZ
            );

            if (sideLength < 0.0001F) {
                continue;
            }

            sideX /= sideLength;
            sideZ /= sideLength;

            // Perpendicular tangent direction.
            float upX = ny * sideZ;
            float upY = nz * sideX - nx * sideZ;
            float upZ = -ny * sideX;

            // Small stars, with occasional larger ones.
            float size = 0.045F
                    + random.nextFloat() * 0.075F;

            if (random.nextInt(12) == 0) {
                size *= 1.5F;
            }

            // Slight variations in star brightness.
            int brightness = 165 + random.nextInt(91);

            float cx = nx * radius;
            float cy = ny * radius;
            float cz = nz * radius;

            starVertex(
                    builder, cx, cy, cz,
                    sideX, 0, sideZ,
                    upX, upY, upZ,
                    -size, -size, brightness
            );

            starVertex(
                    builder, cx, cy, cz,
                    sideX, 0, sideZ,
                    upX, upY, upZ,
                    -size, size, brightness
            );

            starVertex(
                    builder, cx, cy, cz,
                    sideX, 0, sideZ,
                    upX, upY, upZ,
                    size, size, brightness
            );

            starVertex(
                    builder, cx, cy, cz,
                    sideX, 0, sideZ,
                    upX, upY, upZ,
                    size, -size, brightness
            );
        }
    }
    private static VertexBuffer createInfiniteVoidSplashes() {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        builder.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX
        );

        Random random = new Random(2702L);

        final int splashCount = 24;
        final float radius = 46.5F;

        for (int i = 0; i < splashCount; i++) {
            double y = random.nextDouble() * 1.8 - 0.9;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double horizontal = Math.sqrt(1.0 - y * y);

            float nx = (float) (horizontal * Math.cos(angle));
            float ny = (float) y;
            float nz = (float) (horizontal * Math.sin(angle));

            float sideX = -nz;
            float sideZ = nx;
            float sideLength = (float) Math.sqrt(sideX * sideX + sideZ * sideZ);

            if (sideLength < 0.0001F) continue;

            sideX /= sideLength;
            sideZ /= sideLength;

            float upX = ny * sideZ;
            float upY = nz * sideX - nx * sideZ;
            float upZ = -ny * sideX;

            float size = 1.5F + random.nextFloat() * 2.5F;

            float cx = nx * radius;
            float cy = ny * radius;
            float cz = nz * radius;

            splashVertex(builder, cx, cy, cz,
                    sideX, 0F, sideZ, upX, upY, upZ,
                    -size, -size, 0F, 1F);

            splashVertex(builder, cx, cy, cz,
                    sideX, 0F, sideZ, upX, upY, upZ,
                    -size, size, 0F, 0F);

            splashVertex(builder, cx, cy, cz,
                    sideX, 0F, sideZ, upX, upY, upZ,
                    size, size, 1F, 0F);

            splashVertex(builder, cx, cy, cz,
                    sideX, 0F, sideZ, upX, upY, upZ,
                    size, -size, 1F, 1F);
        }

        VertexBuffer buffer =
                new VertexBuffer(VertexBuffer.Usage.STATIC);

        buffer.bind();
        buffer.upload(builder.end());
        VertexBuffer.unbind();

        return buffer;
    }

    private static void splashVertex(
            BufferBuilder builder,
            float cx, float cy, float cz,
            float sx, float sy, float sz,
            float ux, float uy, float uz,
            float horizontal, float vertical,
            float u, float v
    ) {
        builder.vertex(
                cx + sx * horizontal + ux * vertical,
                cy + sy * horizontal + uy * vertical,
                cz + sz * horizontal + uz * vertical
        ).uv(u, v).endVertex();
    }
    private static void starVertex(
            BufferBuilder builder,
            float cx, float cy, float cz,
            float sx, float sy, float sz,
            float ux, float uy, float uz,
            float horizontal, float vertical,
            int brightness
    ) {
        builder.vertex(
                cx + sx * horizontal + ux * vertical,
                cy + sy * horizontal + uy * vertical,
                cz + sz * horizontal + uz * vertical
        ).color(
                brightness,
                brightness,
                brightness,
                255
        ).endVertex();
    }


    private static void skyVertex(
            BufferBuilder builder,
            SkyType type,
            float radius,
            double theta,
            double phi
    )
    {
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


        switch (type) {
            case DAY -> {
                // Existing daytime sky.
                red = (int) (55 + 145 * blend);
                green = (int) (125 + 95 * blend);
                blue = 255;
            }

            case MAHITO -> {
                // Preserve the existing Mahito sky colors.
                red = (int) (76 * (1F - blend));
                green = (int) (35 * (1F - blend));
                blue = (int) (105 * (1F - blend));
            }

            case BLACK -> {
                // Completely black sky.
                red = 0;
                green = 0;
                blue = 0;
            }

            case WHITE -> {
                // Completely white sky.
                red = 255;
                green = 255;
                blue = 255;
            }


            case INFINITE_VOID -> {
                // Black at the top and bottom.
                // #1A202C at the middle (horizon).

                float middle = (float) Math.pow(
                        Math.max(
                                0F,
                                1F - Math.abs(y / radius)
                        ),
                        1.35
                );

                red = Math.round(26F * middle);
                green = Math.round(32F * middle);
                blue = Math.round(44F * middle);
            }


            default -> {
                red = 0;
                green = 0;
                blue = 0;
            }
        }


        builder.vertex(x, y, z)
                .color(red, green, blue, 255)
                .endVertex();
    }

    private static void clearAll() {
        for (CachedSection section : CACHE.values()) {
            section.close();
        }

        for (VertexBuffer sky : SKIES.values()) {
            sky.close();
        }
        if (infiniteVoidSplashes != null) {
            infiniteVoidSplashes.close();
            infiniteVoidSplashes = null;
        }
        CACHE.clear();
        SKIES.clear();
        QUEUE.clear();

        lastLevel = null;
        lastCenter = null;
    }
}
