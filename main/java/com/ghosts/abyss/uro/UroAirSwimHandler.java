
package com.ghosts.abyss.uro;

import com.ghosts.abyss.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class UroAirSwimHandler {

    // Blocks per tick.
    public static final double MAX_SPEED = 0.65D;

    // Higher values reach the target speed faster.
    public static final double ACCELERATION = 0.12D;

    private static final String ACTIVE = "abyss_uro_air_swim";
    private static final String LAST_INPUT = "abyss_uro_swim_input_time";

    private UroAirSwimHandler() {}

    public static boolean canSwim(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.hasEffect(ModEffects.FLIGHT.get())
                && player.getAbilities().flying
                && !player.isPassenger()
                && !player.isInWaterOrBubble()
                && !player.isInLava()
                && !player.isFallFlying();
    }

    public static void setSwimming(ServerPlayer player, boolean swimming) {
        if (!swimming || !canSwim(player)) {
            stop(player);
            return;
        }

        player.getPersistentData().putBoolean(ACTIVE, true);
        player.getPersistentData().putLong(
                LAST_INPUT,
                player.level().getGameTime()
        );
    }

    private static void stop(ServerPlayer player) {
        player.getPersistentData().remove(ACTIVE);
        player.getPersistentData().remove(LAST_INPUT);

        // Clear the old forced swimming pose, if present.
        if (player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(null);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        if (!player.getPersistentData().getBoolean(ACTIVE)) return;

        long lastInput =
                player.getPersistentData().getLong(LAST_INPUT);

        if (!canSwim(player)
                || player.level().getGameTime() - lastInput > 10) {
            stop(player);
            return;
        }


        // The direction comes from the server's view of the player,
        // not from an arbitrary velocity supplied by the client.
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 target = direction.scale(MAX_SPEED);
        Vec3 current = player.getDeltaMovement();

        Vec3 movement = new Vec3(
                current.x + (target.x - current.x) * ACCELERATION,
                current.y + (target.y - current.y) * ACCELERATION,
                current.z + (target.z - current.z) * ACCELERATION
        );

        player.setDeltaMovement(movement);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stop(player);
        }
    }
}
