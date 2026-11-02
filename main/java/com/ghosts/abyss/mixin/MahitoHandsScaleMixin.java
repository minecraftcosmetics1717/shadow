
package com.ghosts.abyss.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mcreator.jujutsucraft.client.renderer.EntityHandsRenderer;
import net.mcreator.jujutsucraft.entity.EntityHandsEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityHandsRenderer.class, remap = false)
public abstract class MahitoHandsScaleMixin
        extends MobRenderer<EntityHandsEntity,
        net.mcreator.jujutsucraft.client.model.Modelself_embodiment_of_perfection<EntityHandsEntity>> {

    protected MahitoHandsScaleMixin(EntityRendererProvider.Context context) {
        super(context, null, 0.0F);
    }

    @Override
    protected void scale(
            EntityHandsEntity entity,
            PoseStack poseStack,
            float partialTick
    ) {
        super.scale(entity, poseStack, partialTick);

        // Scale Mahito's hands to 3x their original size.
        poseStack.scale(3.0F, 3.0F, 3.0F);
    }

    @Override
    public boolean shouldRender(
            EntityHandsEntity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        // Prevent the enlarged hands from disappearing
        // when their original hitbox leaves the screen.
        return true;
    }
}
