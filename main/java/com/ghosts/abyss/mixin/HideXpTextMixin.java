package com.ghosts.abyss.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphics.class)
public class HideXpTextMixin {

    @Inject(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jslop$hideXpNumber(Font font, String text, int x, int y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String targetLevelStr = String.valueOf(mc.player.experienceLevel);
            if (text != null && text.equals(targetLevelStr)) {

                cir.setReturnValue(x);
            }
        }
    }
}