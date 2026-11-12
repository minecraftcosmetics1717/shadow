
package com.ghosts.abyss.mixin.client;

import com.ghosts.abyss.client.UroAirSwimClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
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
    private void abyss$uroFlyingPose(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || entity != mc.player) return;
        if (!UroAirSwimClient.isAirSwimming()) return;

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;

        // Horizontal flying body.
        model.body.xRot = (float) Math.PI / 2.0F;

        // Head faces forward.
        model.head.xRot = -0.35F;
        model.head.yRot = 0.0F;

        // Arms trail backward, similar to Elytra flight.
        model.rightArm.xRot = -(float) Math.PI;
        model.leftArm.xRot = -(float) Math.PI;

        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;

        model.rightArm.zRot = 0.12F;
        model.leftArm.zRot = -0.12F;

        // Straight legs.
        model.rightLeg.xRot = 0.0F;
        model.leftLeg.xRot = 0.0F;

        model.rightLeg.yRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
    }
}
