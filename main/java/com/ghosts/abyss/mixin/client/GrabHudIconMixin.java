package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.GrabHudState;
import net.mcreator.jujutsucraft.client.screens.OverlayDefaultOverlay;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Swap only JC's main 32x32 physical attack icon (not the small
 * 16x16 indicator, not the cooldown icon). The selected grab remains
 * a physical skill and JC keeps drawing the HUD/countdown normally.
 *
 * In JC's OverlayDefaultOverlay, the 9th GuiGraphics.blit invocation
 * is the main icon_punch.png at x=0, y=height-45, size=32x32.
 */
@Mixin(value = OverlayDefaultOverlay.class, remap = false)
public abstract class GrabHudIconMixin {

    private static final ResourceLocation GRAB_READY = new ResourceLocation(
            "abyss", "textures/screens/icon_grab_ready.png");

    @ModifyArg(
            method = "eventHandler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V",
                    ordinal = 8,
                    remap = true
            ),
            index = 0,
            remap = false
    )
    private static ResourceLocation abyss$showGrabReady(ResourceLocation original) {
        return GrabHudState.showReadyIcon() ? GRAB_READY : original;
    }
}
