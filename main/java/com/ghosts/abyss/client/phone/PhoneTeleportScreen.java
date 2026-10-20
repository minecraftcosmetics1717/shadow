package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.RequestTeleportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class PhoneTeleportScreen extends Screen {

    private static final ResourceLocation PHONE_BG =
            new ResourceLocation("abyss", "textures/gui/phone/phone_bg.png");

    private static final int PHONE_W = 140;
    private static final int PHONE_H = 240;

    private int left, top;
    private final List<String> playerNames = new ArrayList<>();
    private int scroll = 0;

    public PhoneTeleportScreen() {
        super(Component.literal("Teleport"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;

        playerNames.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.player != null) {
            String self = mc.player.getGameProfile().getName();
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (!name.equals(self)) {
                    playerNames.add(name);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        // Same phone background
        g.blit(PHONE_BG, left, top, 0, 0, PHONE_W, PHONE_H, PHONE_W, PHONE_H);

        // In-game time
        g.drawString(font, getInGameTimeString(), left + 8, top + 10, 0xFFE6EDF3, false);

        // Title
        g.drawCenteredString(font, "Teleport", left + PHONE_W / 2, top + 28, 0xFFFFFFFF);
        g.drawCenteredString(font, "Select a player", left + PHONE_W / 2, top + 44, 0xFF8B949E);

        // Player list area
        int listTop = top + 54;
        int listBottom = top + PHONE_H - 36;
        int rowH = 16;
        int maxVisible = (listBottom - listTop) / rowH;

        if (playerNames.isEmpty()) {
            g.drawCenteredString(font, "No players online", left + PHONE_W / 2, listTop + 18, 0xFF666666);
        } else {
            for (int i = 0; i < maxVisible; i++) {
                int idx = i + scroll;
                if (idx >= playerNames.size()) break;

                String name = playerNames.get(idx);
                int y = listTop + i * rowH;

                boolean hover = mouseX >= left + 10 && mouseX <= left + PHONE_W - 10
                        && mouseY >= y && mouseY < y + rowH;

                if (hover) {
                    g.fill(left + 8, y, left + PHONE_W - 8, y + rowH, 0x44A371F7);
                }

                g.drawCenteredString(font, name, left + PHONE_W / 2, y + 4, hover ? 0xFFA371F7 : 0xFFE6EDF3);
            }
        }

        // Back hint
        g.drawCenteredString(font, "", left + PHONE_W / 2, top + PHONE_H - 22, 0xFF555555);

        super.render(g, mouseX, mouseY, partialTick);
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
        if (button != 0 || playerNames.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int listTop = top + 68;
        int listBottom = top + PHONE_H - 36;
        int rowH = 16;
        int maxVisible = (listBottom - listTop) / rowH;

        for (int i = 0; i < maxVisible; i++) {
            int idx = i + scroll;
            if (idx >= playerNames.size()) break;

            int y = listTop + i * rowH;
            if (mouseX >= left + 10 && mouseX <= left + PHONE_W - 10
                    && mouseY >= y && mouseY < y + rowH) {

                String target = playerNames.get(idx);
                ModNetwork.CHANNEL.sendToServer(new RequestTeleportPacket(target));

                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("Teleport request sent to " + target), true);
                }
                // Go back to main phone after sending
                minecraft.setScreen(new PhoneScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int listTop = top + 68;
        int listBottom = top + PHONE_H - 36;
        int rowH = 16;
        int maxVisible = (listBottom - listTop) / rowH;

        if (playerNames.size() > maxVisible) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, playerNames.size() - maxVisible);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Esc or I closes / goes back
        if (keyCode == 256) { // ESC
            minecraft.setScreen(new PhoneScreen());
            return true;
        }
        if (com.ghosts.abyss.client.ClientEvents.OPEN_PHONE.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}