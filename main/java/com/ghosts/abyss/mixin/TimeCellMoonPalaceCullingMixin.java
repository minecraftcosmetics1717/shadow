
package com.ghosts.abyss.mixin;

import net.mcreator.jujutsucraft.client.renderer.EntityTimeCellMoonPalaceRenderer;
import net.mcreator.jujutsucraft.entity.EntityTimeCellMoonPalaceEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityTimeCellMoonPalaceRenderer.class, remap = false)
public abstract class TimeCellMoonPalaceCullingMixin
        extends EntityRenderer<EntityTimeCellMoonPalaceEntity> {

    protected TimeCellMoonPalaceCullingMixin(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context
    ) {
        super(context);
    }

    @Override
    public boolean shouldRender(
            EntityTimeCellMoonPalaceEntity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        // Temporarily disable frustum culling for this entity.
        return true;
    }
}
