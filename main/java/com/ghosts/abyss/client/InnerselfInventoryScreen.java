package com.ghosts.abyss.client;

import com.ghosts.abyss.menu.InnerselfInventoryMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class InnerselfInventoryScreen extends AbstractContainerScreen<InnerselfInventoryMenu> {

    // Reuse shadow texture or make a new one
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("abyss", "textures/gui/innerself_inventory.png");

    public InnerselfInventoryScreen(InnerselfInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }
}