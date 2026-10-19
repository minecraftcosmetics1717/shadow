package com.ghosts.abyss.rika;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RikaInventoryScreen extends AbstractContainerScreen<RikaInventoryMenu> {
   private static final ResourceLocation CHEST_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
   private static final int ROWS = 3;

   public RikaInventoryScreen(RikaInventoryMenu menu, Inventory playerInv, Component title) {
      super(menu, playerInv, title);
      this.imageHeight = 168; // Height of the GUI window
      this.inventoryLabelY = this.imageHeight - 94; // Y position for the "Inventory" text label
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      int chestSectionHeight = 71;

      // Draw top half (Rika container area)
      graphics.blit(CHEST_TEXTURE, x, y, 0, 0, this.imageWidth, chestSectionHeight);
      // Draw bottom half (Player inventory area)
      graphics.blit(CHEST_TEXTURE, x, y + chestSectionHeight, 0, 126, this.imageWidth, 96);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }
}