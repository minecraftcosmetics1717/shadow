
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.sixeyes.SixEyesMaskCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class SixEyesEntityDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void abyss$captureEntity(
            E entity,
            double x,
            double y,
            double z,
            float rotationYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        SixEyesMaskCapture.capture(entity);
    }
}
