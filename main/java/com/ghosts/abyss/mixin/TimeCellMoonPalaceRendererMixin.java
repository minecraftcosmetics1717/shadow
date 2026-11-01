
package com.ghosts.abyss.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.jujutsucraft.client.model.ModelTimeCellMoonPalace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelTimeCellMoonPalace.class, remap = false)
public class TimeCellMoonPalaceRendererMixin {

    @Inject(
            method = "m_7695_",
            at = @At("HEAD"),
            remap = false
    )
    private void abyss$scale(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci
    ) {
        poseStack.pushPose();
        poseStack.scale(3.0F, 3.0F, 3.0F);
    }

    @Inject(
            method = "m_7695_",
            at = @At("TAIL"),
            remap = false
    )
    private void abyss$restore(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci
    ) {
        poseStack.popPose();
    }
}
