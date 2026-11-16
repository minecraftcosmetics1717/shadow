package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.client.ClientEvents;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PhoneMessagePackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhoneMessagesScreen extends Screen {
    private int left, top, scroll;
    private final List<PhoneMessageClient.Thread> threads = new ArrayList<>();
    private static final int LIST_START = 48;
    private static final int LIST_END = 200;
    private static final int ROW_H = 30;

    public PhoneMessagesScreen() { super(Component.literal("Messages")); }

    @Override
    protected void init() {
        left = (width - PhoneMessageUi.W) / 2;
        top = (height - PhoneMessageUi.H) / 2;
        scroll = 0;
        ModNetwork.CHANNEL.sendToServer(new PhoneMessagePackets.Request());
    }

    private void updateThreads() {
        threads.clear();
        threads.addAll(PhoneMessageClient.threads());
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;
        UUID self = mc.player.getUUID();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            if (id.equals(self)) continue;
            String key = id.toString();
            if (threads.stream().noneMatch(t -> t.id().equals(key))) {
                threads.add(new PhoneMessageClient.Thread(key, info.getProfile().getName(), false));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        PhoneMessageUi.base(g, left, top, "Messages");
        updateThreads();
        int y0 = top + LIST_START, bottom = top + LIST_END;
        g.enableScissor(left + 4, y0, left + PhoneMessageUi.W - 4, bottom);
        if (threads.isEmpty()) {
            g.drawCenteredString(font, "No conversations", left + 70, y0 + 14, PhoneMessageUi.MUTED);
        }
        for (int i = 0; i < threads.size(); i++) {
            int y = y0 + i * ROW_H - scroll;
            if (y + ROW_H <= y0 || y >= bottom) continue;
            var thread = threads.get(i);
            boolean hover = PhoneMessageUi.inside(mx, my, left + 5, y, 130, ROW_H);
            g.fill(left + 5, y, left + 135, y + ROW_H - 2,
                    hover ? 0xFF36343A : 0xFF242424);
            if (thread.quest()) {
                g.fill(left + 10, y + 5, left + 30, y + 25, 0xFF59466D);
                g.drawCenteredString(font, "!", left + 20, y + 11, 0xFFFFFFFF);
            } else {
                try {
                    UUID id = UUID.fromString(thread.id());
                    PlayerInfo info = Minecraft.getInstance().getConnection() == null ? null :
                            Minecraft.getInstance().getConnection().getPlayerInfo(id);
                    if (info != null) PlayerFaceRenderer.draw(g, info.getSkinLocation(), left + 10, y + 5, 20);
                    else g.fill(left + 10, y + 5, left + 30, y + 25, 0xFF595959);
                } catch (IllegalArgumentException ignored) {}
            }
            String name = font.plainSubstrByWidth(thread.name(), 90);
            g.drawString(font, name, left + 36, y + 11, PhoneMessageUi.WHITE, false);
        }
        g.disableScissor();
        PhoneMessageUi.dock(g, left, top);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int y0 = top + LIST_START;
        if (PhoneMessageUi.inside(mx, my, left + 5, y0, 130, LIST_END - LIST_START)) {
            int idx = (int) ((my - y0 + scroll) / ROW_H);
            if (idx >= 0 && idx < threads.size()) {
                var t = threads.get(idx);
                Minecraft.getInstance().setScreen(new PhoneConversationScreen(t.id(), t.name(), t.quest()));
                return true;
            }
        }
        int dockX = left + (140 - 88) / 2;
        int dockY = top + 209;
        if (PhoneMessageUi.inside(mx, my, dockX, dockY, 24, 24)) return true;
        if (PhoneMessageUi.inside(mx, my, dockX + 32, dockY, 24, 24)) {
            Minecraft.getInstance().setScreen(new PhoneMapScreen()); return true;
        }
        if (PhoneMessageUi.inside(mx, my, dockX + 64, dockY, 24, 24)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen()); return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int max = Math.max(0, threads.size() * ROW_H - (LIST_END - LIST_START));
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(amount) * ROW_H));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 || key == 69 || ClientEvents.OPEN_PHONE.matches(key, scan)) {
            Minecraft.getInstance().setScreen(new PhoneScreen()); return true;
        }
        return super.keyPressed(key, scan, mods);
    }
    @Override public boolean isPauseScreen() { return false; }
}
