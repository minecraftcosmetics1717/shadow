package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.AdvancedJujutsuScreen;
import com.ghosts.abyss.ClientSetup;
import com.ghosts.abyss.gui.YutaTab;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketToggleMinimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PhoneScreen extends Screen {

    private static final ResourceLocation PHONE_BG =
            new ResourceLocation("abyss", "textures/gui/phone/phone_bg.png");

    private static final ResourceLocation ICON_DOMAIN =
            new ResourceLocation("abyss", "textures/gui/phone/icon_domain.png");
    private static final ResourceLocation ICON_BLACKFLASH =
            new ResourceLocation("abyss", "textures/gui/phone/icon_blackflash.png");
    private static final ResourceLocation ICON_RCT =
            new ResourceLocation("abyss", "textures/gui/phone/icon_rct.png");
    private static final ResourceLocation ICON_ASCENDANT =
            new ResourceLocation("abyss", "textures/gui/phone/icon_ascendant.png");
    private static final ResourceLocation ICON_COPY =
            new ResourceLocation("abyss", "textures/gui/phone/icon_copy.png");
    private static final ResourceLocation ICON_CSM =
            new ResourceLocation("abyss", "textures/gui/phone/icon_csm.png");

    private static final ResourceLocation ICON_MESSAGES =
            new ResourceLocation("abyss", "textures/gui/phone/icon_messages.png");
    private static final ResourceLocation ICON_MAP =
            new ResourceLocation("abyss", "textures/gui/phone/icon_map.png");
    private static final ResourceLocation ICON_TELEPORT =
            new ResourceLocation("abyss", "textures/gui/phone/icon_teleport.png");

    private static final int PHONE_W = 140;
    private static final int PHONE_H = 240;

    // Top apps (Domain → Ascendant) — leave these as you like them
    private static final int ICON_SIZE = 26;
    private static final int TOP_GAP = 6;
    private static final float LABEL_SCALE = 0.45f;

    // Dock (bottom)
    private static final int DOCK_ICON_SIZE = 24;  // slightly smaller than top
    private static final int DOCK_GAP = 8;

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
        g.blit(PHONE_BG, left, top, 0, 0, PHONE_W, PHONE_H, PHONE_W, PHONE_H);

        // Time
        g.drawString(font, getInGameTimeString(), left + 8, top + 10, 0xFFE6EDF3, false);

        // ===== TOP ROW (Domain / Blackflash / RCT / Ascendant) =====
        int topY = top + 46;
        int topTotalW = 4 * ICON_SIZE + 3 * TOP_GAP;
        int topStartX = left + (PHONE_W - topTotalW) / 2;

        int domainX = topStartX;
        int bfX     = topStartX + (ICON_SIZE + TOP_GAP);
        int rctX    = topStartX + 2 * (ICON_SIZE + TOP_GAP);
        int ascX    = topStartX + 3 * (ICON_SIZE + TOP_GAP);

        drawIcon(g, ICON_DOMAIN,     domainX, topY, mouseX, mouseY, ICON_SIZE);
        drawIcon(g, ICON_BLACKFLASH, bfX,     topY, mouseX, mouseY, ICON_SIZE);
        drawIcon(g, ICON_RCT,        rctX,    topY, mouseX, mouseY, ICON_SIZE);
        drawIcon(g, ICON_ASCENDANT,  ascX,    topY, mouseX, mouseY, ICON_SIZE);

        // Labels
        var pose = g.pose();
        pose.pushPose();
        pose.scale(LABEL_SCALE, LABEL_SCALE, 1f);
        int labelY = (int) ((topY + ICON_SIZE + 3) / LABEL_SCALE);

        g.drawCenteredString(font, "Domain",
                (int) ((domainX + ICON_SIZE / 2f) / LABEL_SCALE), labelY, 0xFF8B949E);
        g.drawCenteredString(font, "Refinement",
                (int) ((domainX + ICON_SIZE / 2f) / LABEL_SCALE), labelY + 8, 0xFF8B949E);
        g.drawCenteredString(font, "Blackflash",
                (int) ((bfX + ICON_SIZE / 2f) / LABEL_SCALE), labelY, 0xFF8B949E);
        g.drawCenteredString(font, "RCT",
                (int) ((rctX + ICON_SIZE / 2f) / LABEL_SCALE), labelY, 0xFF8B949E);
        g.drawCenteredString(font, "Ascendant",
                (int) ((ascX + ICON_SIZE / 2f) / LABEL_SCALE), labelY, 0xFF8B949E);
        pose.popPose();

        // ===== SECOND ROW: Copy / CSM (a bit higher than before) =====
        boolean isYuta = YutaTab.isLocalPlayerYuta();
        boolean isCsm  = ClientSetup.isLocalPlayerCsm(Minecraft.getInstance());

        if (isYuta || isCsm) {
            // was ICON_SIZE + 22 → lower number = higher on screen
            int secondY = topY + ICON_SIZE + 16;
            int secondX = domainX;

            if (isYuta) {
                drawIcon(g, ICON_COPY, secondX, secondY, mouseX, mouseY, ICON_SIZE);
                pose.pushPose();
                pose.scale(LABEL_SCALE, LABEL_SCALE, 1f);
                int cLabelY = (int) ((secondY + ICON_SIZE + 3) / LABEL_SCALE);
                g.drawCenteredString(font, "Copy",
                        (int) ((secondX + ICON_SIZE / 2f) / LABEL_SCALE), cLabelY, 0xFF8B949E);
                pose.popPose();
            } else {
                drawIcon(g, ICON_CSM, secondX, secondY, mouseX, mouseY, ICON_SIZE);
                pose.pushPose();
                pose.scale(LABEL_SCALE, LABEL_SCALE, 1f);
                int cLabelY = (int) ((secondY + ICON_SIZE + 3) / LABEL_SCALE);
                g.drawCenteredString(font, "CSM",
                        (int) ((secondX + ICON_SIZE / 2f) / LABEL_SCALE), cLabelY, 0xFF8B949E);
                pose.popPose();
            }
        }

        // ===== BOTTOM DOCK =====
        int dockY = top + PHONE_H - 31;
        int dockTotalW = 3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;
        int dockStartX = left + (PHONE_W - dockTotalW) / 2;

        int msgX = dockStartX;
        int mapX = dockStartX + DOCK_ICON_SIZE + DOCK_GAP;
        int tpX  = dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP);

        drawIcon(g, ICON_MESSAGES, msgX, dockY, mouseX, mouseY, DOCK_ICON_SIZE);
        drawIcon(g, ICON_MAP,      mapX, dockY, mouseX, mouseY, DOCK_ICON_SIZE);
        drawIcon(g, ICON_TELEPORT, tpX,  dockY, mouseX, mouseY, DOCK_ICON_SIZE);
    }

    private void drawIcon(GuiGraphics g, ResourceLocation tex, int x, int y, int mouseX, int mouseY, int size) {
        boolean hover = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        if (hover) {
            g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0x44FFFFFF);
        }
        g.blit(tex, x, y, 0, 0, size, size, size, size);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Must match render()
        int topY = top + 46;
        int topTotalW = 4 * ICON_SIZE + 3 * TOP_GAP;
        int topStartX = left + (PHONE_W - topTotalW) / 2;
        int domainX = topStartX;
        int bfX     = topStartX + (ICON_SIZE + TOP_GAP);
        int rctX    = topStartX + 2 * (ICON_SIZE + TOP_GAP);
        int ascX    = topStartX + 3 * (ICON_SIZE + TOP_GAP);

        if (isInside(mouseX, mouseY, domainX, topY, ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneDomainScreen());
            return true;
        }
        if (isInside(mouseX, mouseY, bfX, topY, ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneBlackFlashScreen());
            return true;
        }
        if (isInside(mouseX, mouseY, rctX, topY, ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneRctScreen());
            return true;
        }
        if (isInside(mouseX, mouseY, ascX, topY, ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneAscendantScreen());
            return true;
        }

        boolean isYuta = YutaTab.isLocalPlayerYuta();
        boolean isCsm  = ClientSetup.isLocalPlayerCsm(Minecraft.getInstance());
        if (isYuta || isCsm) {
            int secondY = topY + ICON_SIZE + 16; // same as render
            int secondX = domainX;
            if (isInside(mouseX, mouseY, secondX, secondY, ICON_SIZE)) {
                Minecraft.getInstance().setScreen(new AdvancedJujutsuScreen(isYuta ? -1 : -2));
                return true;
            }
        }

        // Dock positions (must be declared before use)
        int dockY = top + PHONE_H - 31;
        int dockTotalW = 3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;
        int dockStartX = left + (PHONE_W - dockTotalW) / 2;
        int msgX = dockStartX;
        int mapX = dockStartX + DOCK_ICON_SIZE + DOCK_GAP;
        int tpX  = dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP);

        if (isInside(mouseX, mouseY, msgX, dockY, DOCK_ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen("Messages", "Quests & messages coming soon."));
            return true;
        }
        if (isInside(mouseX, mouseY, mapX, dockY, DOCK_ICON_SIZE)) {
            var p = Minecraft.getInstance().player;
            boolean unlocked = p != null && p.getPersistentData().getBoolean("minimap_unlocked");
            if (!unlocked) {
                Minecraft.getInstance().setScreen(new PhoneMapScreen());
            } else {
                ModNetwork.CHANNEL.sendToServer(new PacketToggleMinimap());
            }
            return true;
        }
        if (isInside(mouseX, mouseY, tpX, dockY, DOCK_ICON_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInside(double mx, double my, int x, int y, int size) {
        return mx >= x && mx < x + size && my >= y && my < y + size;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Phone key or E → close
        if (com.ghosts.abyss.client.ClientEvents.OPEN_PHONE.matches(keyCode, scanCode)
                || keyCode == 69 /* E */) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}