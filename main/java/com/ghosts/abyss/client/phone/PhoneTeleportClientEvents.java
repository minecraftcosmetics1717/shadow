
package com.ghosts.abyss.client.phone;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "abyss",
        value = Dist.CLIENT
)
public final class PhoneTeleportClientEvents {

    private PhoneTeleportClientEvents() {}

    @SubscribeEvent
    public static void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        PhoneTeleportClient.clear();
    }
}
