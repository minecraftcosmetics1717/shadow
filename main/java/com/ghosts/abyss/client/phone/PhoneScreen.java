package com.ghosts.abyss.client.phone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class PhoneScreen extends Screen {

    private static final ResourceLocation PHONE_BG =
            new ResourceLocation("abyss", "textures/gui/phone/phone_bg.png");
    private static final ResourceLocation ICON_MESSAGES =
            new ResourceLocation("abyss", "textures/gui/phone/icon_messages.png");
    private static final ResourceLocation ICON_MAP =
            new ResourceLocation("abyss", "textures/gui/phone/icon_map.png");
    private static final ResourceLocation ICON_TELEPORT =
            new ResourceLocation("abyss", "textures/gui/phone/icon_teleport.png");

    // Match your texture size (change if your PNG is different)
    private static final int PHONE_W = 140;
    private static final int PHONE_H = 240;

    private static final int ICON_SIZE = 24;
    private static final int ICON_GAP = 16;

    private int left, top;

    public PhoneScreen() {
        super(Component.literal("Phone"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // Phone background texture
        g.blit(PHONE_BG, left, top, 0, 0, PHONE_W, PHONE_H, PHONE_W, PHONE_H);

        // In-game time (top left)
        String time = getInGameTimeString();
        g.drawString(font, time, left + 8, top + 10, 0xFFE6EDF3, false);

        // Icon positions
        int iconsY = top + 54;
        int totalIconsW = 3 * ICON_SIZE + 2 * ICON_GAP;
        int iconsStartX = left + (PHONE_W - totalIconsW) / 2;

        int msgX = iconsStartX;
        int mapX = iconsStartX + ICON_SIZE + ICON_GAP;
        int tpX  = iconsStartX + 2 * (ICON_SIZE + ICON_GAP);

        // Draw icons (with slight brighten on hover)
        drawIcon(g, ICON_MESSAGES, msgX, iconsY, mouseX, mouseY);
        drawIcon(g, ICON_MAP,      mapX, iconsY, mouseX, mouseY);
        drawIcon(g, ICON_TELEPORT, tpX,  iconsY, mouseX, mouseY);

// Labels (slightly smaller so they stay inside the phone)
        var pose = g.pose();
        pose.pushPose();
        pose.scale(0.75f, 0.75f, 1f);   // 75% size

        int labelY = (int) ((iconsY + ICON_SIZE + 5) / 0.75f);
        g.drawCenteredString(font, "Messages", (int) ((msgX + ICON_SIZE / 2) / 0.75f), labelY, 0xFF8B949E);
        g.drawCenteredString(font, "Map",      (int) ((mapX + ICON_SIZE / 2) / 0.75f), labelY, 0xFF8B949E);
        g.drawCenteredString(font, "Teleport", (int) ((tpX  + ICON_SIZE / 2) / 0.75f), labelY, 0xFF8B949E);

        pose.popPose();
    }

    private void drawIcon(GuiGraphics g, ResourceLocation tex, int x, int y, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
        if (hover) {
            // simple hover highlight behind icon
            g.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, 0x44FFFFFF);
        }
        g.blit(tex, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    /** Converts Minecraft day time into HH:mm */
    private String getInGameTimeString() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "00:00";

        long dayTime = mc.level.getDayTime() % 24000L;
        // Minecraft day starts at 6:00
        int totalMinutes = (int) ((dayTime * 60 * 24) / 24000L);
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int iconsY = top + 48;
        int totalIconsW = 3 * ICON_SIZE + 2 * ICON_GAP;
        int iconsStartX = left + (PHONE_W - totalIconsW) / 2;

        // Messages
        if (isInside(mouseX, mouseY, iconsStartX, iconsY)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen("Messages", "Quests & messages coming soon."));
            return true;
        }
        // Map
        if (isInside(mouseX, mouseY, iconsStartX + ICON_SIZE + ICON_GAP, iconsY)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen("Map", "Quest markers & structures coming soon."));
            return true;
        }
        // Teleport → another phone-style screen
        if (isInside(mouseX, mouseY, iconsStartX + 2 * (ICON_SIZE + ICON_GAP), iconsY)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + ICON_SIZE && my >= y && my < y + ICON_SIZE;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.ghosts.abyss.client.ClientEvents.OPEN_PHONE.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}