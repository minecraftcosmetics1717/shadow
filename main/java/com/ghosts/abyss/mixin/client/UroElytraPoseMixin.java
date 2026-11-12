
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.FlightPoseHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class UroElytraPoseMixin {

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void abyss$flightPose(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) return;
        if (!FlightPoseHelper.shouldUseFlightPose(player)) return;

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;

        // Vanilla-style Elytra flying posture.
        // The renderer mixin rotates the entire player.
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;

        model.head.xRot = -(float) Math.PI / 4.0F;
        model.head.yRot = 0.0F;
        model.head.zRot = 0.0F;

        model.rightArm.xRot = -(float) Math.PI;
        model.leftArm.xRot = -(float) Math.PI;

        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;

        model.rightArm.zRot = 0.12F;
        model.leftArm.zRot = -0.12F;

        model.rightLeg.xRot = 0.0F;
        model.leftLeg.xRot = 0.0F;

        model.rightLeg.yRot = 0.0F;
        model.leftLeg.yRot = 0.0F;

        model.rightLeg.zRot = 0.0F;
        model.leftLeg.zRot = 0.0F;

        model.hat.copyFrom(model.head);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.jacket.copyFrom(model.body);
    }
}
