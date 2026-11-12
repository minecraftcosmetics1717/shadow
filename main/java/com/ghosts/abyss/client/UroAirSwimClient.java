
package com.ghosts.abyss.client;

import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketUroAirSwim;
import com.ghosts.abyss.uro.UroAirSwimHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT
)
public final class UroAirSwimClient {

    private static boolean active = false;
    public static boolean isAirSwimming() {
        return active;
    }
    private static boolean lastSent = false;
    private static int refreshTicks = 0;

    private UroAirSwimClient() {}

    private static boolean canSwim(LocalPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.hasEffect(ModEffects.FLIGHT.get())
                && player.getAbilities().flying
                && !player.isPassenger()
                && !player.isInWaterOrBubble()
                && !player.isInLava()
                && !player.isFallFlying();
    }

    private static void send(boolean swimming) {
        refreshTicks++;

        if (swimming != lastSent || refreshTicks >= 5) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketUroAirSwim(swimming)
            );

            lastSent = swimming;
            refreshTicks = 0;
        }
    }

    private static void stop(LocalPlayer player) {

        active = false;
        send(false);
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

        // Sprint + forward movement activates air swimming.
        boolean wantsSwimming =
                mc.screen == null
                        && canSwim(player)
                        && player.isSprinting()
                        && mc.options.keyUp.isDown();

        if (!wantsSwimming) {
            stop(player);
            return;
        }

        active = true;


        // Swim toward the direction the camera is looking.
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 target = direction.scale(
                UroAirSwimHandler.MAX_SPEED
        );

        Vec3 current = player.getDeltaMovement();

        double acceleration =
                UroAirSwimHandler.ACCELERATION;

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
