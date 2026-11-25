
package com.ghosts.abyss.mixin;

import com.ghosts.abyss.client.GrabHudState;

import net.mcreator.jujutsucraft.procedures.LogicCooldownCombatProcedure;
import net.mcreator.jujutsucraft.procedures.OverlayCooldown2Procedure;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import net.slyrienmodern.com.client.hud.CombatHudRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatHudRenderer.class)
public abstract class CombatHudRendererMixin {

    private static final ResourceLocation ABYSS_GRAB_READY =
            new ResourceLocation(
                    "abyss",
                    "textures/screens/icon_grab_ready.png"
            );

    /**
     * Keep Abyss's existing behavior:
     * remove the circular cursed energy arc.
     */
    @Inject(
            method = "drawCirclingArc",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abyss$cancelCirclingArc(
            GuiGraphics gui,
            int centerX,
            int centerY,
            int radius,
            int thickness,
            float percentage,
            int color,
            CallbackInfo ci
    ) {
        ci.cancel();
    }

    /**
     * Draw the grab icon in Slyrien Modern's HUD.
     *
     * Cooldown overlays always take priority.
     * Also preserve Abyss's existing sub-HUD suppression.
     */
    @Inject(
            method = "renderContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/slyrienmodern/com/util/RenderUtil;drawAngledSlantedBox(Lnet/minecraft/client/gui/GuiGraphics;IIIIII)V",
                    ordinal = 0,
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private static void abyss$cancelSubHudAndHints(
            GuiGraphics gui,
            LocalPlayer player,
            float delta,
            boolean isEditing,
            CallbackInfo ci
    ) {

        if (!isEditing && GrabHudState.showReadyIcon()) {

            // Physical attack cooldown.
            boolean combatCooldown =
                    LogicCooldownCombatProcedure.execute(player);

            // Selected skill's individual cooldown.
            boolean skillCooldown =
                    OverlayCooldown2Procedure.execute(player);

            // Only show the custom icon when both
            // cooldown checks are clear.
            if (!combatCooldown && !skillCooldown) {

                gui.blit(
                        ABYSS_GRAB_READY,
                        0, 0,
                        0.0F, 0.0F,
                        32, 32,
                        32, 32
                );
            }
        }

        // Preserve the original Abyss sub-HUD removal.
        ci.cancel();
    }
}
