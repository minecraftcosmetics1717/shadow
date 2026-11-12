
package com.ghosts.abyss.gojo;

import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss")
public final class GojoFlightHandler {

    public static final int OFF = 0;
    public static final int HOVER = 1;
    public static final int ASCEND = 2;

    private static final String MODE = "abyss_gojo_flight_mode";
    private static final String LAST_INPUT = "abyss_gojo_last_input";
    private static final String GRAVITY = "abyss_gojo_owned_gravity";
    private static final String LEVITATION = "abyss_gojo_owned_levitation";

    private GojoFlightHandler() {}

    public static boolean eligible(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return false;

        JujutsucraftModVariables.PlayerVariables vars =
                player.getCapability(
                        JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
                ).orElse(null);

        return vars != null
                && vars.FlagSixEyes
                && (vars.PlayerCurseTechnique == 2.0
                || vars.PlayerCurseTechnique2 == 2.0);
    }

    public static void setMode(ServerPlayer player, int mode) {
        if (mode < OFF || mode > ASCEND) return;

        if (!eligible(player)) {
            stop(player);
            return;
        }

        // The client requests movement; the server owns velocity.
        player.getPersistentData().putInt(MODE, mode);
        player.getPersistentData().putLong(
                LAST_INPUT, player.level().getGameTime()
        );

        if (mode == OFF) {
            stop(player);
        }
    }

    private static void setHoverGravity(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(GRAVITY)) {
            player.getPersistentData().putBoolean(GRAVITY, true);
        }
        player.setNoGravity(true);
    }

    private static void restoreGravity(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(GRAVITY)) {
            player.setNoGravity(false);
            player.getPersistentData().remove(GRAVITY);
        }
    }

    private static void removeOwnLevitation(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(LEVITATION)) return;

        MobEffectInstance effect = player.getEffect(MobEffects.LEVITATION);

        if (effect != null && effect.getAmplifier() == 0) {
            player.removeEffect(MobEffects.LEVITATION);
        }

        player.getPersistentData().remove(LEVITATION);
    }

    private static void stop(ServerPlayer player) {
        removeOwnLevitation(player);
        restoreGravity(player);
        player.getPersistentData().remove(MODE);
        player.getPersistentData().remove(LAST_INPUT);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        int mode = player.getPersistentData().getInt(MODE);
        boolean ownedGravity =
                player.getPersistentData().getBoolean(GRAVITY);

        if (mode == OFF && !ownedGravity) return;

        long lastInput = player.getPersistentData().getLong(LAST_INPUT);
        long now = player.level().getGameTime();

        if (!eligible(player)
                || !player.isAlive()
                || player.onGround()
                || player.isPassenger()
                || player.isInWaterOrBubble()
                || player.isInLava()
                || player.onClimbable()
                || now - lastInput > 20) {
            stop(player);
            return;
        }

        player.fallDistance = 0.0F;

        Vec3 motion = player.getDeltaMovement();

        if (mode == ASCEND) {
            restoreGravity(player);

            MobEffectInstance effect =
                    player.getEffect(MobEffects.LEVITATION);

            boolean owned =
                    player.getPersistentData().getBoolean(LEVITATION);

            if (effect == null
                    || (owned && effect.getAmplifier() == 0
                    && effect.getDuration() <= 5)) {

                player.addEffect(new MobEffectInstance(
                        MobEffects.LEVITATION,
                        10,
                        0,
                        false,
                        false,
                        false
                ));

                player.getPersistentData().putBoolean(LEVITATION, true);
            }

            // Moderately faster ascending with smooth acceleration
            double targetSpeed = 0.8D;
            double acceleration = 0.8D;

            double newY = motion.y
                    + (targetSpeed - motion.y) * acceleration;

            player.setDeltaMovement(
                    motion.x,
                    newY,
                    motion.z
            );

            player.hasImpulse = true;

        } else if (mode == HOVER) {
            removeOwnLevitation(player);
            setHoverGravity(player);

            // Smoothly approach zero vertical movement.
            double y = motion.y * 0.55D;

            if (Math.abs(y) < 0.005D) {
                y = 0.0D;
            }

            player.setDeltaMovement(motion.x, y, motion.z);
            player.hasImpulse = true;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stop(player);
        }
    }
}
