
package com.ghosts.abyss.client;

import com.ghosts.abyss.effect.ModEffects;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketFlightToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public final class FlightModeClient {

    private static boolean lastRequested = false;
    private static int refreshTicks = 0;
    private static LocalPlayer lastPlayer;

    private FlightModeClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            lastPlayer = null;
            lastRequested = false;
            refreshTicks = 0;
            return;
        }

        if (lastPlayer != player) {
            lastPlayer = player;
            lastRequested = false;
            refreshTicks = 0;
        }

        boolean eligible =
                player.isAlive()
                        && !player.isSpectator()
                        && player.hasEffect(ModEffects.FLIGHT.get())
                        && player.getAbilities().mayfly
                        && !player.isPassenger()
                        && !player.isInWaterOrBubble()
                        && !player.isInLava();

        boolean requestFlight =
                eligible
                        && mc.screen == null
                        && !player.onGround()
                        && (player.getAbilities().flying
                        || player.isFallFlying())
                        && player.isSprinting()
                        && player.input.forwardImpulse > 0.0F;

        refreshTicks++;

        if (requestFlight != lastRequested || refreshTicks >= 5) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketFlightToggle(requestFlight)
            );

            lastRequested = requestFlight;
            refreshTicks = 0;
        }
    }
}
