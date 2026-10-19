package com.ghosts.abyss.mixin;

import com.ghosts.abyss.client.ShadowDiveSync;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.ghosts.abyss.client.FourArmLayer", remap = false)
public class HideFourArmsInShadowDiveMixin {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void jreborn$hideFourArmsInShadowDive(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        Boolean diving = ShadowDiveSync.MAP.get(player.getUUID());
        if (Boolean.TRUE.equals(diving)) {
            ci.cancel();
        }
    }
}