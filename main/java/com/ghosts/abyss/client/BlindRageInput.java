package com.ghosts.abyss.client;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketBlindRageInput;
import net.mcreator.jujutsucraft.network.JujutsucraftModVariables;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Existing left-click grab input, shared by Uro and Mahito. */
@Mod.EventBusSubscriber(modid = "abyss", bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class BlindRageInput {
    private static boolean pressed;

    private BlindRageInput() {}

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        Minecraft mc = Minecraft.getInstance();

        if (event.getAction() == GLFW.GLFW_PRESS && canGrab(mc)) {
            // Prevent ordinary punching/block breaking while either grab is armed.
            event.setCanceled(true);
            if (!pressed) {
                pressed = true;
                ModNetwork.CHANNEL.sendToServer(new PacketBlindRageInput(true));
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE && pressed) {
            // Do not cancel mouse release: vanilla can clear its input state.
            stop(mc);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pressed) return;
        Minecraft mc = Minecraft.getInstance();
        if (!canGrab(mc)
                || GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            stop(mc);
        }
    }

    private static void stop(Minecraft mc) {
        pressed = false;
        if (mc.getConnection() != null) {
            ModNetwork.CHANNEL.sendToServer(new PacketBlindRageInput(false));
        }
    }

    private static boolean canGrab(Minecraft mc) {
        if (mc.player == null || mc.screen != null || mc.player.isSpectator())
            return false;
        var vars = mc.player.getCapability(
                JujutsucraftModVariables.PLAYER_VARIABLES_CAPABILITY).orElse(null);
        if (vars == null) return false;
        double ct = vars.SecondTechnique
                ? vars.PlayerCurseTechnique2 : vars.PlayerCurseTechnique;
        int technique = (int) Math.round(ct);
        int selected = (int) Math.round(vars.PlayerSelectCurseTechnique);
        // Shared input for Uro's Blind Rage and Mahito's Soul Grasp.
        return (technique == 38 && selected == 9)
                || (technique == 15 && selected == 11);
    }
}
