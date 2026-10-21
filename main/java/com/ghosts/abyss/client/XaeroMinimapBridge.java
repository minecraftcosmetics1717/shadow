package com.ghosts.abyss.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class XaeroMinimapBridge {
    public static boolean clientUnlocked = false;
    public static boolean clientEnabled = false;

    /** Xaero listens for these special strings in chat/system messages. */
    private static final String DISABLE = "§n§o§m§i§n§i§m§a§p";
    private static final String RESET   = "§r§e§s§e§t§x§a§e§r§o";

    public static void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!clientUnlocked || !clientEnabled) {
            // Force minimap off
            mc.player.displayClientMessage(Component.literal(DISABLE), false);
        } else {
            // Clear forced-off flag so normal minimap can show
            mc.player.displayClientMessage(Component.literal(RESET), false);
        }
    }

    public static void forceOffIfLocked() {
        if (!clientUnlocked || !clientEnabled) {
            apply();
        }
    }
}