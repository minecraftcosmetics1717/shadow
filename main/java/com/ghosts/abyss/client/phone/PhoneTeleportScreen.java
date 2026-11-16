
package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.client.ClientEvents;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.RequestTeleportPacket;
import com.ghosts.abyss.network.RequestTeleportSyncPacket;
import com.ghosts.abyss.network.RespondTeleportPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhoneTeleportScreen extends Screen {

    private static final int W = 140;
    private static final int H = 240;

    private static final int LIST_TOP = 125;
    private static final int LIST_BOTTOM = 202;
    private static final int ROW_H = 19;

    private int left, top;
    private int scroll = 0;
    private int incomingScroll = 0;

    private final List<PlayerInfo> players = new ArrayList<>();

    public PhoneTeleportScreen() {
        super(Component.literal("Teleport"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        scroll = 0;
        incomingScroll = 0;
        refreshPlayers();
        ModNetwork.CHANNEL.sendToServer(
                new RequestTeleportSyncPacket()
        );
    }

    private void refreshPlayers() {
        players.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;

        UUID self = mc.player.getUUID();

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (!info.getProfile().getId().equals(self)) {
                players.add(info);
            }
        }

        int visible = (LIST_BOTTOM - LIST_TOP) / ROW_H;
        scroll = Mth.clamp(scroll, 0,
                Math.max(0, players.size() - visible));
    }

    private long clientTick() {
        return minecraft != null && minecraft.level != null
                ? minecraft.level.getGameTime() : 0L;
    }

    private void drawHead(GuiGraphics g, UUID id, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        PlayerInfo info = mc.getConnection() == null
                ? null : mc.getConnection().getPlayerInfo(id);

        if (info != null) {
            PlayerFaceRenderer.draw(
                    g, info.getSkinLocation(), x, y, 14
            );
        } else {
            g.fill(x, y, x + 14, y + 14, 0xFF555555);
        }
    }

    @Override
    public void render(
            GuiGraphics g, int mx, int my, float partialTick
    ) {
        renderBackground(g);
        refreshPlayers();

        PhoneMessageUi.base(g, left, top, "Teleport");

        long tick = clientTick();
        var incoming = PhoneTeleportClient.incoming(tick);

        incomingScroll = Mth.clamp(
                incomingScroll, 0,
                Math.max(0, incoming.size() - 2)
        );

        g.drawString(font, "Incoming Requests",
                left + 9, top + 44, 0xFFE6EDF3, false);

        int shown = Math.min(
                2, incoming.size() - incomingScroll
        );

        for (int i = 0; i < shown; i++) {
            var request = incoming.get(i + incomingScroll);
            int y = top + 56 + i * 27;

            g.fill(left + 7, y,
                    left + 133, y + 25, 0xFF29282E);

            drawHead(g, request.senderId(), left + 10, y + 5);

            String name = font.plainSubstrByWidth(
                    request.senderName(), 60
            );

            g.drawString(font, name,
                    left + 28, y + 3, 0xFFFFFFFF, false);

            int seconds = PhoneTeleportClient.secondsLeft(
                    request.expiresAt(), tick
            );

            g.drawString(font, seconds + "s",
                    left + 110, y + 3, 0xFFBBBBBB, false);

            // Accept button.
            g.fill(left + 29, y + 13,
                    left + 73, y + 23, 0xFF247C46);

            g.drawCenteredString(
                    font, "Accept",
                    left + 51, y + 14, 0xFFFFFFFF
            );

            // Deny button.
            g.fill(left + 77, y + 13,
                    left + 121, y + 23, 0xFF9C3038);

            g.drawCenteredString(
                    font, "Deny",
                    left + 99, y + 14, 0xFFFFFFFF
            );
        }

        if (incoming.isEmpty()) {
            g.drawCenteredString(
                    font, "No incoming requests",
                    left + 70, top + 72, 0xFF8B949E
            );
        }

        g.drawString(font, "Request Teleport",
                left + 9, top + 113, 0xFFE6EDF3, false);

        int listY = top + LIST_TOP;
        int bottom = top + LIST_BOTTOM;
        int visible = (LIST_BOTTOM - LIST_TOP) / ROW_H;

        g.enableScissor(
                left + 6, listY, left + 134, bottom
        );

        for (int i = 0; i < visible; i++) {
            int index = i + scroll;
            if (index >= players.size()) break;

            PlayerInfo info = players.get(index);
            UUID id = info.getProfile().getId();
            int y = listY + i * ROW_H;

            int remaining = PhoneTeleportClient.outgoingSeconds(
                    id, tick
            );
            boolean pending = remaining > 0;

            boolean hover = PhoneMessageUi.inside(
                    mx, my, left + 8, y, 124, ROW_H
            );

            g.fill(left + 8, y,
                    left + 132, y + ROW_H - 2,
                    pending ? 0xFF30303A :
                            hover ? 0xFF39323F : 0xFF242424
            );

            drawHead(g, id, left + 12, y + 2);

            String name = font.plainSubstrByWidth(
                    info.getProfile().getName(),
                    pending ? 60 : 87
            );

            g.drawString(font, name,
                    left + 31, y + 5,
                    pending ? 0xFF999999 : 0xFFE6EDF3,
                    false);

            if (pending) {
                g.drawString(
                        font, remaining + "s",
                        left + 109, y + 5,
                        0xFFA371F7, false
                );
            }
        }

        g.disableScissor();

        if (players.isEmpty()) {
            g.drawCenteredString(
                    font, "No players online",
                    left + 70, listY + 15, 0xFF8B949E
            );
        }

        PhoneMessageUi.dock(g, left, top);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }

        int dockX = left + 26;
        int dockY = top + 209;

        if (PhoneMessageUi.inside(
                mx, my, dockX, dockY, 24, 24)) {
            minecraft.setScreen(new PhoneMessagesScreen());
            return true;
        }

        if (PhoneMessageUi.inside(
                mx, my, dockX + 32, dockY, 24, 24)) {
            minecraft.setScreen(new PhoneMapScreen());
            return true;
        }

        if (PhoneMessageUi.inside(
                mx, my, dockX + 64, dockY, 24, 24)) {
            return true;
        }

        long tick = clientTick();
        var incoming = PhoneTeleportClient.incoming(tick);

        incomingScroll = Mth.clamp(
                incomingScroll, 0,
                Math.max(0, incoming.size() - 2)
        );

        for (int i = 0; i < Math.min(
                2, incoming.size() - incomingScroll); i++) {

            var request = incoming.get(i + incomingScroll);
            int y = top + 56 + i * 27;

            if (PhoneMessageUi.inside(
                    mx, my, left + 29, y + 13, 44, 10)) {

                ModNetwork.CHANNEL.sendToServer(
                        new RespondTeleportPacket(
                                request.senderId(), true
                        )
                );
                return true;
            }

            if (PhoneMessageUi.inside(
                    mx, my, left + 77, y + 13, 44, 10)) {

                ModNetwork.CHANNEL.sendToServer(
                        new RespondTeleportPacket(
                                request.senderId(), false
                        )
                );
                return true;
            }
        }

        int visible = (LIST_BOTTOM - LIST_TOP) / ROW_H;

        for (int i = 0; i < visible; i++) {
            int index = i + scroll;
            if (index >= players.size()) break;

            int y = top + LIST_TOP + i * ROW_H;

            if (PhoneMessageUi.inside(
                    mx, my, left + 8, y, 124, ROW_H - 2
            )) {
                PlayerInfo info = players.get(index);
                UUID id = info.getProfile().getId();

                if (PhoneTeleportClient.hasOutgoing(id, tick)) {
                    return true;
                }

                if (PhoneMessageClient.blocked()) {
                    return true;
                }

                ModNetwork.CHANNEL.sendToServer(
                        new RequestTeleportPacket(
                                info.getProfile().getName()
                        )
                );

                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(
            double mx, double my, double amount
    ) {
        if (PhoneMessageUi.inside(
                mx, my, left + 7, top + 54, 126, 55)) {

            int count = PhoneTeleportClient
                    .incoming(clientTick()).size();

            incomingScroll = Mth.clamp(
                    incomingScroll - (int) Math.signum(amount),
                    0,
                    Math.max(0, count - 2)
            );
            return true;
        }

        int visible = (LIST_BOTTOM - LIST_TOP) / ROW_H;

        scroll = Mth.clamp(
                scroll - (int) Math.signum(amount),
                0,
                Math.max(0, players.size() - visible)
        );

        return true;
    }

    @Override
    public boolean keyPressed(
            int key, int scan, int mods
    ) {
        if (key == 256 || key == 69
                || ClientEvents.OPEN_PHONE.matches(key, scan)) {
            minecraft.setScreen(new PhoneScreen());
            return true;
        }

        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
