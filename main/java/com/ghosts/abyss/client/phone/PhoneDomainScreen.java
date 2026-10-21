package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.RefinementPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketRequestUnlockHWB;
import com.ghosts.abyss.network.PacketSpendRefinementPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class PhoneDomainScreen extends Screen {

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


    // Colors matching your screenshot
    private static final int COL_TITLE = 0xFFE6EDF3;
    private static final int COL_CARD_BG = 0xFF020B0F;
    private static final int COL_CARD_BORDER = 0xFF207EAA; // cyan/blue
    private static final int COL_LABEL = 0xFF8B949E;
    private static final int COL_VALUE = 0xFFE6EDF3;
    private static final int COL_POINTS = 0xFF58A6FF;
    private static final int COL_BAR_BG = 0xFF21262D;
    private static final int COL_BAR_FILL = 0xFF1F6FEB;
    private static final int COL_BTN = 0xFF1F6FEB;
    private static final int COL_BTN_HOVER = 0xFF2A9FD4;
    private static final int COL_BTN_DISABLED = 0xFF30363D;
    private static final int COL_STATUS = 0xFF58A6FF;
    private static final ResourceLocation ICON_GLOBE =
            new ResourceLocation("abyss", "textures/gui/phone/globe.png");
    private int left, top;

    // Clickable button bounds (set during render)
    private int spendX, spendY, spendW, spendH;
    private int hwbX, hwbY, hwbW, hwbH;
    private boolean spendActive, hwbActive;

    public PhoneDomainScreen() {
        super(Component.literal("Domain"));
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

        // Time
        g.drawString(font, getInGameTimeString(), left + 8, top + 10, COL_TITLE, false);

        // Title
        g.drawCenteredString(font, "Domain", left + PHONE_W / 2, top + 28, COL_TITLE);

        Player player = Minecraft.getInstance().player;
        int available = player != null ? RefinementPoints.getAvailable(player) : 0;
        int refinement = player != null ? RefinementPoints.getRefinement(player) : 0;
        int atAvailable = player != null ? AdvTechPoints.getAvailable(player) : 0;
        boolean hwbUnlocked = player != null && player.getPersistentData().getBoolean("unlocked_hwb");
        boolean hwbWitnessed = player != null && player.getPersistentData().getBoolean("witnessed_hwb");
        boolean refinementEnabled = AbyssGamerules.isClientDomainRefinementEnabled();
        boolean hwbEnabled = AbyssGamerules.isClientHollowWickerBasketEnabled();

        spendActive = refinementEnabled && available > 0 && refinement < 50;
        hwbActive = hwbEnabled && !hwbUnlocked && atAvailable > 0 && hwbWitnessed;

// ===== Card 1: Refinement Level =====
        int card1X = left + 10;
        int card1Y = top + 44;
        int card1W = PHONE_W - 20;
        int card1H = 78;
        drawCard(g, card1X, card1Y, card1W, card1H);

        g.drawCenteredString(font, "Refinement Level", left + PHONE_W / 2, card1Y + 8, COL_TITLE);

// 0/50 under title
        String refStr = refinement + "/50";
        g.drawCenteredString(font, refStr, left + PHONE_W / 2, card1Y + 20, COL_LABEL);

// Centered bar
        int barW = card1W - 28;
        int barH = 8;
        int barX = card1X + (card1W - barW) / 2;
        int barY = card1Y + 36;

        g.fill(barX, barY, barX + barW, barY + barH, COL_BAR_BG);
        int fillW = (int) ((float) Math.min(refinement, 50) / 50f * barW);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, COL_BAR_FILL);
        }

// Available points — top-right of the bar
        String pts = String.valueOf(Math.min(available, 99));
        int ptsW = font.width(pts);
        g.drawString(font, pts, barX + barW - ptsW, barY - 10, COL_POINTS, false);

// Spend point button
        spendW = 70;
        spendH = 14;
        spendX = left + (PHONE_W - spendW) / 2;
        spendY = card1Y + card1H - 20;
        boolean spendHover = isInside(mouseX, mouseY, spendX, spendY, spendW, spendH);
        int spendColor = !spendActive ? COL_BTN_DISABLED : (spendHover ? COL_BTN_HOVER : COL_BTN);
        g.fill(spendX, spendY, spendX + spendW, spendY + spendH, spendColor);
// light highlight border on hover
        if (spendActive && spendHover) {
            g.fill(spendX - 1, spendY - 1, spendX + spendW + 1, spendY, 0x66FFFFFF);
            g.fill(spendX - 1, spendY + spendH, spendX + spendW + 1, spendY + spendH + 1, 0x66FFFFFF);
            g.fill(spendX - 1, spendY, spendX, spendY + spendH, 0x66FFFFFF);
            g.fill(spendX + spendW, spendY, spendX + spendW + 1, spendY + spendH, 0x66FFFFFF);
        }
        g.drawCenteredString(font, "Spend point", spendX + spendW / 2, spendY + 3, 0xFFFFFFFF);

// ===== Card 2: Hollow Wicker Basket =====
        int card2X = left + 10;
        int card2Y = card1Y + card1H + 8;
        int card2W = PHONE_W - 20;
        int card2H = 72;
        drawCard(g, card2X, card2Y, card2W, card2H);

