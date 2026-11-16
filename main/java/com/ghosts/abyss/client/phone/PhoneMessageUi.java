package com.ghosts.abyss.client.phone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class PhoneMessageUi {
    static final int W = 140, H = 240;
    static final int WHITE = 0xFFE6EDF3;
    static final int MUTED = 0xFF9CA0AA;
    static final int BLUE = 0xFF3189FF;
    static final ResourceLocation BG = new ResourceLocation("abyss", "textures/gui/phone/phone_bg.png");
    private static final ResourceLocation MESSAGES = new ResourceLocation("abyss", "textures/gui/phone/icon_messages.png");
    private static final ResourceLocation MAP = new ResourceLocation("abyss", "textures/gui/phone/icon_map.png");
    private static final ResourceLocation TELEPORT = new ResourceLocation("abyss", "textures/gui/phone/icon_teleport.png");

    static void base(GuiGraphics g, int left, int top, String title) {
        g.blit(BG, left, top, 0, 0, W, H, W, H);
        Minecraft mc = Minecraft.getInstance();
        long day = mc.level == null ? 0 : mc.level.getDayTime() % 24000L;
        int mins = (int) (day * 1440L / 24000L);
        g.drawString(mc.font, String.format("%02d:%02d", (mins / 60 + 6) % 24, mins % 60),
                left + 8, top + 10, WHITE, false);
        g.drawCenteredString(mc.font, title, left + W / 2, top + 28, WHITE);
        signal(g, left, top);
    }

    static void signal(GuiGraphics g, int left, int top) {
        boolean blocked = PhoneMessageClient.blocked();
        int color = blocked ? 0xFFFF2020 : 0xFFFFFFFF;
        int x = left + W - 23, y = top + 9;
        if (blocked) {
            for (int i = 0; i < 3; i++)
                g.fill(x + i * 5, y + 5, x + i * 5 + 4, y + 7, color);
        } else {
            for (int i = 0; i < 3; i++) {
                int h = 3 + i * 3;
                g.fill(x + i * 5, y + 9 - h, x + i * 5 + 4, y + 9, color);
            }
        }
    }

    static void dock(GuiGraphics g, int left, int top) {
        int y = top + H - 31;
        int x = left + (W - (3 * 24 + 2 * 8)) / 2;
        g.blit(MESSAGES, x, y, 0, 0, 24, 24, 24, 24);
        g.blit(MAP, x + 32, y, 0, 0, 24, 24, 24, 24);
        g.blit(TELEPORT, x + 64, y, 0, 0, 24, 24, 24, 24);
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
