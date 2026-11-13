package com.ghosts.abyss.client;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketFireRed;
import com.ghosts.abyss.network.PacketRedScroll;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ControlledRedInput {
    private ControlledRedInput() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null
                || !RedControlClientState.isControlling()) {
            return;
        }

        event.setCanceled(true);
        ModNetwork.CHANNEL.sendToServer(new PacketFireRed());
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null
                || !RedControlClientState.isControlling()
                || event.getScrollDelta() == 0.0D) {
            return;
        }

        event.setCanceled(true);
        ModNetwork.CHANNEL.sendToServer(
                new PacketRedScroll(event.getScrollDelta() > 0.0D ? 1 : -1));
    }
}
