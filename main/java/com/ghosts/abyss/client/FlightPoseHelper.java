
package com.ghosts.abyss.client;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FlightPoseHelper {

    private FlightPoseHelper() {}

    public static boolean shouldUseFlightPose(Player player) {
        if (player == null || !player.isAlive()) return false;
        if (!player.hasEffect(ModEffects.FLIGHT.get())) return false;
        if (player.onGround()) return false;
        if (player.isPassenger()) return false;
        if (player.isInWaterOrBubble() || player.isInLava()) return false;
        if (player.isFallFlying()) return false;
        if (!player.isSprinting()) return false;

        Minecraft mc = Minecraft.getInstance();

        if (player == mc.player) {
            return UroAirSwimClient.isAirSwimming();
        }

        // Approximate the state for other players using
        // information already synchronized by Minecraft.
        Vec3 motion = player.getDeltaMovement();

        double horizontalSpeed =
                motion.x * motion.x + motion.z * motion.z;

        return horizontalSpeed > 0.01D;
    }
}
