
package com.ghosts.abyss.client;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketAnchorBlue;

import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ControlledBlueInput {

    private ControlledBlueInput() {}

    @SubscribeEvent
    public static void onMouseButton(
            InputEvent.MouseButton.Pre event
    ) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null) {
            return;
        }

        if ((int) mc.player.getPersistentData()
                .getDouble("skill") != 206) {
            return;
        }

        // The server validates the actual held state.
        ModNetwork.CHANNEL.sendToServer(
                new PacketAnchorBlue()
        );
    }
}
