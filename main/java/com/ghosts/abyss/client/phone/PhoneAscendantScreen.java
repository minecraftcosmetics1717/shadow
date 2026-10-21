package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.ModItems;
import com.ghosts.abyss.PerfectBodySync;
import com.ghosts.abyss.RefinementPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketPurchaseBindingVow;
import com.ghosts.abyss.network.PacketRecoverBindingVow;
import com.ghosts.abyss.network.PacketRequestUnlockOpenDomain;
import com.ghosts.abyss.network.PacketToggleFourArms;
import com.ghosts.abyss.network.PacketUnlockCursedObjectTransformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PhoneAscendantScreen extends Screen {

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

    // ---- size knobs (tweak these) ----
    private static final float DESC_SCALE = 0.80f;   // description text size
    private static final float COT_TITLE_SCALE = 0.80f; // "Cursed Object Transformation" title
    private static final float REQ_SCALE = 0.75f;    // Open Domain requirement lines
    private static final int OD_EXTRA_H = 96;        // Open Domain card height when locked (raise if still tight)

    // Gold theme
    private static final int COL_TITLE = 0xFFE6EDF3;
    private static final int COL_CARD_BG = 0xFF030303;
    private static final int COL_CARD_BORDER = 0xFFAA8520;
    private static final int COL_HEADER = 0xFFFFBB00;
    private static final int COL_DESC = 0xFFFFD256;
    private static final int COL_BTN = 0xFFE5BE3A;
    private static final int COL_BTN_HOVER = 0xFFFFD666;
    private static final int COL_BTN_DISABLED = 0xFF30363D;
    private static final int COL_ADV = 0xFFFFBB00;
    private static final int COL_REQ_MET = 0xFF3FB950;
    private static final int COL_REQ_UNMET = 0xFF8B949E;
    private static final int COL_BTN_TEXT_DARK = 0xFF1A1400;

    private int left, top;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int contentTop, contentBottom, contentH;

    private final List<Btn> buttons = new ArrayList<>();

    public PhoneAscendantScreen() {
        super(Component.literal("Ascendant"));
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

        g.drawString(font, getInGameTimeString(), left + 8, top + 10, COL_TITLE, false);
        g.drawCenteredString(font, "Ascendant", left + PHONE_W / 2, top + 28, COL_TITLE);

        buttons.clear();

        Player player = Minecraft.getInstance().player;
        int atAvailable = player != null ? AdvTechPoints.getAvailable(player) : 0;
        int rpAvailable = player != null ? RefinementPoints.getAvailable(player) : 0;
        int rpRefinement = player != null ? RefinementPoints.getRefinement(player) : 0;

        boolean perfectBody = player != null && player.getPersistentData().getBoolean("perfect_body");
        boolean vowPurchased = player != null && player.getPersistentData().getBoolean("binding_vow_purchased");
        boolean hasVow = player != null && player.getInventory().items.stream()
                .anyMatch(s -> s.getItem() == ModItems.BINDING_VOW.get());
        boolean hasGrade1 = hasGrade1Advancement(player);
        boolean armsVisible = player != null && Boolean.TRUE.equals(PerfectBodySync.MAP.get(player.getUUID()));

        boolean cotUnlocked = player != null && player.getPersistentData().getBoolean("cursed_object_transform_unlocked");
        boolean odUnlocked = player != null && player.getPersistentData().getBoolean("open_domain_unlocked");
        boolean odWitnessed = player != null && player.getPersistentData().getBoolean("witnessed_open_domain");
        boolean hasSpecial = hasSpecialGradeAdvancement(player);

        boolean pbEnabled = AbyssGamerules.isClientPerfectBodyEnabled();
        boolean cotEnabled = AbyssGamerules.isClientCursedObjectTransformEnabled();
        boolean odEnabled = AbyssGamerules.isClientOpenDomainEnabled();

        g.enableScissor(left + 4, contentTop, left + PHONE_W - 4, contentBottom);

        int cx = left + PHONE_W / 2;
        int cy = contentTop + 4 - scrollOffset;
        int cardW = PHONE_W - 20;
        int cardX = left + 10;

        g.drawCenteredString(font, "Adv Tech Points: " + Math.min(atAvailable, 99), cx, cy, COL_ADV);
        cy += 14;

        cy = drawPerfectBodyCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                perfectBody, hasVow, vowPurchased, hasGrade1, armsVisible,
                atAvailable, pbEnabled);

        cy = drawCotCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                cotUnlocked, atAvailable, cotEnabled);

        cy = drawOpenDomainCard(g, mouseX, mouseY, cardX, cy, cardW, cx,
                odUnlocked, odWitnessed, hasSpecial, rpRefinement, rpAvailable, atAvailable, odEnabled);

        contentHeight = (cy + scrollOffset) - contentTop + 8;
        g.disableScissor();

        renderDock(g, mouseX, mouseY);
    }

    private int drawPerfectBodyCard(GuiGraphics g, int mx, int my,
                                    int x, int y, int w, int cx,
                                    boolean perfectBody, boolean hasVow, boolean vowPurchased,
                                    boolean hasGrade1, boolean armsVisible,
                                    int atAvailable, boolean enabled) {
        String desc = "A binding vow made with a cursed spirit. Grant yourself four arms and two mouths, the perfect body for jujutsu.";
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 12);
        int boxH = 52 + (int) (lines.size() * 9 * DESC_SCALE);

        drawCard(g, x, y, w, boxH);
        g.drawCenteredString(font, "Perfect Body", cx, y + 6, COL_HEADER);

        int dy = drawScaledLines(g, lines, cx, y + 18, COL_DESC, DESC_SCALE);

        int btnW = 90, btnH = 14;
        int btnX = cx - btnW / 2;
        int btnY = y + boxH - 18;
        boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);

        if (perfectBody) {
            int col = hover ? COL_BTN_HOVER : COL_BTN;
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
            g.drawCenteredString(font, "Toggle visibility", btnX + btnW / 2, btnY + 3, COL_BTN_TEXT_DARK);
            if (btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketToggleFourArms())));
            }
        } else if (vowPurchased && !hasVow) {
            boolean can = enabled;
            int col = !can ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN);
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (can && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
            g.drawCenteredString(font, "Recover vow", btnX + btnW / 2, btnY + 3,
                    can ? COL_BTN_TEXT_DARK : 0xFFAAAAAA);
            if (can && btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketRecoverBindingVow())));
            }
        } else if (hasVow) {
            g.drawCenteredString(font, "Vow held — use it", cx, btnY + 3, COL_DESC);
        } else {
            boolean can = enabled && hasGrade1 && atAvailable >= 10;
            int col = !can ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN);
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (can && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
            g.drawCenteredString(font, "Purchase (10)", btnX + btnW / 2, btnY + 3,
                    can ? COL_BTN_TEXT_DARK : 0xFFAAAAAA);
            if (can && btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBindingVow())));
            }
        }

        return y + boxH + 6;
    }

    private int drawCotCard(GuiGraphics g, int mx, int my,
                            int x, int y, int w, int cx,
                            boolean unlocked, int atAvailable, boolean enabled) {
        String desc = "Transcend into a cursed object and find a suitable host for a secondary technique";
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 12);
        int boxH = 52 + (int) (lines.size() * 9 * DESC_SCALE);

        drawCard(g, x, y, w, boxH);

        // Smaller COT title
        var pose = g.pose();
        pose.pushPose();
        pose.scale(COT_TITLE_SCALE, COT_TITLE_SCALE, 1f);
        g.drawCenteredString(font, "Cursed Object Transformation",
                (int) (cx / COT_TITLE_SCALE),
                (int) ((y + 6) / COT_TITLE_SCALE),
                COL_HEADER);
        pose.popPose();

        int dy = drawScaledLines(g, lines, cx, y + 18, COL_DESC, DESC_SCALE);

        if (unlocked) {
            g.drawCenteredString(font, "✔ Unlocked", cx, dy + 2, COL_REQ_MET);
        } else {
            int shown = Math.min(atAvailable, 99);
            g.drawCenteredString(font, shown + "/20", cx, dy + 2, COL_DESC);
        }

        int btnW = 80, btnH = 14;
        int btnX = cx - btnW / 2;
        int btnY = y + boxH - 18;
        boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);

        if (unlocked) {
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, COL_BTN_DISABLED);
            g.drawCenteredString(font, "Unlocked", btnX + btnW / 2, btnY + 3, 0xFFAAAAAA);
        } else {
            boolean can = enabled && atAvailable >= 20;
            int col = !can ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN);
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (can && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
            g.drawCenteredString(font, "Transform", btnX + btnW / 2, btnY + 3,
                    can ? COL_BTN_TEXT_DARK : 0xFFAAAAAA);
            if (can && btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketUnlockCursedObjectTransformation())));
            }
        }

        return y + boxH + 6;
    }

    private int drawOpenDomainCard(GuiGraphics g, int mx, int my,
                                   int x, int y, int w, int cx,
                                   boolean unlocked, boolean witnessed, boolean hasSpecial,
                                   int rpRefinement, int rpAvailable, int atAvailable,
                                   boolean enabled) {
        String desc = "Paint a picture in the air without a canvas, it is truly a divine feat";
        List<FormattedCharSequence> lines = font.split(Component.literal(desc), w - 12);

        int boxH = unlocked
                ? (44 + (int) (lines.size() * 9 * DESC_SCALE))
                : (OD_EXTRA_H + (int) (lines.size() * 9 * DESC_SCALE));

        drawCard(g, x, y, w, boxH);
        g.drawCenteredString(font, "Open Domain", cx, y + 6, COL_HEADER);

        int dy = drawScaledLines(g, lines, cx, y + 18, COL_DESC, DESC_SCALE);

        if (unlocked) {
            g.drawCenteredString(font, "✔ Unlocked", cx, dy + 4, COL_REQ_MET);
        } else {
            dy += 4;

            // Compact scaled requirements
            var pose = g.pose();
            pose.pushPose();
            pose.scale(REQ_SCALE, REQ_SCALE, 1f);
            int rx = (int) (cx / REQ_SCALE);
            int ry = (int) (dy / REQ_SCALE);

            drawReqScaled(g, rx, ry, "Special Grade", hasSpecial);
            ry += 11;
            drawReqScaled(g, rx, ry, "Witness", witnessed);
            ry += 11;
            drawReqScaled(g, rx, ry, "Max Domain Refinement (" + rpRefinement + "/50)", rpRefinement >= 50);
            ry += 11;
            drawReqScaled(g, rx, ry, "Refinement Points: " + Math.min(rpAvailable, 99) + "/20", rpAvailable >= 20);
            ry += 11;
            drawReqScaled(g, rx, ry, "Adv. Tech Points: " + Math.min(atAvailable, 99) + "/20", atAvailable >= 20);
            pose.popPose();

            boolean can = enabled && witnessed && hasSpecial
                    && rpRefinement >= 50 && rpAvailable >= 20 && atAvailable >= 20;

            // Button always at bottom of card
            int btnW = 80, btnH = 14;
            int btnX = cx - btnW / 2;
            int btnY = y + boxH - 18;
            boolean hover = isInside(mx, my, btnX, btnY, btnW, btnH);
            int col = !can ? COL_BTN_DISABLED : (hover ? COL_BTN_HOVER : COL_BTN);
            g.fill(btnX, btnY, btnX + btnW, btnY + btnH, col);
            if (can && hover) drawHoverEdge(g, btnX, btnY, btnW, btnH);
            g.drawCenteredString(font, "Unlock", btnX + btnW / 2, btnY + 3,
                    can ? COL_BTN_TEXT_DARK : 0xFFAAAAAA);

            if (can && btnY + btnH > contentTop && btnY < contentBottom) {
                buttons.add(new Btn(btnX, btnY, btnW, btnH, () ->
                        ModNetwork.CHANNEL.sendToServer(new PacketRequestUnlockOpenDomain())));
            }
        }

        return y + boxH + 6;
    }

    /** Draws scaled centered lines; returns the Y just below them (unscaled). */
    private int drawScaledLines(GuiGraphics g, List<FormattedCharSequence> lines,
                                int cx, int startY, int color, float scale) {
        var pose = g.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1f);
        int sy = (int) (startY / scale);
        for (FormattedCharSequence line : lines) {
            g.drawCenteredString(font, line, (int) (cx / scale), sy, color);
            sy += 10;
        }
        pose.popPose();
        return (int) (sy * scale) + 2;
    }

    private void drawReqScaled(GuiGraphics g, int cx, int y, String text, boolean met) {
        String line = (met ? "✔ " : "✘ ") + text;
        g.drawCenteredString(font, line, cx, y, met ? COL_REQ_MET : COL_REQ_UNMET);
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

    // Client UI assumes eligible; server enforces real requirements
    private boolean hasGrade1Advancement(Player player) {
        return true;
    }

    private boolean hasSpecialGradeAdvancement(Player player) {
        return true;
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