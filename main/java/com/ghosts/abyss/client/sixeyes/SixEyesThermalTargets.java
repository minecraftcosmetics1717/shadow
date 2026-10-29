
package com.ghosts.abyss.client.sixeyes;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

public final class SixEyesThermalTargets {

    private static RenderTarget entityMask;

    private SixEyesThermalTargets() {}

    public static RenderTarget getEntityMask() {
        return entityMask;
    }

    public static void ensureCreated() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getWindow() == null) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (entityMask != null
                && entityMask.width == width
                && entityMask.height == height) {
            return;
        }

        destroy();

        entityMask = new TextureTarget(
                width,
                height,
                true,
                Minecraft.ON_OSX
        );

        entityMask.setClearColor(0f, 0f, 0f, 0f);
        entityMask.clear(Minecraft.ON_OSX);
    }

    public static void clear() {
        if (entityMask == null) {
            return;
        }

        entityMask.setClearColor(0f, 0f, 0f, 0f);
        entityMask.clear(Minecraft.ON_OSX);
    }

    public static void destroy() {
        if (entityMask != null) {
            entityMask.destroyBuffers();
            entityMask = null;
        }
    }
}
