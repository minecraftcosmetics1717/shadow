package com.ghosts.abyss.client.phone;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PhonePlaceholderScreen extends Screen {

    private final String titleText;
    private final String bodyText;

    public PhonePlaceholderScreen(String titleText, String bodyText) {
        super(Component.literal(titleText));
        this.titleText = titleText;
        this.bodyText = bodyText;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Back"), b ->
                minecraft.setScreen(new PhoneScreen())
        ).bounds(width / 2 - 50, height / 2 + 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, titleText, width / 2, height / 2 - 20, 0xFFFFFF);
        g.drawCenteredString(font, bodyText, width / 2, height / 2, 0xAAAAAA);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}