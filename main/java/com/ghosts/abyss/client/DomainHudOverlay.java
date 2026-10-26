package com.ghosts.abyss.client;

import com.ghosts.abyss.network.PacketSyncDomainHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "abyss", value = Dist.CLIENT)
public class DomainHudOverlay {

    private static boolean inSession;
    private static boolean yourDomain;
    private static boolean clashing;
    private static String ownerName = "";
    private static List<String> clashers = List.of();
    private static long lastUpdateMs;

    public static void update(PacketSyncDomainHud pkt) {
        inSession = pkt.inDomainSession;
        yourDomain = pkt.yourDomainActive;
        clashing = pkt.clashing;
        ownerName = pkt.ownerName;
        clashers = new ArrayList<>(pkt.clasherNames);
        lastUpdateMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        // Hide if stale (left domain / no packets)
        if (!inSession || System.currentTimeMillis() - lastUpdateMs > 3000) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics g = event.getGuiGraphics();
        int x = 6;
        int y = event.getWindow().getGuiScaledHeight() - 60;
        int color = 0xFFE0E0E0;
        int accent = clashing ? 0xFFFF6666 : 0xFF66FFAA;

        g.drawString(mc.font, "Domain session", x, y, accent, true);
        y += 10;
        g.drawString(mc.font, "Owner: " + (ownerName.isEmpty() ? "?" : ownerName), x, y, color, true);
        y += 10;
        g.drawString(mc.font, "You: " + (yourDomain ? "ACTIVE" : "not expanding"), x, y,
                yourDomain ? 0xFF66FF66 : 0xFFAAAAAA, true);
        y += 10;
        if (clashing && !clashers.isEmpty()) {
            g.drawString(mc.font, "Clashing: " + String.join(", ", clashers), x, y, 0xFFFF8888, true);
        } else if (yourDomain) {
            g.drawString(mc.font, "No clash", x, y, 0xFF888888, true);
        }
    }
}