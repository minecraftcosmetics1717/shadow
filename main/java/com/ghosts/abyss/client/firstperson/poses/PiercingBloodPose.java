
package com.ghosts.abyss.client.firstperson.poses;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class PiercingBloodPose {

    // How far below the final position the hands begin.
    // Increase this to make them start farther below the screen.
    private static final float START_BELOW = 1.50F;

    private PiercingBloodPose() {}

    public static void render(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            AbstractClientPlayer player,
            PlayerRenderer renderer,
            float progress
    ) {
        float t = Math.max(0.0F, Math.min(1.0F, progress));

        // Smooth ease-out: fast at first, slower near the end.
        float eased = 1.0F - (float) Math.pow(1.0F - t, 3.0F);

        // At the beginning: hands are below the screen.
        // At the end: offset becomes zero.
        float riseOffset = -START_BELOW * (1.0F - eased);

        // RIGHT ARM
        pose.pushPose();

        pose.translate(
                -0.18F,
                -0.50F + riseOffset,
                -0.25F
        );

        // Keep the exact final rotations.
        pose.mulPose(Axis.XP.rotationDegrees(-85.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-10.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-40.0F));

        renderer.renderRightHand(
                pose, buffers, light, player
        );

        pose.popPose();

        // LEFT ARM
        pose.pushPose();

        pose.translate(
                0.18F,
                -0.50F + riseOffset,
                -0.25F
        );

        // Keep the exact final rotations.
        pose.mulPose(Axis.XP.rotationDegrees(-85.0F));
        pose.mulPose(Axis.YP.rotationDegrees(10.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(40.0F));

        renderer.renderLeftHand(
                pose, buffers, light, player
        );

        pose.popPose();
    }
}
