
package com.ghosts.abyss.client.phone;

import com.ghosts.abyss.client.ClientEvents;
import com.ghosts.abyss.gui.YutaTab;
import com.ghosts.abyss.network.*;
import com.ghosts.abyss.yuta.ClientYutaTechniqueCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class PhoneCopyScreen extends Screen {
    private static final ResourceLocation PHONE_BG =
            new ResourceLocation("abyss", "textures/gui/phone/phone_bg.png");
    private static final ResourceLocation ICON_MESSAGES =
            new ResourceLocation("abyss", "textures/gui/phone/icon_messages.png");
    private static final ResourceLocation ICON_MAP =
            new ResourceLocation("abyss", "textures/gui/phone/icon_map.png");
    private static final ResourceLocation ICON_TELEPORT =
            new ResourceLocation("abyss", "textures/gui/phone/icon_teleport.png");

    private static final int PHONE_W = 140, PHONE_H = 240;
    private static final int DOCK_SIZE = 24, DOCK_GAP = 8;
    private static final int PINK = 0xFFFF00B2;
    private static final int HOVER = 0xFFFF66D4;
    private static final int DARK_PINK = 0xFF590037;
    private static final int CARD = 0xFF110511;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int MUTED = 0xFF8B949E;
    private static final int DISABLED = 0xFF30363D;

    private int left, top, contentTop, contentBottom;
    private int scrollOffset, contentHeight;
    private int renameIndex = -1;
    private EditBox renameField;
    private final List<Hitbox> buttons = new ArrayList<>();

    public PhoneCopyScreen() {
        super(Component.literal("Copy"));
    }

    @Override
    protected void init() {
        left = (width - PHONE_W) / 2;
        top = (height - PHONE_H) / 2;
        contentTop = top + 42;
        contentBottom = top + PHONE_H - 36;
        scrollOffset = 0;
        renameIndex = -1;

        renameField = new EditBox(font, left + 14, contentTop,
                PHONE_W - 42, 12, Component.literal("Rename technique"));
        renameField.setMaxLength(50);
        renameField.setTextColor(TEXT);
        renameField.visible = false;
        addRenderableWidget(renameField);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        g.blit(PHONE_BG, left, top, 0, 0,
                PHONE_W, PHONE_H, PHONE_W, PHONE_H);
        g.drawString(font, getTime(), left + 8, top + 10, TEXT, false);
        g.drawCenteredString(font, "Copy", left + PHONE_W / 2, top + 28, TEXT);

        buttons.clear();
        renameField.visible = false;
        List<ClientYutaTechniqueCache.Entry> entries = ClientYutaTechniqueCache.getAll();
        if (renameIndex >= entries.size()) cancelRename();

        int x = left + 10;
        int w = PHONE_W - 20;
        int cx = left + PHONE_W / 2;
        int y = contentTop + 4 - scrollOffset;

        g.enableScissor(left + 4, contentTop, left + PHONE_W - 4, contentBottom);
        if (!YutaTab.isLocalPlayerYuta()) {
            drawCard(g, x, y, w, 42);
            g.drawCenteredString(font, "Yuta CT required", cx, y + 10, PINK);
            smallText(g, "Obtain Copy (CT 5)", cx, y + 25, MUTED, 0.8f);
            y += 48;
        } else {
            var player = Minecraft.getInstance().player;
            int xp = player == null ? 0 : player.experienceLevel;
            int selected = ClientYutaTechniqueCache.getDomainIdx();

            g.drawCenteredString(font, "XP Levels: " + xp, cx, y, PINK);
            y += 14;

            // Registration panel.
            drawCard(g, x, y, w, 57);
            g.drawCenteredString(font, "Register Technique", cx, y + 5, PINK);
            divider(g, x + 6, y + 17, w - 12);
            ItemStack held = getHeldCopyItem();
            if (held.isEmpty()) {
                smallText(g, "Hold a Copied Technique", cx, y + 23, MUTED, 0.75f);
            } else {
                smallText(g, shorten(held.getHoverName().getString(), 145),
                        cx, y + 24, TEXT, 0.82f);
            }
            boolean canRegister = !held.isEmpty() && xp >= 1 && entries.size() < 40;
            drawButton(g, mx, my, x + 12, y + 39, w - 24, 13,
                    entries.size() >= 40 ? "Library Full" : "Register",
                    canRegister, () -> {
                        var p = Minecraft.getInstance().player;
                        if (p != null) ModNetwork.CHANNEL.sendToServer(
                                new PacketRegisterYutaTechnique(p.getInventory().selected));
                    });
            y += 63;

            // Acquisition above the library, so it remains easy to reach.
            boolean disabled = player != null
                    && player.getPersistentData().getBoolean("jslop_disable_copy");
            drawCard(g, x, y, w, 51);
            g.drawCenteredString(font, "Copy Acquisition", cx, y + 5, PINK);
            divider(g, x + 6, y + 17, w - 12);
            smallText(g, disabled ? "Disabled" : "Enabled",
                    cx, y + 21, disabled ? MUTED : TEXT, 0.82f);
            drawButton(g, mx, my, x + 9, y + 33, w - 18, 13,
                    disabled ? "Enable New Copies" : "Disable New Copies",
                    true, () -> ModNetwork.CHANNEL.sendToServer(
                            new PacketToggleCopyOnHit()));
            y += 57;

            // One growing library panel: header and all technique rows.
            // When there are many entries, the phone's scroll clips the panel.
            final int rowHeight = 33;
            final int headerHeight = 24;
            final int listHeight = headerHeight + Math.max(1, entries.size()) * rowHeight + 6;
            drawCard(g, x, y, w, listHeight);
            g.drawCenteredString(font, "Registered: " + entries.size() + "/40",
                    cx, y + 5, PINK);
            divider(g, x + 6, y + 19, w - 12);

            if (entries.isEmpty()) {
                smallText(g, "No saved techniques", cx, y + headerHeight + 8,
                        MUTED, 0.82f);
            }

            for (int i = 0; i < entries.size(); i++) {
                final int index = i;
                ClientYutaTechniqueCache.Entry entry = entries.get(i);
                boolean active = selected == i;
                int rowY = y + headerHeight + i * rowHeight;

                smallText(g, shorten(entry.name(), 145), cx, rowY + 2,
                        active ? PINK : TEXT, 0.85f);

                if (renameIndex == i) {
                    // Keep input within the scrolling phone area.
                    if (rowY + 16 >= contentTop && rowY + 29 <= contentBottom) {
                        renameField.setX(x + 5);
                        renameField.setY(rowY + 16);
                        renameField.setWidth(w - 37);
                        renameField.visible = true;
                    }
                    drawButton(g, mx, my, x + w - 30, rowY + 16,
                            25, 12, "Save", true, this::saveRename);
                } else {
                    drawButton(g, mx, my, x + 4, rowY + 16,
                            60, 12, active ? "Sure-Hit" : "Set Domain",
                            !active, () -> ModNetwork.CHANNEL.sendToServer(
                                    new PacketYutaDomainSelect(index)));
                    drawButton(g, mx, my, x + 66, rowY + 16,
                            27, 12, " Edit", true,
                            () -> startRename(index, entry.name()));
                    drawButton(g, mx, my, x + 95, rowY + 16,
                            21, 12, " X", true, () -> {
                                cancelRename();
                                ModNetwork.CHANNEL.sendToServer(
                                        new PacketRemoveYutaTechnique(index));
                            });
                }
            }
            y += listHeight + 6;
        }

        contentHeight = y + scrollOffset - contentTop + 8;
        g.disableScissor();
        drawDock(g, mx, my);

        // EditBox is a normal Screen child; render it only when visible.
        if (renameField.visible) {
            g.enableScissor(left + 4, contentTop,
                    left + PHONE_W - 4, contentBottom);
            renameField.render(g, mx, my, partialTick);
            g.disableScissor();
        }
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD);
        g.fill(x, y, x + w, y + 1, PINK);
        g.fill(x, y + h - 1, x + w, y + h, PINK);
        g.fill(x, y, x + 1, y + h, PINK);
        g.fill(x + w - 1, y, x + w, y + h, PINK);
    }

    private void divider(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, DARK_PINK);
    }

    private void drawButton(GuiGraphics g, int mx, int my,
                            int x, int y, int w, int h,
                            String label, boolean enabled, Runnable action) {
        boolean hovered = inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h,
                !enabled ? DISABLED : hovered ? HOVER : PINK);
        smallText(g, label, x + w / 2, y + 4,
                TEXT, 0.72f);
        if (enabled && y >= contentTop && y + h <= contentBottom)
            buttons.add(new Hitbox(x, y, w, h, action));
    }

    private void smallText(GuiGraphics g, String text,
                           int x, int y, int color, float scale) {
        var pose = g.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        g.drawCenteredString(font, text, (int) (x / scale),
                (int) (y / scale), color);
        pose.popPose();
    }

    private String shorten(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    private ItemStack getHeldCopyItem() {
        var p = Minecraft.getInstance().player;
        if (p == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("jujutsucraft", "copied_cursed_technique"));
        ItemStack held = p.getMainHandItem();
        return item != null && !held.isEmpty() && held.getItem() == item
                && held.getOrCreateTag().getDouble("skill") > 0.0D
                ? held : ItemStack.EMPTY;
    }

    private void startRename(int index, String currentName) {
        renameIndex = index;
        renameField.setValue(currentName);
        renameField.setFocused(true);
        setFocused(renameField);
    }

    private void saveRename() {
        if (renameIndex < 0) return;
        String name = renameField.getValue().strip();
        if (!name.isEmpty()) ModNetwork.CHANNEL.sendToServer(
                new PacketRenameYutaTechnique(renameIndex, name));
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

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!inside(mx, my, left + 4, contentTop,
                PHONE_W - 8, contentBottom - contentTop)) return false;
        int max = Math.max(0, contentHeight - (contentBottom - contentTop));
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) (delta * 15)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (renameField.visible && inside(mx, my,
                renameField.getX(), renameField.getY(),
                renameField.getWidth(), renameField.getHeight())) {
            return super.mouseClicked(mx, my, button);
        }
        for (Hitbox hit : buttons) {
            if (inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
                hit.action().run();
                return true;
            }
        }

        int dockY = top + PHONE_H - 31;
        int dockX = left + (PHONE_W - (3 * DOCK_SIZE + 2 * DOCK_GAP)) / 2;
        if (inside(mx, my, dockX, dockY, DOCK_SIZE, DOCK_SIZE)) {
            Minecraft.getInstance().setScreen(new PhonePlaceholderScreen(
                    "Messages", "Quests & messages coming soon."));
            return true;
        }
        if (inside(mx, my, dockX + DOCK_SIZE + DOCK_GAP,
                dockY, DOCK_SIZE, DOCK_SIZE)) {
            var p = Minecraft.getInstance().player;
            boolean unlocked = p != null
                    && p.getPersistentData().getBoolean("minimap_unlocked");
            if (!unlocked) Minecraft.getInstance().setScreen(new PhoneMapScreen());
            else ModNetwork.CHANNEL.sendToServer(new PacketToggleMinimap());
            return true;
        }
        if (inside(mx, my, dockX + 2 * (DOCK_SIZE + DOCK_GAP),
                dockY, DOCK_SIZE, DOCK_SIZE)) {
            Minecraft.getInstance().setScreen(new PhoneTeleportScreen());
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void drawDock(GuiGraphics g, int mx, int my) {
        int y = top + PHONE_H - 31;
        int x = left + (PHONE_W - (3 * DOCK_SIZE + 2 * DOCK_GAP)) / 2;
        drawDockIcon(g, ICON_MESSAGES, x, y, mx, my);
        drawDockIcon(g, ICON_MAP, x + DOCK_SIZE + DOCK_GAP, y, mx, my);
        drawDockIcon(g, ICON_TELEPORT, x + 2 * (DOCK_SIZE + DOCK_GAP), y, mx, my);
    }

    private void drawDockIcon(GuiGraphics g, ResourceLocation texture,
                              int x, int y, int mx, int my) {
        if (inside(mx, my, x, y, DOCK_SIZE, DOCK_SIZE))
            g.fill(x - 2, y - 2, x + DOCK_SIZE + 2, y + DOCK_SIZE + 2,
                    0x44FFFFFF);
        g.blit(texture, x, y, 0, 0, DOCK_SIZE, DOCK_SIZE, DOCK_SIZE, DOCK_SIZE);
    }

    private boolean inside(double mx, double my,
                           int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String getTime() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "00:00";
        long time = mc.level.getDayTime() % 24000L;
        int minutes = (int) (time * 1440L / 24000L);
        return String.format("%02d:%02d", (minutes / 60 + 6) % 24,
                minutes % 60);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (renameIndex >= 0) {
            if (key == 257 || key == 335) {
                saveRename();
                return true;
            }
            if (key == 256) {
                cancelRename();
                return true;
            }
            return super.keyPressed(key, scanCode, modifiers);
        }
        if (key == 256 || key == 69 || ClientEvents.OPEN_PHONE.matches(key, scanCode)) {
            Minecraft.getInstance().setScreen(new PhoneScreen());
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Hitbox(int x, int y, int w, int h, Runnable action) {}
}
