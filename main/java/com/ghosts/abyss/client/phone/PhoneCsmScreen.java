
package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.ClientSetup;
import com.ghosts.abyss.client.ClientEvents;
import com.ghosts.abyss.csm.ClientCsmCache;
import com.ghosts.abyss.csm.CsmSpiritData;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketCsmFeedFinger;
import com.ghosts.abyss.network.PacketCsmMarkUnique;
import com.ghosts.abyss.network.PacketCsmRenameSpirit;
import com.ghosts.abyss.network.PacketCsmToggleSpared;
import com.ghosts.abyss.network.PacketToggleMinimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class PhoneCsmScreen extends Screen {

    private static final ResourceLocation PHONE_BG =
            new ResourceLocation(
                    "abyss",
                    "textures/gui/phone/phone_bg.png"
            );

    private static final ResourceLocation ICON_MESSAGES =
            new ResourceLocation(
                    "abyss",
                    "textures/gui/phone/icon_messages.png"
            );

    private static final ResourceLocation ICON_MAP =
            new ResourceLocation(
                    "abyss",
                    "textures/gui/phone/icon_map.png"
            );

    private static final ResourceLocation ICON_TELEPORT =
            new ResourceLocation(
                    "abyss",
                    "textures/gui/phone/icon_teleport.png"
            );

    private static final int PHONE_W = 140;
    private static final int PHONE_H = 240;

    private static final int DOCK_SIZE = 24;
    private static final int DOCK_GAP = 8;

    // CSM purple theme: #771FA3
    private static final int PURPLE = 0xFF771FA3;
    private static final int HOVER = 0xFF9C41CF;
    private static final int DARK_PURPLE = 0xFF3E1458;

    private static final int CARD = 0xFF11091A;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int MUTED = 0xFF9B91A5;
    private static final int DISABLED = 0xFF30363D;
    private static final int GOLD = 0xFFFFAA33;

    private int left;
    private int top;

    private int contentTop;
    private int contentBottom;
    private int contentHeight;
    private int scrollOffset;

    private int renameIndex = -1;
    private EditBox renameField;

    private final List<Hitbox> buttons =
            new ArrayList<>();

    public PhoneCsmScreen() {
        super(Component.literal(
                "Cursed Spirit Manipulation"
        ));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;

        contentTop = top + 42;
        contentBottom = top + PHONE_H - 36;

        scrollOffset = 0;
        renameIndex = -1;

        renameField = new EditBox(
                font,
                left + 15,
                contentTop,
                PHONE_W - 50,
                13,
                Component.literal("Rename Spirit")
        );

        renameField.setMaxLength(50);
        renameField.setTextColor(TEXT);
        renameField.visible = false;

        addRenderableWidget(renameField);
    }

    @Override
    public void render(
            GuiGraphics g,
            int mx,
            int my,
            float partialTick
    ) {
        renderBackground(g);

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
                getTime(),
                left + 8,
                top + 10,
                TEXT,
                false
        );

        // Smaller title to fit the phone.
        smallText(
                g,
                "Cursed Spirit Manipulation",
                left + PHONE_W / 2,
                top + 28,
                TEXT,
                0.72f
        );

        buttons.clear();
        renameField.visible = false;

        List<CsmSpiritData> spirits =
                ClientCsmCache.getAll();

        if (renameIndex >= spirits.size()) {
            cancelRename();
        }

        int x = left + 10;
        int w = PHONE_W - 20;
        int cx = left + PHONE_W / 2;

        int y = contentTop + 4 - scrollOffset;

        g.enableScissor(
                left + 4,
                contentTop,
                left + PHONE_W - 4,
                contentBottom
        );

        if (!ClientSetup.isLocalPlayerCsm(
                Minecraft.getInstance()
        )) {
            drawCard(g, x, y, w, 43);

            smallText(
                    g,
                    "CSM Technique Required",
                    cx,
                    y + 10,
                    PURPLE,
                    0.80f
            );

            smallText(
                    g,
                    "Obtain Geto's technique",
                    cx,
                    y + 25,
                    MUTED,
                    0.73f
            );

            y += 49;

        } else {

            // =====================================
            // LEGEND - FIRST PANEL
            // =====================================

            int legendH = 79;

            drawCard(g, x, y, w, legendH);

            g.drawCenteredString(
                    font,
                    "Legend",
                    cx,
                    y + 5,
                    PURPLE
            );

            divider(
                    g,
                    x + 6,
                    y + 17,
                    w - 12
            );

            smallText(
                    g,
                    "◆ Spared from Uzumaki",
                    cx,
                    y + 22,
                    TEXT,
                    0.78f
            );

            smallText(
                    g,
                    "● Available for Uzumaki",
                    cx,
                    y + 33,
                    TEXT,
                    0.78f
            );

            smallText(
                    g,
                    "✦ Finger Bearer (Unique)",
                    cx,
                    y + 44,
                    TEXT,
                    0.78f
            );

            smallText(
                    g,
                    "● Feed Finger",
                    cx,
                    y + 55,
                    GOLD,
                    0.78f
            );

            smallText(
                    g,
                    "Edit = Rename Spirit",
                    cx,
                    y + 66,
                    MUTED,
                    0.73f
            );

            y += legendH + 6;

            // =====================================
            // ONE EXPANDING STORED SPIRITS PANEL
            // =====================================

            int rowH = 40;
            int headerH = 30;

            int listH =
                    headerH
                            + Math.max(1, spirits.size()) * rowH
                            + 5;

            drawCard(g, x, y, w, listH);

            g.drawCenteredString(
                    font,
                    "Stored Spirits",
                    cx,
                    y + 5,
                    PURPLE
            );

            smallText(
                    g,
                    "Spirits Stored: " + spirits.size(),
                    cx,
                    y + 17,
                    TEXT,
                    0.80f
            );

            divider(
                    g,
                    x + 6,
                    y + 27,
                    w - 12
            );

            if (spirits.isEmpty()) {
                smallText(
                        g,
                        "No spirits stored",
                        cx,
                        y + headerH + 12,
                        MUTED,
                        0.80f
                );
            }

            for (int i = 0; i < spirits.size(); i++) {

                final int index = i;

                CsmSpiritData spirit =
                        spirits.get(i);

                int rowY =
                        y + headerH + i * rowH;

                // Spirit name
                String displayName =
                        shorten(
                                spirit.shortLabel(),
                                145
                        );

                smallText(
                        g,
                        displayName,
                        cx,
                        rowY + 2,
                        spirit.fingers > 0
                                ? GOLD
                                : TEXT,
                        0.82f
                );

                // Count and current state
                String status;

                if (spirit.isUnique) {
                    status = "Unique";

                    if (spirit.fingers > 0) {
                        status += " | " +
                                spirit.fingers +
                                "/20 Fingers";
                    }
                } else {
                    status = "Count: " + spirit.count;
                }

                smallText(
                        g,
                        status,
                        cx,
                        rowY + 12,
                        MUTED,
                        0.72f
                );

                // =================================
                // RENAME MODE
                // =================================

                if (renameIndex == i) {

                    if (rowY + 24 >= contentTop
                            && rowY + 37 <= contentBottom) {

                        renameField.setX(x + 4);
                        renameField.setY(rowY + 24);
                        renameField.setWidth(w - 37);

                        renameField.visible = true;
                    }

                    drawButton(
                            g, mx, my,
                            x + w - 31,
                            rowY + 24,
                            27,
                            12,
                            "Save",
                            true,
                            this::saveRename
                    );

                } else {

                    // =================================
                    // FINGER BEARER / FEED FINGER
                    // =================================

                    if (!spirit.isUnique) {

                        drawButton(
                                g, mx, my,
                                x + 4,
                                rowY + 24,
                                59,
                                12,
                                "Finger Bearer",
                                spirit.count > 0,
                                () -> ModNetwork.CHANNEL
                                        .sendToServer(
                                                new PacketCsmMarkUnique(index)
                                        )
                        );

                    } else {

                        drawButton(
                                g, mx, my,
                                x + 4,
                                rowY + 24,
                                59,
                                12,
                                spirit.fingers >= 20
                                        ? " Max Fingers"
                                        : " Feed Finger",
                                spirit.fingers < 20,
                                () -> ModNetwork.CHANNEL
                                        .sendToServer(
                                                new PacketCsmFeedFinger(index)
                                        )
                        );
                    }

                    // =================================
                    // SPARE / CONSUME TOGGLE
                    // =================================

                    drawButton(
                            g, mx, my,
                            x + 65,
                            rowY + 24,
                            27,
                            12,
                            spirit.spared ? " Keep" : " Spare",
                            true,
                            () -> ModNetwork.CHANNEL
                                    .sendToServer(
                                            new PacketCsmToggleSpared(index)
                                    )
                    );

                    // =================================
                    // RENAME BUTTON
                    // =================================

                    drawButton(
                            g, mx, my,
                            x + 94,
                            rowY + 24,
                            22,
                            12,
                            "Edit",
                            true,
                            () -> startRename(
                                    index,
                                    spirit.displayName
                            )
                    );
                }
            }

            y += listH + 6;
        }

        contentHeight =
                y + scrollOffset - contentTop + 8;

        g.disableScissor();

        // Fixed dock, not part of scroll area
        renderDock(g, mx, my);

        // Render text field inside content clip.
        if (renameField.visible) {

            g.enableScissor(
                    left + 4,
                    contentTop,
                    left + PHONE_W - 4,
                    contentBottom
            );

            renameField.render(
                    g,
                    mx,
                    my,
                    partialTick
            );

            g.disableScissor();
        }
    }

    // =========================================
    // CARD RENDERING
    // =========================================

    private void drawCard(
            GuiGraphics g,
            int x,
            int y,
            int w,
            int h
    ) {
        g.fill(
                x, y,
                x + w, y + h,
                CARD
        );

        g.fill(x, y, x + w, y + 1, PURPLE);
        g.fill(x, y + h - 1, x + w, y + h, PURPLE);
        g.fill(x, y, x + 1, y + h, PURPLE);
        g.fill(x + w - 1, y, x + w, y + h, PURPLE);
    }

    private void divider(
            GuiGraphics g,
            int x,
            int y,
            int w
    ) {
        g.fill(
                x, y,
                x + w, y + 1,
                DARK_PURPLE
        );
    }

    // =========================================
    // BUTTON RENDERING
    // =========================================

    private void drawButton(
            GuiGraphics g,
            int mx,
            int my,
            int x,
            int y,
            int w,
            int h,
            String label,
            boolean enabled,
            Runnable action
    ) {
        boolean hovered =
                inside(mx, my, x, y, w, h);

        int color = !enabled
                ? DISABLED
                : hovered ? HOVER : PURPLE;

        g.fill(
                x,
                y,
                x + w,
                y + h,
                color
        );

        // White button text, lowered to match Copy.
        smallText(
                g,
                label,
                x + w / 2,
                y + 4,
                TEXT,
                0.65f
        );

        // Only register clickable, fully visible buttons.
        if (enabled
                && y >= contentTop
                && y + h <= contentBottom) {

            buttons.add(
                    new Hitbox(
                            x, y, w, h, action
                    )
            );
        }
    }

    private void smallText(
            GuiGraphics g,
            String text,
            int x,
            int y,
            int color,
            float scale
    ) {
        var pose = g.pose();

        pose.pushPose();
        pose.scale(scale, scale, 1.0f);

        g.drawCenteredString(
                font,
                text,
                (int) (x / scale),
                (int) (y / scale),
                color
        );

        pose.popPose();
    }

    private String shorten(
            String text,
            int maxWidth
    ) {
        if (text == null) return "";

        if (font.width(text) <= maxWidth) {
            return text;
        }

        return font.plainSubstrByWidth(
                text,
                Math.max(
                        0,
                        maxWidth - font.width("...")
                )
        ) + "...";
    }

    // =========================================
    // RENAMING
    // =========================================

    private void startRename(
            int index,
            String oldName
    ) {
        renameIndex = index;

        renameField.setValue(
                oldName == null ? "" : oldName
        );

        renameField.moveCursorToEnd();
        renameField.setFocused(true);
        setFocused(renameField);
    }

    private void saveRename() {

        if (renameIndex < 0) return;

        String name =
                renameField.getValue().strip();

        if (!name.isEmpty()) {

            ModNetwork.CHANNEL.sendToServer(
                    new PacketCsmRenameSpirit(
                            renameIndex,
                            name
                    )
            );
        }

        cancelRename();
    }

    private void cancelRename() {

        renameIndex = -1;

        if (renameField != null) {
            renameField.setFocused(false);
            renameField.visible = false;
        }

        setFocused(null);
    }

    // =========================================
    // SCROLLING
    // =========================================

    @Override
    public boolean mouseScrolled(
            double mx,
            double my,
            double delta
    ) {
        if (!inside(
                mx, my,
                left + 4,
                contentTop,
                PHONE_W - 8,
                contentBottom - contentTop
        )) {
            return false;
        }

        int maximum = Math.max(
                0,
                contentHeight -
                        (contentBottom - contentTop)
        );

        scrollOffset = Math.max(
                0,
                Math.min(
                        maximum,
                        scrollOffset -
                                (int) (delta * 15)
                )
        );

        return true;
    }

    // =========================================
    // MOUSE INPUT
    // =========================================

    @Override
    public boolean mouseClicked(
            double mx,
            double my,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(
                    mx, my, button
            );
        }

        if (renameField.visible
                && inside(
                mx, my,
                renameField.getX(),
                renameField.getY(),
                renameField.getWidth(),
                renameField.getHeight()
        )) {

            return super.mouseClicked(
                    mx, my, button
            );
        }

        for (Hitbox hit : buttons) {

            if (inside(
                    mx, my,
                    hit.x(),
                    hit.y(),
                    hit.w(),
                    hit.h()
            )) {

                hit.action().run();
                return true;
            }
        }

        int dockY = top + PHONE_H - 31;

        int dockX =
                left +
                        (PHONE_W -
                                (3 * DOCK_SIZE + 2 * DOCK_GAP)) / 2;

        // Messages
        if (inside(
                mx, my,
                dockX, dockY,
                DOCK_SIZE, DOCK_SIZE
        )) {

            Minecraft.getInstance().setScreen(
                    new PhonePlaceholderScreen(
                            "Messages",
                            "Quests & messages coming soon."
                    )
            );

            return true;
        }

        // Map
        if (inside(
                mx, my,
                dockX + DOCK_SIZE + DOCK_GAP,
                dockY,
                DOCK_SIZE,
                DOCK_SIZE
        )) {

            var player =
                    Minecraft.getInstance().player;

            boolean unlocked =
                    player != null
                            && player.getPersistentData()
                            .getBoolean(
                                    "minimap_unlocked"
                            );

            if (!unlocked) {

                Minecraft.getInstance().setScreen(
                        new PhoneMapScreen()
                );

            } else {

                ModNetwork.CHANNEL.sendToServer(
                        new PacketToggleMinimap()
                );
            }

            return true;
        }

        // Teleport
        if (inside(
                mx, my,
                dockX + 2 * (DOCK_SIZE + DOCK_GAP),
                dockY,
                DOCK_SIZE,
                DOCK_SIZE
        )) {

            Minecraft.getInstance().setScreen(
                    new PhoneTeleportScreen()
            );

            return true;
        }

        return super.mouseClicked(
                mx, my, button
        );
    }

    // =========================================
    // DOCK
    // =========================================

    private void renderDock(
            GuiGraphics g,
            int mx,
            int my
    ) {
        int y = top + PHONE_H - 31;

        int x = left +
                (PHONE_W -
                        (3 * DOCK_SIZE + 2 * DOCK_GAP)) / 2;

        drawDockIcon(
                g, ICON_MESSAGES,
                x, y, mx, my
        );

        drawDockIcon(
                g, ICON_MAP,
                x + DOCK_SIZE + DOCK_GAP,
                y, mx, my
        );

        drawDockIcon(
                g, ICON_TELEPORT,
                x + 2 * (DOCK_SIZE + DOCK_GAP),
                y, mx, my
        );
    }

    private void drawDockIcon(
            GuiGraphics g,
            ResourceLocation texture,
            int x,
            int y,
            int mx,
            int my
    ) {
        if (inside(
                mx, my,
                x, y,
                DOCK_SIZE, DOCK_SIZE
        )) {

            g.fill(
                    x - 2,
                    y - 2,
                    x + DOCK_SIZE + 2,
                    y + DOCK_SIZE + 2,
                    0x44FFFFFF
            );
        }

        g.blit(
                texture,
                x, y,
                0, 0,
                DOCK_SIZE,
                DOCK_SIZE,
                DOCK_SIZE,
                DOCK_SIZE
        );
    }

    private boolean inside(
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

    private String getTime() {

        Minecraft mc =
                Minecraft.getInstance();

        if (mc.level == null) {
            return "00:00";
        }

        long time =
                mc.level.getDayTime() % 24000L;

        int minutes =
                (int) (time * 1440L / 24000L);

        return String.format(
                "%02d:%02d",
                (minutes / 60 + 6) % 24,
                minutes % 60
        );
    }

    // =========================================
    // KEY INPUT
    // =========================================

    @Override
    public boolean keyPressed(
            int key,
            int scanCode,
            int modifiers
    ) {

        if (renameIndex >= 0) {

            if (key == 257 || key == 335) {
                saveRename();
                return true;
            }

            if (key == 256) {
                cancelRename();
                return true;
            }

            return super.keyPressed(
                    key,
                    scanCode,
                    modifiers
            );
        }

        if (key == 256
                || key == 69
                || ClientEvents.OPEN_PHONE.matches(
                key, scanCode
        )) {

            Minecraft.getInstance().setScreen(
                    new PhoneScreen()
            );

            return true;
        }

        return super.keyPressed(
                key,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Hitbox(
            int x,
            int y,
            int w,
            int h,
            Runnable action
    ) {}
}
