
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
public class RikaInventoryScreen
        extends AbstractContainerScreen<RikaInventoryMenu> {

   // Custom Rika inventory texture
   private static final ResourceLocation RIKA_TEXTURE =
           new ResourceLocation(
                   "abyss",
                   "textures/gui/rika_inventory.png"
           );

   private static final int TEX_WIDTH = 256;
   private static final int TEX_HEIGHT = 256;

   public RikaInventoryScreen(
           RikaInventoryMenu menu,
           Inventory playerInv,
           Component title
   ) {
      super(menu, playerInv, title);

      // Match the existing Rika inventory dimensions
      this.imageWidth = 176;
      this.imageHeight = 168;

      // Text positions
      this.titleLabelX = 8;
      this.titleLabelY = 6;

      this.inventoryLabelX = 8;
      this.inventoryLabelY = 74;
   }

   @Override
   protected void renderBg(
           GuiGraphics graphics,
           float partialTick,
           int mouseX,
           int mouseY
   ) {
      RenderSystem.setShaderColor(
              1.0F, 1.0F, 1.0F, 1.0F
      );

      int x = this.leftPos;
      int y = this.topPos;

      // Draw custom inventory texture
      graphics.blit(
              RIKA_TEXTURE,
              x,
              y,
              0,
              0,
              this.imageWidth,
              this.imageHeight,
              TEX_WIDTH,
              TEX_HEIGHT
      );
   }

   @Override
   public void render(
           GuiGraphics graphics,
           int mouseX,
           int mouseY,
           float partialTick
   ) {
      this.renderBackground(graphics);

      super.render(
              graphics,
              mouseX,
              mouseY,
              partialTick
      );

      this.renderTooltip(
              graphics,
              mouseX,
              mouseY
      );
   }
}
