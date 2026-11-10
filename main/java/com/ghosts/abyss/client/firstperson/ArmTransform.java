
package com.ghosts.abyss.client.firstperson;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public final class ArmTransform {

    private ArmTransform() {}

    public static void apply(
            PoseStack pose,
            float baseX, float baseY, float baseZ,
            float moveX, float moveY, float moveZ,
            float rotX, float rotY, float rotZ,
            float progress
    ) {
        float t = Math.max(0.0F, Math.min(1.0F, progress));

        // Convert Blockbench pixel movement to model units.
        float x = moveX / 16.0F * t;
        float y = moveY / 16.0F * t;
        float z = moveZ / 16.0F * t;

        pose.translate(
                baseX + x,
                baseY + y,
                baseZ + z
        );

        // Apply rotations in a consistent order.
        pose.mulPose(Axis.ZP.rotationDegrees(rotZ * t));
        pose.mulPose(Axis.YP.rotationDegrees(rotY * t));
        pose.mulPose(Axis.XP.rotationDegrees(rotX * t));
    }
}
