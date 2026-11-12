
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.FlightPoseHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class FlightPlayerRotationMixin {

    @Inject(
            method = "setupRotations",
            at = @At("TAIL")
    )
    private void abyss$rotateFlyingPlayer(
            LivingEntity entity,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) return;
        if (!FlightPoseHelper.shouldUseFlightPose(player)) return;

        // Rotate the whole character into a horizontal
        // flying position without changing entity physics.
        float pitch = entity.getViewXRot(partialTicks);

        poseStack.mulPose(
                Axis.XP.rotationDegrees(90.0F - pitch)
        );
    }
}
