package com.ghosts.abyss.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.slyrienmodern.com.client.hud.CombatHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatHudRenderer.class)
public abstract class CombatHudRendererMixin {

    /**
     * Prevents the circular Cursed Energy arc from drawing around the main icon.
     */
    @Inject(
            method = "drawCirclingArc",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void cancelCirclingArc(GuiGraphics gui, int centerX, int centerY, int radius, int thickness, float percentage, int color, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Cancels renderContent right before the sub-HUD box, sub-icons, and keybind hints are drawn.
     */
    @Inject(
            method = "renderContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/slyrienmodern/com/util/RenderUtil;drawAngledSlantedBox(Lnet/minecraft/client/gui/GuiGraphics;IIIIII)V",
                    ordinal = 0
            ),
            cancellable = true,
            remap = false
    )
    private static void cancelSubHudAndHints(GuiGraphics gui, LocalPlayer player, float delta, boolean isEditing, CallbackInfo ci) {
        ci.cancel();
    }
}