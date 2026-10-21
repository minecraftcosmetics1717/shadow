package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketUnlockMinimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class PhoneMapScreen extends Screen {

    private static final ResourceLocation PHONE_BG =
            new ResourceLocation("abyss", "textures/gui/phone/phone_bg.png");
    private static final ResourceLocation ICON_MESSAGES =
            new ResourceLocation("abyss", "textures/gui/phone/icon_messages.png");
    private static final ResourceLocation ICON_MAP =
            new ResourceLocation("abyss", "textures/gui/phone/icon_map.png");
    private static final ResourceLocation ICON_TELEPORT =
            new ResourceLocation("abyss", "textures/gui/phone/icon_teleport.png");

    private static final int PHONE_W = 140;
    private static final int PHONE_H = 240;
    private static final int DOCK_ICON_SIZE = 24;
    private static final int DOCK_GAP = 8;

    private static final int COL_TITLE = 0xFFE6EDF3;
    private static final int COL_CARD_BG = 0xFF0A0F02;
    private static final int COL_CARD_BORDER = 0xFF77AA20;
    private static final int COL_HEADER = 0xFFB8E05A;
    private static final int COL_DESC = 0xFFD0E8A0;
    private static final int COL_BTN = 0xFF649E00;
    private static final int COL_BTN_HOVER = 0xFF7FBF20;
    private static final int COL_BTN_DISABLED = 0xFF30363D;
    private static final int COL_BTN_TEXT = 0xFF0A1200;

    private int left, top;
    private int btnX, btnY, btnW, btnH;
    private boolean btnActive;

    public PhoneMapScreen() {
        super(Component.literal("Map"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.blit(PHONE_BG, left, top, 0, 0, PHONE_W, PHONE_H, PHONE_W, PHONE_H);

        g.drawString(font, getInGameTimeString(), left + 8, top + 10, COL_TITLE, false);
        g.drawCenteredString(font, "Map", left + PHONE_W / 2, top + 28, COL_TITLE);

        Player player = Minecraft.getInstance().player;
        int at = player != null ? AdvTechPoints.getAvailable(player) : 0;
        boolean unlocked = player != null && player.getPersistentData().getBoolean("minimap_unlocked");

        int cardX = left + 10;
        int cardY = top + 48;
        int cardW = PHONE_W - 20;
        int cardH = 90;

        // Card
        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, COL_CARD_BG);
        g.fill(cardX, cardY, cardX + cardW, cardY + 1, COL_CARD_BORDER);
        g.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, COL_CARD_BORDER);
        g.fill(cardX, cardY, cardX + 1, cardY + cardH, COL_CARD_BORDER);
        g.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH, COL_CARD_BORDER);

        g.drawCenteredString(font, "Minimap Upgrade", left + PHONE_W / 2, cardY + 10, COL_HEADER);
        g.drawCenteredString(font, "Have a minimap available to you", left + PHONE_W / 2, cardY + 26, COL_DESC);

        // Show progress toward 5 cost
        String prog = Math.min(at, 99) + "/5";
        g.drawCenteredString(font, prog, left + PHONE_W / 2, cardY + 42, COL_DESC);

        btnW = 70;
        btnH = 14;
        btnX = left + (PHONE_W - btnW) / 2;
        btnY = cardY + cardH - 22;
        btnActive = !unlocked && at >= 5;

        boolean hover = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int col = unlocked ? COL_BTN_DISABLED : (!btnActive ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN));
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
        if (btnActive && hover) {
            g.fill(btnX - 1, btnY - 1, btnX + btnW + 1, btnY, 0x66FFFFFF);
            g.fill(btnX - 1, btnY + btnH, btnX + btnW + 1, btnY + btnH + 1, 0x66FFFFFF);
            g.fill(btnX - 1, btnY, btnX, btnY + btnH, 0x66FFFFFF);
            g.fill(btnX + btnW, btnY, btnX + btnW + 1, btnY + btnH, 0x66FFFFFF);
        }
        String label = unlocked ? "Unlocked" : "Spend point";
        g.drawCenteredString(font, label, btnX + btnW / 2, btnY + 3,
                (btnActive || unlocked) ? COL_BTN_TEXT : 0xFFAAAAAA);

        renderDock(g, mouseX, mouseY);
    }

    private void renderDock(GuiGraphics g, int mouseX, int mouseY) {
        int dockY = top + PHONE_H - 31;
        int dockTotalW = 3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;
        int dockStartX = left + (PHONE_W - dockTotalW) / 2;
        drawDockIcon(g, ICON_MESSAGES, dockStartX, dockY, mouseX, mouseY);
        drawDockIcon(g, ICON_MAP, dockStartX + DOCK_ICON_SIZE + DOCK_GAP, dockY, mouseX, mouseY);
        drawDockIcon(g, ICON_TELEPORT, dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP), dockY, mouseX, mouseY);
    }

    private void drawDockIcon(GuiGraphics g, ResourceLocation tex, int x, int y, int mx, int my) {
        if (mx >= x && mx < x + DOCK_ICON_SIZE && my >= y && my < y + DOCK_ICON_SIZE) {
            g.fill(x - 2, y - 2, x + DOCK_ICON_SIZE + 2, y + DOCK_ICON_SIZE + 2, 0x44FFFFFF);
        }
        g.blit(tex, x, y, 0, 0, DOCK_ICON_SIZE, DOCK_ICON_SIZE, DOCK_ICON_SIZE, DOCK_ICON_SIZE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (btnActive && mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
            ModNetwork.CHANNEL.sendToServer(new PacketUnlockMinimap());
            return true;
        }

        int dockY = top + PHONE_H - 31;
        int dockTotalW = 3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;
        int dockStartX = left + (PHONE_W - dockTotalW) / 2;
        int msgX = dockStartX;
        int mapX = dockStartX + DOCK_ICON_SIZE + DOCK_GAP;
        int tpX  = dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP);

        if (isInside(mouseX, mouseY, msgX, dockY)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen("Messages", "Quests & messages coming soon."));
            return true;
        }
        if (isInside(mouseX, mouseY, mapX, dockY)) {
            // stay on map screen
            return true;
        }
        if (isInside(mouseX, mouseY, tpX, dockY)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInside(double mx, double my, int x, int y) {
        return mx >= x && mx < x + DOCK_ICON_SIZE && my >= y && my < y + DOCK_ICON_SIZE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69
                || com.ghosts.abyss.client.ClientEvents.OPEN_PHONE.matches(keyCode, scanCode)) {
            Minecraft.getInstance().setScreen(new PhoneScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getInGameTimeString() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "00:00";
        long dayTime = mc.level.getDayTime() % 24000L;
        int totalMinutes = (int) ((dayTime * 60 * 24) / 24000L);
        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}