// Globe texture + slightly smaller title
        int globeSize = 10;
        g.blit(ICON_GLOBE, card2X + 8, card2Y + 8, 0, 0, globeSize, globeSize, globeSize, globeSize);

        var pose = g.pose();
        pose.pushPose();
        pose.scale(0.85f, 0.85f, 1f);
        g.drawString(font, "Hollow Wicker Basket",
                (int) ((card2X + 22) / 0.85f),
                (int) ((card2Y + 9) / 0.85f),
                COL_TITLE, false);
        pose.popPose();

// Status
        String status;
        if (hwbUnlocked || hwbWitnessed) {
            status = "Witnessed";
        } else {
            status = "Not yet witnessed";
        }
        g.drawCenteredString(font, status, left + PHONE_W / 2, card2Y + 26, COL_STATUS);

// HWB button
        hwbW = 50;
        hwbH = 14;
        hwbX = left + (PHONE_W - hwbW) / 2;
        hwbY = card2Y + card2H - 20;
        boolean hwbHover = isInside(mouseX, mouseY, hwbX, hwbY, hwbW, hwbH);
        int hwbColor = !hwbActive ? COL_BTN_DISABLED : (hwbHover ? COL_BTN_HOVER : COL_BTN);
        g.fill(hwbX, hwbY, hwbX + hwbW, hwbY + hwbH, hwbColor);
        if (hwbActive && hwbHover) {
            g.fill(hwbX - 1, hwbY - 1, hwbX + hwbW + 1, hwbY, 0x66FFFFFF);
            g.fill(hwbX - 1, hwbY + hwbH, hwbX + hwbW + 1, hwbY + hwbH + 1, 0x66FFFFFF);
            g.fill(hwbX - 1, hwbY, hwbX, hwbY + hwbH, 0x66FFFFFF);
            g.fill(hwbX + hwbW, hwbY, hwbX + hwbW + 1, hwbY + hwbH, 0x66FFFFFF);
        }
        String hwbLabel;
        if (hwbUnlocked) {
            hwbLabel = "Unlocked";
        } else {
            hwbLabel = Math.min(atAvailable, 99) + "/5";
        }
        g.drawCenteredString(font, hwbLabel, hwbX + hwbW / 2, hwbY + 3, 0xFFFFFFFF);

        // ===== Dock =====
        renderDock(g, mouseX, mouseY);
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COL_CARD_BG);
        // border
        g.fill(x, y, x + w, y + 1, COL_CARD_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, COL_CARD_BORDER);
        g.fill(x, y, x + 1, y + h, COL_CARD_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, COL_CARD_BORDER);
    }

    private void renderDock(GuiGraphics g, int mouseX, int mouseY) {
        int dockY = top + PHONE_H - 31;
        int dockTotalW = 3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;
        int dockStartX = left + (PHONE_W - dockTotalW) / 2;

        drawDockIcon(g, ICON_MESSAGES, dockStartX, dockY, mouseX, mouseY);
        drawDockIcon(g, ICON_MAP, dockStartX + DOCK_ICON_SIZE + DOCK_GAP, dockY, mouseX, mouseY);
        drawDockIcon(g, ICON_TELEPORT, dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP), dockY, mouseX, mouseY);
    }

    private void drawDockIcon(GuiGraphics g, ResourceLocation tex, int x, int y, int mouseX, int mouseY) {
        boolean hover = isInside(mouseX, mouseY, x, y, DOCK_ICON_SIZE, DOCK_ICON_SIZE);
        if (hover) {
            g.fill(x - 2, y - 2, x + DOCK_ICON_SIZE + 2, y + DOCK_ICON_SIZE + 2, 0x44FFFFFF);
        }
        g.blit(tex, x, y, 0, 0, DOCK_ICON_SIZE, DOCK_ICON_SIZE, DOCK_ICON_SIZE, DOCK_ICON_SIZE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Spend point
        if (spendActive && isInside(mouseX, mouseY, spendX, spendY, spendW, spendH)) {
            ModNetwork.CHANNEL.sendToServer(new PacketSpendRefinementPoint());
            return true;
        }

        // HWB unlock
        if (hwbActive && isInside(mouseX, mouseY, hwbX, hwbY, hwbW, hwbH)) {
            ModNetwork.CHANNEL.sendToServer(new PacketRequestUnlockHWB());
            return true;
        }

        // Dock
        int dockY = top + PHONE_H - 30;
        int dockTotalW = 3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;
        int dockStartX = left + (PHONE_W - dockTotalW) / 2;
        int msgX = dockStartX;
        int mapX = dockStartX + DOCK_ICON_SIZE + DOCK_GAP;
        int tpX  = dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP);

        if (isInside(mouseX, mouseY, msgX, dockY, DOCK_ICON_SIZE, DOCK_ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen("Messages", "Quests & messages coming soon."));
            return true;
        }
        if (isInside(mouseX, mouseY, mapX, dockY, DOCK_ICON_SIZE, DOCK_ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen("Map", "Quest markers & structures coming soon."));
            return true;
        }
        if (isInside(mouseX, mouseY, tpX, dockY, DOCK_ICON_SIZE, DOCK_ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Esc, phone key, or E → back to main phone
        if (keyCode == 256 /* Esc */
                || keyCode == 69 /* E */
                || com.ghosts.abyss.client.ClientEvents.OPEN_PHONE.matches(keyCode, scanCode)) {
            Minecraft.getInstance().setScreen(new PhoneScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}