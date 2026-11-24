package com.ghosts.abyss.client;

import com.ghosts.abyss.effect.ModEffects;
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
 * A technique ID is intentionally NOT checked: the abyss:flight effect is
 * the only power requirement, so other characters can use this later.
 */
@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public final class FlightModeClient {
    // Allow brief sprint/input changes without flickering between poses.
    private static final int EXIT_GRACE_TICKS = 4;
    private static final int HEARTBEAT_TICKS = 8;

    private static LocalPlayer trackedPlayer;
    private static boolean powered;
    private static boolean lastSent;
    private static int exitTicks;
    private static int heartbeatTicks;

    private FlightModeClient() {}

    /** Used only by the client-side travel mixin. */
    public static boolean isPowered(LocalPlayer player) {
        return player != null && trackedPlayer == player && powered
                && player.hasEffect(ModEffects.FLIGHT.get());
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

        // Predict the transition immediately. The server receives the same
        // request and maintains the synced fall-flying flag for other players.
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
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        // Don't force an upright hitbox when there is no headroom.
        if (restoreCreativeFlight && eligible(player) && canStand(player)
                && !player.onGround()) {
            player.getAbilities().flying = true;
        }
        player.fallDistance = 0.0F;
    }

    private static void sendRequest(boolean desired) {
        heartbeatTicks++;
        if (desired != lastSent || heartbeatTicks >= HEARTBEAT_TICKS) {
            ModNetwork.CHANNEL.sendToServer(new PacketFlightToggle(desired));
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
            return;
        }

        if (trackedPlayer != player) {
            trackedPlayer = player;
            powered = false;
            lastSent = false;
            exitTicks = 0;
            // Force an immediate state-sync on player/world changes.
            heartbeatTicks = HEARTBEAT_TICKS;
        }

        boolean allowed = eligible(player);
        boolean standingRoom = canStand(player);
        boolean canStayProne = !player.onGround() || !standingRoom;
        boolean sprintForward = mc.screen == null
                && player.isSprinting()
                && player.input.forwardImpulse > 0.0F;

        // Keep the input state independent of the creative-flight flag:
        // that flag becomes false when we START fall-flying.
        boolean wantsPower = allowed && sprintForward && canStayProne
                && (powered || player.isFallFlying()
                || player.getAbilities().flying);

        if (!powered) {
            // Only enter from actual airborne creative-style flight.
            if (wantsPower && !player.onGround()) {
                enter(player);
            }
        } else if (!allowed || (player.onGround() && standingRoom)) {
            // Normal landing or effect removal.
            leave(player, false);
        } else if (wantsPower) {
            exitTicks = 0;
        } else if (!standingRoom) {
            // Hold the prone hitbox inside 1-block tunnels, even if sprint
            // is released. The server makes the same clearance check.
            exitTicks = 0;
        } else if (++exitTicks >= EXIT_GRACE_TICKS) {
            leave(player, true);
        }

        if (powered) {
            // Usually this flag is already set. Recover gracefully if some
            // other movement code cleared it during a transition.
            if (!player.isFallFlying()) {
                player.startFallFlying();
            }
            player.fallDistance = 0.0F;
        }

        // During the exit grace period keep the server in powered mode.
        // In a cramped tunnel send false but remain prone until space opens.
        boolean requestPowered = powered && allowed
                && (wantsPower || (standingRoom && exitTicks > 0));
        sendRequest(requestPowered);
    }
}
