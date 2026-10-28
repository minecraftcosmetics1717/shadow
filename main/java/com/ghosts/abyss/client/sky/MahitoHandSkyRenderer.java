package com.ghosts.abyss.client.sky;

import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Client-only, world-anchored hand lattice drawn through Mahito's existing stencil mask. */
public final class MahitoHandSkyRenderer {
    private static final ResourceLocation MODEL = new ResourceLocation("abyss", "models/sky/2hands.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("abyss", "textures/sky/handtexture.png");
    private static final int GRID_RADIUS = 12; // 17 x 17 models, 272 blocks wide
    private static final float HEIGHT = 129.0f;
    private static VertexBuffer mesh;
    private static boolean attempted;
    private static net.minecraft.server.packs.resources.ResourceManager resourceManager;

    private MahitoHandSkyRenderer() {}

    public static void render(Vec3 camera, Matrix4f basePose, Matrix4f projection, int arenaCenterX) {
        var manager = Minecraft.getInstance().getResourceManager();
        if (resourceManager != manager) {
            reset();
            resourceManager = manager;
        }
        if (!attempted) {
            attempted = true;
            build(manager);
        }
        if (mesh == null) return;

// Snap the mesh origin to whole world blocks.
// This keeps the repeating pattern aligned to the world,
// while allowing it to extend around the player.
        double originX = Math.floor(camera.x);
        double originZ = Math.floor(camera.z);

        Matrix4f matrix = new Matrix4f(basePose);

        matrix.translate(
                (float) (originX - camera.x),
                (float) -camera.y,
                (float) (originZ - camera.z)
        );
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        mesh.bind();
        mesh.drawWithShader(matrix, projection, GameRenderer.getPositionTexShader());
        VertexBuffer.unbind();
    }

    public static void reset() {
        if (mesh != null) mesh.close();
        mesh = null;
        attempted = false;
    }

    private static void build(net.minecraft.server.packs.resources.ResourceManager manager) {
        try (var stream = manager.open(MODEL);
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            for (int ix = -GRID_RADIUS; ix <= GRID_RADIUS; ix++) {
                for (int iz = -GRID_RADIUS; iz <= GRID_RADIUS; iz++) {
                    float offsetX = ix;
                    float offsetZ = iz;
                    for (JsonElement item : elements) {
                        appendElement(b, item.getAsJsonObject(), offsetX, offsetZ);
                    }
                }
            }
            var built = b.end();
            mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);
            mesh.bind();
            mesh.upload(built);
            VertexBuffer.unbind();
        } catch (Exception e) {
            System.err.println("[Abyss] Could not build Mahito sky hands: " + e);
        }
    }

    private static float[] vec(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    private static void appendElement(BufferBuilder b, JsonObject element, float tx, float tz) {
        float[] lo = vec(element.getAsJsonArray("from"));
        float[] hi = vec(element.getAsJsonArray("to"));
        float[] origin = {8, 8, 8};
        float angle = 0;
        String axis = "y";
        if (element.has("rotation")) {
            JsonObject rot = element.getAsJsonObject("rotation");
            if (rot.has("origin")) origin = vec(rot.getAsJsonArray("origin"));
            if (rot.has("angle")) {
                angle = rot.get("angle").getAsFloat();
                axis = rot.get("axis").getAsString();
            } else if (rot.has("y")) {
                angle = rot.get("y").getAsFloat();
            } else if (rot.has("x")) {
                angle = rot.get("x").getAsFloat();
                axis = "x";
            } else if (rot.has("z")) {
                angle = rot.get("z").getAsFloat();
                axis = "z";
            }
        }
        JsonObject faces = element.getAsJsonObject("faces");
        // Vertex order for each face, with UV corners in matching order.
        String[] names = {"north", "south", "west", "east", "up", "down"};
        float[][][] corners = {
            {{lo[0],lo[1],lo[2]},{lo[0],hi[1],lo[2]},{hi[0],hi[1],lo[2]},{hi[0],lo[1],lo[2]}},
            {{hi[0],lo[1],hi[2]},{hi[0],hi[1],hi[2]},{lo[0],hi[1],hi[2]},{lo[0],lo[1],hi[2]}},
            {{lo[0],lo[1],hi[2]},{lo[0],hi[1],hi[2]},{lo[0],hi[1],lo[2]},{lo[0],lo[1],lo[2]}},
            {{hi[0],lo[1],lo[2]},{hi[0],hi[1],lo[2]},{hi[0],hi[1],hi[2]},{hi[0],lo[1],hi[2]}},
            {{lo[0],hi[1],lo[2]},{lo[0],hi[1],hi[2]},{hi[0],hi[1],hi[2]},{hi[0],hi[1],lo[2]}},
            {{lo[0],lo[1],lo[2]},{hi[0],lo[1],lo[2]},{hi[0],lo[1],hi[2]},{lo[0],lo[1],hi[2]}}
        };
        for (int face = 0; face < names.length; face++) {
            if (!faces.has(names[face])) continue;
            JsonObject f = faces.getAsJsonObject(names[face]);
            JsonArray uv = f.getAsJsonArray("uv");
            if (uv == null) continue;
            float u0 = uv.get(0).getAsFloat()/16f;
            float v0 = uv.get(1).getAsFloat()/16f;
            float u1 = uv.get(2).getAsFloat()/16f;
            float v1 = uv.get(3).getAsFloat()/16f;
            float[][] coords = {{u0,v1},{u0,v0},{u1,v0},{u1,v1}};
            int turns = f.has("rotation") ? Math.floorMod(f.get("rotation").getAsInt()/90,4) : 0;
            for (int i = 0; i < 4; i++) {
                float[] p = corners[face][i].clone();
                rotate(p, origin, axis, angle);
                float[] uvp = coords[(i + turns) % 4];
                b.vertex(tx + p[0]/16f, HEIGHT + p[1]/16f, tz + p[2]/16f)
                        .uv(uvp[0], uvp[1]).endVertex();
            }
        }
    }

    private static void rotate(float[] p, float[] origin, String axis, float degrees) {
        if (degrees == 0) return;
        float x = p[0] - origin[0], y = p[1] - origin[1], z = p[2] - origin[2];
        double a = Math.toRadians(degrees), c = Math.cos(a), s = Math.sin(a);
        switch (axis) {
            case "x" -> {p[1] = origin[1] + (float)(y*c-z*s); p[2] = origin[2] + (float)(y*s+z*c);}
            case "z" -> {p[0] = origin[0] + (float)(x*c-y*s); p[1] = origin[1] + (float)(x*s+y*c);}
            default -> {p[0] = origin[0] + (float)(x*c+z*s); p[2] = origin[2] + (float)(-x*s+z*c);}
        }
    }
}
