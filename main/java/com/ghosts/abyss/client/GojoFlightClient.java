
package com.ghosts.abyss.client;

import com.ghosts.abyss.gojo.GojoFlightHandler;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketGojoFlightInput;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT
)
public final class GojoFlightClient {

    private static final int DOUBLE_TAP_TICKS = 7;
    private static final int HOLD_TICKS = 3;

    private static int tick = 0;
    private static int lastTap = -100;
    private static int secondPressTick = -1;
    private static int lastSentMode = -1;
    private static int refreshTicks = 0;

    private static boolean previousSpace = false;
    private static boolean pendingActivation = false;
    private static boolean active = false;
    private static boolean ascending = false;
    private static boolean ascentUsed = false;

    private GojoFlightClient() {}

    private static boolean eligible(LocalPlayer player) {
        JujutsucraftModVariables.PlayerVariables vars =
                player.getCapability(
                        JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY
                ).orElse(null);

        return vars != null
                && vars.FlagSixEyes
                && (vars.PlayerCurseTechnique == 2.0
                || vars.PlayerCurseTechnique2 == 2.0)
                && !player.isCreative()
                && !player.isSpectator();
    }

    private static void sendMode(int mode) {
        refreshTicks++;

        if (mode != lastSentMode || refreshTicks >= 5) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketGojoFlightInput(mode)
            );
            lastSentMode = mode;
            refreshTicks = 0;
        }
    }

    private static void reset(LocalPlayer player) {
        if (active || pendingActivation || lastSentMode > 0) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketGojoFlightInput(GojoFlightHandler.OFF)
            );
        }

        if (active) {
            player.setNoGravity(false);
        }

        active = false;
        ascending = false;
        ascentUsed = false;
        pendingActivation = false;
        secondPressTick = -1;
        lastTap = -100;
        lastSentMode = GojoFlightHandler.OFF;
        refreshTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            active = false;
            ascending = false;
            pendingActivation = false;
            previousSpace = false;
            lastTap = -100;
            lastSentMode = -1;
            return;
        }

        tick++;

        boolean allowed = eligible(player);
        boolean space = allowed
                && mc.screen == null
                && mc.options.keyJump.isDown();

        boolean pressed = space && !previousSpace;
        boolean released = !space && previousSpace;

        previousSpace = space;

        if (!allowed || !player.isAlive()
                || player.isPassenger()
                || player.isInWaterOrBubble()
                || player.isInLava()
                || player.onClimbable()) {
            reset(player);
            return;
        }

        if (player.onGround() && (active || pendingActivation)) {
            reset(player);
            return;
        }

        if (pressed) {
            if (tick - lastTap <= DOUBLE_TAP_TICKS) {

                lastTap = -100;

                if (active) {
                    // Double-tap while hovering:
                    // turn flight OFF and fall.
                    reset(player);
                    return;

                } else {
                    // Double-tap detected.
                    // Wait until the second press is held.
                    pendingActivation = true;
                    secondPressTick = tick;
                }

            } else {
                // First tap of a possible double-tap.
                lastTap = tick;
            }
        }

        if (pendingActivation) {
            if (released || !space) {
                // Second press was not held long enough.
                pendingActivation = false;
                secondPressTick = -1;

            } else if (tick - secondPressTick >= HOLD_TICKS
                    && !player.onGround()) {

                // Only a double-tap-and-HOLD activates ascent.
                pendingActivation = false;
                active = true;
                ascending = true;
                ascentUsed = true;
                lastTap = -100;
            }
        }

        if (active && released) {
            ascending = false;
        }

// A new SPACE press while hovering resumes ascent.
// Double-tapping still cancels flight through the
// earlier double-tap detection.
        if (active && pressed) {
            ascending = true;
        }

        if (!active) {
            sendMode(GojoFlightHandler.OFF);
            return;
        }

        player.fallDistance = 0.0F;

        if (ascending && space && ascentUsed) {
            player.setNoGravity(false);

            // Match the server's faster ascent physics.
            Vec3 motion = player.getDeltaMovement();

            double targetSpeed = 0.40D;
            double acceleration = 0.12D;

            double newY = motion.y
                    + (targetSpeed - motion.y) * acceleration;

            player.setDeltaMovement(
                    motion.x,
                    newY,
                    motion.z
            );

            sendMode(GojoFlightHandler.ASCEND);

        } else {
            // Hover, even if SPACE is pressed once.
            player.setNoGravity(true);

            Vec3 motion = player.getDeltaMovement();
            double y = motion.y * 0.55D;

            if (Math.abs(y) < 0.005D) {
                y = 0.0D;
            }

            player.setDeltaMovement(motion.x, y, motion.z);
            sendMode(GojoFlightHandler.HOVER);
        }
    }
}
