
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.sixeyes.SixEyesVisionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class SixEyesNightVisionMixin {

    @Inject(
            method = "getNightVisionScale",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void abyss$disableSixEyesNightVision(
            LivingEntity entity,
            float partialTick,
            CallbackInfoReturnable<Float> cir
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || entity != mc.player) {
            return;
        }

        if (!SixEyesVisionHandler.shouldRender(mc.player)) {
            return;
        }

        // Zero means no Night Vision brightness boost.
        cir.setReturnValue(0.0F);
    }
}
