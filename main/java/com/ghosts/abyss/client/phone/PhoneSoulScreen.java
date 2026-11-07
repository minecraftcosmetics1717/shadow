
package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketSpendSoulAwarenessPoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class PhoneSoulScreen extends Screen {

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

    // Purple theme
    private static final int COL_TITLE = 0xFFE6EDF3;
    private static final int COL_PURPLE = 0xFFAA00FF;
    private static final int COL_PURPLE_HOVER = 0xFFCC66FF;
    private static final int COL_DARK_PURPLE = 0xFF39005C;
    private static final int COL_CARD_BG = 0xFF030003;
    private static final int COL_DISABLED = 0xFF30363D;
    private static final int COL_DISABLED_TEXT = 0xFFAAAAAA;

    private int left;
    private int top;

    private int buttonX;
    private int buttonY;
    private int buttonW;
    private int buttonH;

    private boolean canUpgrade;

    public PhoneSoulScreen() {
        super(Component.literal("Soul Awareness"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;

        canUpgrade = false;
    }

    @Override
    public void render(
            GuiGraphics g,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(g);

        g.blit(
                PHONE_BG,
                left,
                top,
                0,
                0,
                PHONE_W,
                PHONE_H,
                PHONE_W,
                PHONE_H
        );

        g.drawString(
                font,
                getInGameTimeString(),
                left + 8,
                top + 10,
                COL_TITLE,
                false
        );

        g.drawCenteredString(
                font,
                "Soul Awareness",
                left + PHONE_W / 2,
                top + 28,
                COL_TITLE
        );

        Player player = Minecraft.getInstance().player;

        int awareness = 0;
        int points = 0;

        if (player != null) {
            awareness = Math.max(
                    0,
                    Math.min(
                            50,
                            player.getPersistentData().getInt(
                                    "abyss_soul_awareness"
                            )
                    )
            );

            points = Math.max(
                    0,
                    player.getPersistentData().getInt(
                            "abyss_soul_points_available"
                    )
            );
        }

        int cx = left + PHONE_W / 2;

        g.drawCenteredString(
                font,
                "Soul Points: " + points,
                cx,
                top + 48,
                COL_PURPLE
        );

        // Main card
        int cardX = left + 6;
        int cardY = top + 62;
        int cardW = PHONE_W - 12;
        int cardH = 102;

        g.fill(
                cardX,
                cardY,
                cardX + cardW,
                cardY + cardH,
                COL_CARD_BG
        );

        // Purple border
        g.fill(cardX, cardY, cardX + cardW, cardY + 1, COL_PURPLE);
        g.fill(cardX, cardY + cardH - 1,
                cardX + cardW, cardY + cardH, COL_PURPLE);
        g.fill(cardX, cardY, cardX + 1, cardY + cardH, COL_PURPLE);
        g.fill(cardX + cardW - 1, cardY,
                cardX + cardW, cardY + cardH, COL_PURPLE);

        g.drawCenteredString(
                font,
                "Soul Awareness",
                cx,
                cardY + 8,
                COL_PURPLE
        );

        // Description
        String description =
                "Souls act as blueprints for physical bodies. " +
                        "The more you understand them, the more defense " +
                        "you have against soul-based attacks.";

        float scale = 0.67f;

        List<FormattedCharSequence> lines =
                font.split(
                        Component.literal(description),
                        (int) ((cardW - 14) / scale)
                );

        var pose = g.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);

        int descY = (int) ((cardY + 23) / scale);
        int scaledCx = (int) (cx / scale);

        for (FormattedCharSequence line : lines) {
            g.drawCenteredString(
                    font,
                    line,
                    scaledCx,
                    descY,
                    COL_PURPLE
            );
            descY += 12;
        }

        pose.popPose();

        // Progress bar
        int barX = cardX + 24;
        int barY = cardY + 68;
        int barW = cardW - 48;
        int barH = 8;

        g.fill(
                barX,
                barY,
                barX + barW,
                barY + barH,
                COL_DARK_PURPLE
        );

        int fillW = (int) (barW * (awareness / 50.0f));

        g.fill(
                barX,
                barY,
                barX + fillW,
                barY + barH,
                COL_PURPLE
        );

        // Show numeric progress inside the bar.
        String progress = awareness + "/50";

        g.drawCenteredString(
                font,
                progress,
                cx,
                barY,
                COL_TITLE
        );

        // Upgrade button
        buttonW = 96;
        buttonH = 14;
        buttonX = cx - buttonW / 2;
        buttonY = cardY + 82;

        canUpgrade =
                player != null
                        && points > 0
                        && awareness < 50;

        boolean hovered = isInside(
                mouseX,
                mouseY,
                buttonX,
                buttonY,
                buttonW,
                buttonH
        );

        int buttonColor = !canUpgrade
                ? COL_DISABLED
                : (hovered ? COL_PURPLE_HOVER : COL_PURPLE);

        g.fill(
                buttonX,
                buttonY,
                buttonX + buttonW,
                buttonY + buttonH,
                buttonColor
        );

        String buttonText =
                awareness >= 50 ? "MAXED" : "Upgrade";

        g.drawCenteredString(
                font,
                buttonText,
                cx,
                buttonY + 3,
                canUpgrade ? 0xFF160020 : COL_DISABLED_TEXT
        );

        renderDock(g, mouseX, mouseY);
    }

    private void renderDock(
            GuiGraphics g,
            int mouseX,
            int mouseY
    ) {
        int dockY = top + PHONE_H - 31;
        int dockTotalW =
                3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;

        int dockStartX =
                left + (PHONE_W - dockTotalW) / 2;

        drawDockIcon(
                g, ICON_MESSAGES,
                dockStartX, dockY, mouseX, mouseY
        );

        drawDockIcon(
                g, ICON_MAP,
                dockStartX + DOCK_ICON_SIZE + DOCK_GAP,
                dockY, mouseX, mouseY
        );

        drawDockIcon(
                g, ICON_TELEPORT,
                dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP),
                dockY, mouseX, mouseY
        );
    }

    private void drawDockIcon(
            GuiGraphics g,
            ResourceLocation texture,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        if (isInside(
                mouseX, mouseY,
                x, y,
                DOCK_ICON_SIZE, DOCK_ICON_SIZE
        )) {
            g.fill(
                    x - 2,
                    y - 2,
                    x + DOCK_ICON_SIZE + 2,
                    y + DOCK_ICON_SIZE + 2,
                    0x44FFFFFF
            );
        }

        g.blit(
                texture,
                x, y,
                0, 0,
                DOCK_ICON_SIZE,
                DOCK_ICON_SIZE,
                DOCK_ICON_SIZE,
                DOCK_ICON_SIZE
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (canUpgrade && isInside(
                mouseX, mouseY,
                buttonX, buttonY,
                buttonW, buttonH
        )) {
            ModNetwork.CHANNEL.sendToServer(
                    new PacketSpendSoulAwarenessPoint()
            );
            return true;
        }

        int dockY = top + PHONE_H - 31;
        int dockTotalW =
                3 * DOCK_ICON_SIZE + 2 * DOCK_GAP;

        int dockStartX =
                left + (PHONE_W - dockTotalW) / 2;

        if (isInside(
                mouseX, mouseY,
                dockStartX, dockY,
                DOCK_ICON_SIZE, DOCK_ICON_SIZE
        )) {
            Minecraft.getInstance().setScreen(
                    new PhonePlaceholderScreen(
                            "Messages",
                            "Quests & messages coming soon."
                    )
            );
            return true;
        }

        if (isInside(
                mouseX, mouseY,
                dockStartX + DOCK_ICON_SIZE + DOCK_GAP,
                dockY,
                DOCK_ICON_SIZE,
                DOCK_ICON_SIZE
        )) {
            var player = Minecraft.getInstance().player;
            boolean unlocked = player != null
                    && player.getPersistentData()
                    .getBoolean("minimap_unlocked");

            if (!unlocked) {
                Minecraft.getInstance().setScreen(
                        new PhoneMapScreen()
                );
            } else {
                ModNetwork.CHANNEL.sendToServer(
                        new com.ghosts.abyss.network.PacketToggleMinimap()
                );
            }
            return true;
        }

        if (isInside(
                mouseX, mouseY,
                dockStartX + 2 * (DOCK_ICON_SIZE + DOCK_GAP),
                dockY,
                DOCK_ICON_SIZE,
                DOCK_ICON_SIZE
        )) {
            Minecraft.getInstance().setScreen(
                    new PhoneTeleportScreen()
            );
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == 256
                || keyCode == 69
                || com.ghosts.abyss.client.ClientEvents
                .OPEN_PHONE.matches(keyCode, scanCode)) {
            Minecraft.getInstance().setScreen(
                    new PhoneScreen()
            );
            return true;
        }

        return super.keyPressed(
                keyCode, scanCode, modifiers
        );
    }

    private boolean isInside(
            double mx,
            double my,
            int x,
            int y,
            int w,
            int h
    ) {
        return mx >= x
                && mx < x + w
                && my >= y
                && my < y + h;
    }

    private String getInGameTimeString() {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return "00:00";
        }

        long dayTime = mc.level.getDayTime() % 24000L;
        int totalMinutes =
                (int) ((dayTime * 60 * 24) / 24000L);

        int hours = (totalMinutes / 60 + 6) % 24;
        int minutes = totalMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
