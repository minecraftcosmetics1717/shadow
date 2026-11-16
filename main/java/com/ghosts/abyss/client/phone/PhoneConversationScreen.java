package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.client.ClientEvents;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PhoneMessagePackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.UUID;

public class PhoneConversationScreen extends Screen {
    private final String thread, title;
    private final boolean quest;
    private int left, top, scroll;
    private int lastCount = -1;
    private int contentHeight;
    private EditBox input;
    private static final int AREA_TOP = 43;
    private static final int AREA_BOTTOM = 176;

    public PhoneConversationScreen(String thread, String title, boolean quest) {
        super(Component.literal(title));
        this.thread = thread;
        this.title = title;
        this.quest = quest;
    }

    @Override
    protected void init() {
        left = (width - PhoneMessageUi.W) / 2;
        top = (height - PhoneMessageUi.H) / 2;
        input = new EditBox(font, left + 8, top + 180, 98, 18, Component.literal("Message"));
        input.setMaxLength(180);
        input.setTextColor(0xFFFFFFFF);
        input.setVisible(!quest);
        addRenderableWidget(input);
        scroll = 0;
        lastCount = -1;
        ModNetwork.CHANNEL.sendToServer(new PhoneMessagePackets.Request());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        PhoneMessageUi.base(g, left, top, font.plainSubstrByWidth(title, 100));
        List<PhoneMessageClient.Message> messages = PhoneMessageClient.messages(thread);
        int height = 5;
        for (var m : messages) {
            int numLines = font.split(Component.literal(m.body()), 94).size();
            height += 8 + numLines * 10 + 6;
        }
        contentHeight = height;
        int visible = AREA_BOTTOM - AREA_TOP;
        int maxScroll = Math.max(0, height - visible);
        if (lastCount != messages.size()) {
            scroll = maxScroll;
            lastCount = messages.size();
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        g.enableScissor(left + 5, top + AREA_TOP, left + 135, top + AREA_BOTTOM);
        int y = top + AREA_TOP + 5 - scroll;
        for (var m : messages) {
            List<FormattedCharSequence> lines = font.split(Component.literal(m.body()), 94);
            int textW = 20;
            for (FormattedCharSequence line : lines) textW = Math.max(textW, font.width(line));
            int bubbleW = Math.min(112, textW + 10);
            int bubbleH = lines.size() * 10 + 8;
            int x = m.outgoing() ? left + 134 - bubbleW : left + 7;
            g.fill(x, y, x + bubbleW, y + bubbleH,
                    m.outgoing() ? PhoneMessageUi.BLUE : 0xFF666666);
            for (int i = 0; i < lines.size(); i++)
                g.drawString(font, lines.get(i), x + 5, y + 4 + i * 10, 0xFFFFFFFF, false);
            y += bubbleH + 6;
        }
        g.disableScissor();

        if (quest) {
            g.fill(left + 6, top + 179, left + 134, top + 201, 0xFF242424);
            g.drawCenteredString(font, "Quest messages: read only", left + 70,
                    top + 186, PhoneMessageUi.MUTED);
        } else {
            boolean blocked = PhoneMessageClient.blocked();
            input.setEditable(!blocked);
            input.render(g, mx, my, pt);
            g.fill(left + 108, top + 180, left + 133, top + 198,
                    blocked ? 0xFF7F2020 : PhoneMessageUi.BLUE);
            g.drawCenteredString(font, ">", left + 120, top + 185, 0xFFFFFFFF);
            if (blocked)
                g.drawCenteredString(font, "No connection", left + 70, top + 169, 0xFFFF5555);
        }
        PhoneMessageUi.dock(g, left, top);
    }

    private void send() {
        if (quest || PhoneMessageClient.blocked()) return;
        String text = input.getValue().strip();
        if (text.isBlank()) return;
        try {
            UUID id = UUID.fromString(thread);
            ModNetwork.CHANNEL.sendToServer(new PhoneMessagePackets.Send(id, text));
            input.setValue("");
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (!quest && PhoneMessageUi.inside(mx, my, left + 108, top + 180, 25, 18)) {
            send(); return true;
        }
        int dockX = left + 26;
        if (PhoneMessageUi.inside(mx, my, dockX, top + 209, 24, 24)) {
            Minecraft.getInstance().setScreen(new PhoneMessagesScreen()); return true;
        }
        if (PhoneMessageUi.inside(mx, my, dockX + 32, top + 209, 24, 24)) {
            Minecraft.getInstance().setScreen(new PhoneMapScreen()); return true;
        }
        if (PhoneMessageUi.inside(mx, my, dockX + 64, top + 209, 24, 24)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen()); return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int max = Math.max(0, contentHeight - (AREA_BOTTOM - AREA_TOP));
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(amount) * 20));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            Minecraft.getInstance().setScreen(new PhoneMessagesScreen()); return true;
        }
        if (!quest && key == 257) { send(); return true; }
        if (input != null && input.isFocused() && !quest)
            return super.keyPressed(key, scan, mods);
        if (key == 69 || ClientEvents.OPEN_PHONE.matches(key, scan)) {
            Minecraft.getInstance().setScreen(new PhoneScreen()); return true;
        }
        return super.keyPressed(key, scan, mods);
    }
    @Override public boolean isPauseScreen() { return false; }
}
