package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.BlackFlashPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketActivateAtWillBlackFlash;
import com.ghosts.abyss.network.PacketPurchaseBlackFlashPerk;
import com.ghosts.abyss.network.PacketPurchaseBlackFlashPerk.Perk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class PhoneBlackFlashScreen extends Screen {

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

    // Red theme
    private static final int COL_TITLE = 0xFFE6EDF3;
    private static final int COL_CARD_BG = 0xFF0A0000;
    private static final int COL_CARD_BORDER = 0xFFAA2020;
    private static final int COL_HEADER = 0xFFFF4444;
    private static final int COL_LABEL = 0xFFE6EDF3;
    private static final int COL_SUB = 0xFF8B949E;
    private static final int COL_SEG_ON = 0xFFE63939;
    private static final int COL_SEG_OFF = 0xFF3D1515;
    private static final int COL_BTN = 0xFFAA2020;
    private static final int COL_BTN_HOVER = 0xFFD32F2F;
    private static final int COL_BTN_DISABLED = 0xFF30363D;
    private static final int COL_REQ_MET = 0xFF3FB950;
    private static final int COL_REQ_UNMET = 0xFFFF6B6B;
    private static final int COL_ORANGE = 0xFFE67E22;

    private int left, top;
    private int scrollOffset = 0;
    private int contentHeight = 0;

    // Content area (between title and dock) — fixed, does not scroll
    private int contentTop;
    private int contentBottom;
    private int contentH;

    // Click targets (screen-space, updated each frame)
    private final List<Btn> buttons = new ArrayList<>();

    public PhoneBlackFlashScreen() {
        super(Component.literal("Black Flash"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;
        contentTop = top + 42;
        contentBottom = top + PHONE_H - 36; // above dock
        contentH = contentBottom - contentTop;
        scrollOffset = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.blit(PHONE_BG, left, top, 0, 0, PHONE_W, PHONE_H, PHONE_W, PHONE_H);

        // Fixed: time + title
        g.drawString(font, getInGameTimeString(), left + 8, top + 10, COL_TITLE, false);
        g.drawCenteredString(font, "Black Flash", left + PHONE_W / 2, top + 28, COL_TITLE);

        buttons.clear();

        if (!AbyssGamerules.isClientBlackFlashEnabled()) {
            g.drawCenteredString(font, "Disabled by server", left + PHONE_W / 2, top + 100, COL_SUB);
            renderDock(g, mouseX, mouseY);
            return;
        }

        int exp = BlackFlashPoints.getClientExp();
        int chance = BlackFlashPoints.getClientChanceTier();
        int fatigue = BlackFlashPoints.getClientFatigueTier();
        int energy = BlackFlashPoints.getClientEnergyTier();
        int brain = BlackFlashPoints.getClientBrainTier();
        boolean atWillUnlocked = BlackFlashPoints.isClientAtWillUnlocked();
        boolean atWillCharged = BlackFlashPoints.isClientAtWillCharged();
        long cooldownEnd = BlackFlashPoints.getClientAtWillCooldownEnd();
        long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        boolean onCooldown = !atWillCharged && gameTime < cooldownEnd;
        long cooldownLeft = Math.max(0L, (cooldownEnd - gameTime) / 20L);

        // Scrollable content
        g.enableScissor(left + 4, contentTop, left + PHONE_W - 4, contentBottom);

        int cx = left + PHONE_W / 2;
        int cy = contentTop + 4 - scrollOffset;
        int cardW = PHONE_W - 20;
        int cardX = left + 10;

        // Optional: show exp at top of scroll area
        g.drawCenteredString(font, "BF Exp: " + exp, cx, cy, COL_ORANGE);
        cy += 14;

        // --- Chance (max 5) ---
        cy = drawPerkCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                "Chance",
                "Each tier grants an extra roll to hit a black flash",
                chance, 5,
                chance < 5 && exp >= 5,
                chance < 5 ? "Spend point" : "Maxed",
                Perk.CHANCE);

        // --- Fatigue (max 4) ---
        cy = drawPerkCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                "Fatigue Reduction",
                "Each tier deepens restoration upon black flashing",
                fatigue, 4,
                fatigue < 4 && exp >= 5,
                fatigue < 4 ? "Spend point" : "Maxed",
                Perk.FATIGUE);

        // --- Cursed Energy (max 3) ---
        cy = drawPerkCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                "Cursed Energy",
                "Gain cursed energy back from black flashing",
                energy, 3,
                energy < 3 && exp >= 5,
                energy < 3 ? "Spend point" : "Maxed",
                Perk.ENERGY);

        // --- Brain Damage Recovery (max 3) ---
        cy = drawPerkCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                "Brain Damage Recovery",
                "Accelerate recovery from brain damage by hitting a black flash",
                brain, 3,
                brain < 3 && exp >= 5,
                brain < 3 ? "Spend point" : "Maxed",
                Perk.BRAIN);

        // --- At-Will Black Flash ---
        cy = drawAtWillCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                exp, chance >= 5, atWillUnlocked, atWillCharged, onCooldown, cooldownLeft);

        contentHeight = (cy + scrollOffset) - contentTop + 8;
        g.disableScissor();

        // Fixed dock (never scrolls)
        renderDock(g, mouseX, mouseY);
    }

    /** Returns Y just below the card. */
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

        // tier text + segments
        g.drawCenteredString(font, tier + "/" + maxTier, cx, dy + 2, COL_SUB);
        drawSegments(g, cx, dy + 12, tier, maxTier);

        // button
        int btnW = 70, btnH = 14;
        int btnX = cx - btnW / 2;
        int btnY = y + boxH - 18;
        boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);
        int col = !canBuy ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN);
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
        if (canBuy && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
        g.drawCenteredString(font, btnLabel, btnX + btnW / 2, btnY + 3, 0xFFFFFFFF);

        // only clickable if visible in content area
        if (canBuy && btnY + btnH > contentTop && btnY < contentBottom) {
            buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                    ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(perk))));
        }

        return y + boxH + 6;
    }

    private int drawAtWillCard(GuiGraphics g, int mx, int my,
                               int x, int y, int w, int cx,
                               int exp, boolean chanceMaxed,
                               boolean unlocked, boolean charged,
                               boolean onCooldown, long cooldownLeft) {
        String desc = "The pinnacle of black flashes. Put your full focus onto getting a definite black flash";
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 16);
        int boxH = 62 + lines.size() * 9 + (unlocked ? 10 : 12);

        drawCard(g, x, y, w, boxH);
        g.drawCenteredString(font, "At-Will Black Flash", cx, y + 6, COL_HEADER);

        int dy = y + 18;
        for (FormattedCharSequence line : lines) {
            g.drawCenteredString(font, line, cx, dy, COL_LABEL);
            dy += 9;
        }

        if (!unlocked) {
            // Requirement: Max Black Flash Chance tier
            int reqColor = chanceMaxed ? COL_REQ_MET : COL_REQ_UNMET;
            String mark = chanceMaxed ? "✔" : "✘";
            g.drawCenteredString(font, mark + " Max Black Flash Chance tier", cx, dy + 4, reqColor);
            dy += 14;

            boolean canUnlock = chanceMaxed && exp >= 20;
            int btnW = 90, btnH = 14;
            int btnX = cx - btnW / 2;
            int btnY = y + boxH - 18;
            boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);
            int col = !canUnlock ? COL_BTN_DISABLED : (hover ? COL_ORANGE : 0xFFC47820);
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (canUnlock && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
            g.drawCenteredString(font, "Unlock (20 exp)", btnX + btnW / 2, btnY + 3, 0xFFFFFFFF);

            if (canUnlock && btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(Perk.AT_WILL))));
            }
        } else {
            String status = charged ? "§aCharged" : (onCooldown ? "§7Cooldown: " + cooldownLeft + "s" : "§eReady");
            g.drawCenteredString(font, status, cx, dy + 4, COL_LABEL);

            boolean canCharge = !charged && !onCooldown;
            int btnW = 90, btnH = 14;
            int btnX = cx - btnW / 2;
            int btnY = y + boxH - 18;
            boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);
            int col = !canCharge ? COL_BTN_DISABLED : (hover ? COL_ORANGE : 0xFFC47820);
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (canCharge && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);

            String label = charged ? "Already Charged" : (onCooldown ? "On Cooldown" : "Charge At-Will");
            g.drawCenteredString(font, label, btnX + btnW / 2, btnY + 3, 0xFFFFFFFF);

            if (canCharge && btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketActivateAtWillBlackFlash())));
            }
        }

        return y + boxH + 6;
    }

    private void drawSegments(GuiGraphics g, int cx, int y, int current, int max) {
        int segW = 12;
        int gap = 3;
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