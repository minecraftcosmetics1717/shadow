
package com.ghosts.abyss.client;

import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketUroAirSwim;
import com.ghosts.abyss.uro.UroAirSwimHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public final class UroAirSwimClient {

    private static boolean active;
    private static boolean lastSent;
    private static int refreshTicks;

    private UroAirSwimClient() {}

    public static boolean isAirSwimming() {
        return active;
    }

    private static boolean canFly(LocalPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.hasEffect(ModEffects.FLIGHT.get())
                && player.getAbilities().flying
                && !player.onGround()
                && !player.isPassenger()
                && !player.isInWaterOrBubble()
                && !player.isInLava()
                && !player.isFallFlying();
    }

    private static void send(boolean flying) {
        refreshTicks++;

        if (flying != lastSent || refreshTicks >= 5) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketUroAirSwim(flying)
            );

            lastSent = flying;
            refreshTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            active = false;
            lastSent = false;
            refreshTicks = 0;
            return;
        }

        boolean shouldFly =
                mc.screen == null
                        && canFly(player)
                        && player.isSprinting()
                        && player.input.forwardImpulse > 0.0F;

        active = shouldFly;

        if (!active) {
            send(false);
            return;
        }

        // Accelerate toward the direction the camera faces.
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 target = direction.scale(UroAirSwimHandler.MAX_SPEED);
        Vec3 current = player.getDeltaMovement();

        double acceleration = UroAirSwimHandler.ACCELERATION;

        Vec3 movement = new Vec3(
                current.x + (target.x - current.x) * acceleration,
                current.y + (target.y - current.y) * acceleration,
                current.z + (target.z - current.z) * acceleration
        );

        player.setDeltaMovement(movement);
        player.fallDistance = 0.0F;

        send(true);
    }
}
