package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.RctPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketPurchaseRctPerk;
import com.ghosts.abyss.network.PacketPurchaseRctPerk.Perk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class PhoneRctScreen extends Screen {

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

    // Green theme
    private static final int COL_TITLE = 0xFFE6EDF3;
    private static final int COL_CARD_BG = 0xFF020F05;
    private static final int COL_CARD_BORDER = 0xFF20AA42;
    private static final int COL_HEADER = 0xFF20AA42;
    private static final int COL_LABEL = 0xFFE6EDF3;
    private static final int COL_SUB = 0xFF8B949E;
    private static final int COL_SEG_ON = 0xFF00FF45;
    private static final int COL_SEG_OFF = 0xFF0A2A12;
    private static final int COL_BTN = 0xFF00FF45;
    private static final int COL_BTN_HOVER = 0xFF5CFF7A;
    private static final int COL_BTN_DISABLED = 0xFF30363D;
    private static final int COL_EXP = 0xFF009E27;
    private static final int COL_BTN_TEXT_DARK = 0xFF02200A;

    private int left, top;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int contentTop, contentBottom, contentH;

    private final List<Btn> buttons = new ArrayList<>();

    public PhoneRctScreen() {
        super(Component.literal("Reverse Cursed Technique"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;
        contentTop = top + 42;
        contentBottom = top + PHONE_H - 36;
        contentH = contentBottom - contentTop;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.blit(PHONE_BG, left, top, 0, 0, PHONE_W, PHONE_H, PHONE_W, PHONE_H);

// Fixed header
        g.drawString(font, getInGameTimeString(), left + 8, top + 10, COL_TITLE, false);

        var pose = g.pose();
        pose.pushPose();
        pose.scale(0.85f, 0.85f, 1f);
        g.drawCenteredString(font, "Reverse Cursed Technique",
                (int) ((left + PHONE_W / 2f) / 0.85f),
                (int) ((top + 28) / 0.85f),
                COL_TITLE);
        pose.popPose();

        buttons.clear();

        if (!AbyssGamerules.isClientReverseCursedTechniqueEnabled()) {
            g.drawCenteredString(font, "Disabled by server", left + PHONE_W / 2, top + 100, COL_SUB);
            renderDock(g, mouseX, mouseY);
            return;
        }

        int exp = RctPoints.getClientExp();
        int efficiency = RctPoints.getClientEfficiencyTier();
        int proficiency = RctPoints.getClientProficiencyTier();
        boolean outputUnlocked = RctPoints.isClientOutputUnlocked();

        g.enableScissor(left + 4, contentTop, left + PHONE_W - 4, contentBottom);

        int cx = left + PHONE_W / 2;
        int cy = contentTop + 4 - scrollOffset;
        int cardW = PHONE_W - 20;
        int cardX = left + 10;

        // RCT Experience (green)
        g.drawCenteredString(font, "RCT Experience: " + exp, cx, cy, COL_EXP);
        cy += 14;

        // RCT Efficiency (5 tiers)
        cy = drawPerkCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                "RCT Efficiency",
                "Each tier raises the output you are healed",
                efficiency, 5,
                efficiency < 5 && exp >= 5,
                efficiency < 5 ? "Spend point" : "Maxed",
                Perk.EFFICIENCY);

        // RCT Proficiency (4 tiers)
        cy = drawPerkCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                "RCT Proficiency",
                "Delays the fatigue you suffer from after using RCT",
                proficiency, 4,
                proficiency < 4 && exp >= 5,
                proficiency < 4 ? "Spend point" : "Maxed",
                Perk.PROFICIENCY);

        // RCT Output (one-time unlock)
        cy = drawOutputCard(g, mouseX, mouseY, cardX, cy, cardW, cx, exp, outputUnlocked);

        contentHeight = (cy + scrollOffset) - contentTop + 8;
        g.disableScissor();

        renderDock(g, mouseX, mouseY);
    }

    private int drawPerkCard(GuiGraphics g, int mx, int my,
                             int x, int y, int w, int cx,
                             String title, String desc,
                             int tier, int maxTier,
                             boolean canBuy, String btnLabel, Perk perk) {
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 16);
        int boxH = 56 + lines.size() * 9;

        drawCard(g, x, y, w, boxH);
        g.drawCenteredString(font, title, cx, y + 6, COL_HEADER);

        int dy = y + 18;
        for (FormattedCharSequence line : lines) {
            g.drawCenteredString(font, line, cx, dy, COL_LABEL);
            dy += 9;
        }

        g.drawCenteredString(font, tier + "/" + maxTier, cx, dy + 2, COL_SUB);
        drawSegments(g, cx, dy + 12, tier, maxTier);

        int btnW = 70, btnH = 14;
        int btnX = cx - btnW / 2;
        int btnY = y + boxH - 18;
        boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);
        int col = !canBuy ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN);
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
        if (canBuy && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);

        // Bright green button → dark text for readability
        int textCol = canBuy ? COL_BTN_TEXT_DARK : 0xFFAAAAAA;
        g.drawCenteredString(font, btnLabel, btnX + btnW / 2, btnY + 3, textCol);

        if (canBuy && btnY + btnH > contentTop && btnY < contentBottom) {
            buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                    ModNetwork.CHANNEL.sendToServer(new PacketPurchaseRctPerk(perk))));
        }

        return y + boxH + 6;
    }

    private int drawOutputCard(GuiGraphics g, int mx, int my,
                               int x, int y, int w, int cx,
                               int exp, boolean unlocked) {
        String desc = "Complete mastery over RCT, allowing the user to manipulate the flow of their cursed energy";
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 16);
        int boxH = 48 + lines.size() * 9;

        drawCard(g, x, y, w, boxH);
        g.drawCenteredString(font, "RCT Output", cx, y + 6, COL_HEADER);

        int dy = y + 18;
        for (FormattedCharSequence line : lines) {
            g.drawCenteredString(font, line, cx, dy, COL_LABEL);
            dy += 9;
        }

        boolean canUnlock = !unlocked && exp >= 20;
        int btnW = 90, btnH = 14;
        int btnX = cx - btnW / 2;
        int btnY = y + boxH - 18;
        boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);
        int col = (!canUnlock && !unlocked) ? COL_BTN_DISABLED : (unlocked ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN));
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
        if (canUnlock && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);

        String label = unlocked ? "Unlocked" : "Unlock (20 exp)";
        int textCol = canUnlock ? COL_BTN_TEXT_DARK : 0xFFAAAAAA;
        g.drawCenteredString(font, label, btnX + btnW / 2, btnY + 3, textCol);

        if (canUnlock && btnY + btnH > contentTop && btnY < contentBottom) {
            buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                    ModNetwork.CHANNEL.sendToServer(new PacketPurchaseRctPerk(Perk.OUTPUT))));
        }

        return y + boxH + 6;
    }

    private void drawSegments(GuiGraphics g, int cx, int y, int current, int max) {
        int segW = 12, gap = 3;
        int totalW = max * segW + (max - 1) * gap;
        int startX = cx - totalW / 2;
        for (int i = 0; i < max; i++) {
            int sx = startX + i * (segW + gap);
            g.fill(sx, y, sx + segW, y + 5, i < current ? COL_SEG_ON : COL_SEG_OFF);
        }
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COL_CARD_BG);
        g.fill(x, y, x + w, y + 1, COL_CARD_BORDER);
        g.fill(x, y + h - 1, x + w, y + h, COL_CARD_BORDER);
        g.fill(x, y, x + 1, y + h, COL_CARD_BORDER);
        g.fill(x + w - 1, y, x + w, y + h, COL_CARD_BORDER);
    }

    private void drawHoverEdge(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y, 0x66FFFFFF);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0x66FFFFFF);
        g.fill(x - 1, y, x, y + h, 0x66FFFFFF);
        g.fill(x + w, y, x + w + 1, y + h, 0x66FFFFFF);
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
        if (isInside(mx, my, x, y, DOCK_ICON_SIZE, DOCK_ICON_SIZE)) {
            g.fill(x - 2, y - 2, x + DOCK_ICON_SIZE + 2, y + DOCK_ICON_SIZE + 2, 0x44FFFFFF);
        }
        g.blit(tex, x, y, 0, 0, DOCK_ICON_SIZE, DOCK_ICON_SIZE, DOCK_ICON_SIZE, DOCK_ICON_SIZE);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int maxScroll = Math.max(0, contentHeight - contentH);
        if (maxScroll <= 0) return false;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 12));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        for (Btn b : buttons) {
            if (isInside(mouseX, mouseY, b.x, b.y, b.w, b.h)) {
                b.action.run();
                return true;
            }
        }

        int dockY = top + PHONE_H - 31;
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69
                || com.ghosts.abyss.client.ClientEvents.OPEN_PHONE.matches(keyCode, scanCode)) {
            Minecraft.getInstance().setScreen(new PhoneScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

    private record Btn(int x, int y, int w, int h, Runnable action) {}
}