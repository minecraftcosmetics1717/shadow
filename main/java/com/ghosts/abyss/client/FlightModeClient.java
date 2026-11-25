package com.ghosts.abyss.client;

import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.flight.PoweredFlightSpeed;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketFlightToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Predicted client state for Abyss's powered, one-block-high flight.
 * Any player with abyss:flight can enter this mode.
 */
@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public final class FlightModeClient {
    private static final int EXIT_GRACE_TICKS = 4;
    private static final int HEARTBEAT_TICKS = 8;

    private static LocalPlayer trackedPlayer;
    private static boolean powered;
    private static boolean lastSent;
    private static int exitTicks;
    private static int heartbeatTicks;

    // A Space key press during prone flight triggers one boost.
    private static boolean lastJumpDown;
    private static long clientTicks;
    private static long boostStartedAt = -1L;
    private static long nextBoostAllowedAt;

    private FlightModeClient() {}

    /** Used by the client-only travel mixin. */
    public static boolean isPowered(LocalPlayer player) {
        return player != null && trackedPlayer == player && powered
                && player.hasEffect(ModEffects.FLIGHT.get());
    }

    /** Same speed curve as the server; movement remains client-predicted. */
    public static double currentSpeed(LocalPlayer player) {
        if (!isPowered(player) || boostStartedAt < 0L) {
            return PoweredFlightSpeed.NORMAL;
        }
        return PoweredFlightSpeed.atElapsedTicks(clientTicks - boostStartedAt);
    }

    private static boolean eligible(LocalPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.hasEffect(ModEffects.FLIGHT.get())
                && player.getAbilities().mayfly
                && !player.isPassenger()
                && !player.isInWaterOrBubble()
                && !player.isInLava();
    }

    private static boolean canStand(LocalPlayer player) {
        AABB standingBox = player.getDimensions(Pose.STANDING)
                .makeBoundingBox(player.position());
        return player.level().noCollision(player, standingBox);
    }

    private static void enter(LocalPlayer player) {
        if (powered) return;
        powered = true;
        exitTicks = 0;
        boostStartedAt = -1L;

        // Predict this once; the server independently sets the synced pose.
        player.getAbilities().flying = false;
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
        player.fallDistance = 0.0F;
    }

    private static void leave(LocalPlayer player, boolean restoreCreativeFlight) {
        if (!powered) return;
        powered = false;
        exitTicks = 0;
        boostStartedAt = -1L;
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        if (restoreCreativeFlight && eligible(player) && canStand(player)
                && !player.onGround()) {
            player.getAbilities().flying = true;
        }
        player.fallDistance = 0.0F;
    }

    private static void sendRequest(boolean desired, boolean boostPress) {
        heartbeatTicks++;
        if (desired != lastSent || boostPress
                || heartbeatTicks >= HEARTBEAT_TICKS) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketFlightToggle(desired, boostPress));
            lastSent = desired;
            heartbeatTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null || mc.getConnection() == null) {
            trackedPlayer = null;
            powered = false;
            lastSent = false;
            exitTicks = 0;
            heartbeatTicks = 0;
            lastJumpDown = false;
            clientTicks = 0L;
            boostStartedAt = -1L;
            nextBoostAllowedAt = 0L;
            return;
        }

        if (trackedPlayer != player) {
            trackedPlayer = player;
            powered = false;
            lastSent = false;
            exitTicks = 0;
            heartbeatTicks = HEARTBEAT_TICKS;
            lastJumpDown = mc.options.keyJump.isDown();
            clientTicks = 0L;
            boostStartedAt = -1L;
            nextBoostAllowedAt = 0L;
        }

        clientTicks++;
        boolean jumpDown = mc.screen == null && mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !lastJumpDown;
        lastJumpDown = jumpDown;

        boolean allowed = eligible(player);
        boolean standingRoom = canStand(player);
        boolean canStayProne = !player.onGround() || !standingRoom;
        boolean sprintForward = mc.screen == null
                && player.isSprinting()
                && player.input.forwardImpulse > 0.0F;

        boolean wantsPower = allowed && sprintForward && canStayProne
                && (powered || player.isFallFlying()
                || player.getAbilities().flying);

        // A press that ENTERS prone flight does not also trigger a boost.
        boolean wasPowered = powered;
        if (!powered) {
            if (wantsPower && !player.onGround()) {
                enter(player);
            }
        } else if (!allowed || (player.onGround() && standingRoom)) {
            leave(player, false);
        } else if (wantsPower) {
            exitTicks = 0;
        } else if (!standingRoom) {
            exitTicks = 0; // Remain prone until the space is tall enough.
        } else if (++exitTicks >= EXIT_GRACE_TICKS) {
            leave(player, true);
        }

        boolean boostPress = false;
        if (wasPowered && powered && allowed && sprintForward
                && jumpPressed && clientTicks >= nextBoostAllowedAt) {
            boostStartedAt = clientTicks;
            nextBoostAllowedAt = clientTicks + PoweredFlightSpeed.RECHARGE_TICKS;
            boostPress = true;
        }

        if (powered) {
            if (!player.isFallFlying()) {
                player.startFallFlying();
            }
            player.fallDistance = 0.0F;
        }

        // Retain the previous low-tunnel and pose-exit synchronization rules.
        boolean requestPowered = powered && allowed
                && (wantsPower || (standingRoom && exitTicks > 0));
        sendRequest(requestPowered, boostPress);
    }
}
