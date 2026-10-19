package com.ghosts.abyss.gui;

import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketPactResponse;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PactDialogueScreen extends Screen {
   private static final int COL_BG = -872415232;
   private static final int COL_BORDER = -9814112;
   private static final int COL_PANEL = -15592932;
   private static final int COL_TITLE = -5537836;
   private static final int COL_BODY = -3355444;
   private static final int COL_WARNING = -48060;
   private static final int PANEL_W = 260;
   private static final int PANEL_H = 160;

   public PactDialogueScreen() {
      super(Component.literal(""));
   }

   @Override
   protected void init() {
      super.init();
      int px = (this.width - PANEL_W) / 2;
      int py = (this.height - PANEL_H) / 2;
      int btnW = 110;
      int btnH = 20;
      int btnY = py + PANEL_H - 30;

      // Cancel Button
      this.addRenderableWidget(JslopButton.jslopBuilder(
              Component.translatable("button.abyss.pact_cancel"),
              (btn) -> {
                 ModNetwork.CHANNEL.sendToServer(new PacketPactResponse(false));
                 this.onClose();
              }
      ).bounds(px + 10, btnY, btnW, btnH).build());

      // Confirm Button
      this.addRenderableWidget(JslopButton.jslopBuilder(
              Component.translatable("button.abyss.pact_confirm"),
              (btn) -> {
                 ModNetwork.CHANNEL.sendToServer(new PacketPactResponse(true));
                 this.onClose();
              }
      ).bounds(px + PANEL_W - btnW - 10, btnY, btnW, btnH).build());
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      // Dark background overlay
      g.fill(0, 0, this.width, this.height, COL_BG);

      int px = (this.width - PANEL_W) / 2;
      int py = (this.height - PANEL_H) / 2;

      // Dialogue panel background & borders
      g.fill(px, py, px + PANEL_W, py + PANEL_H, COL_PANEL);
      g.fill(px - 1, py - 1, px + PANEL_W + 1, py, COL_BORDER);
      g.fill(px - 1, py + PANEL_H, px + PANEL_W + 1, py + PANEL_H + 1, COL_BORDER);
      g.fill(px - 1, py - 1, px, py + PANEL_H + 1, COL_BORDER);
      g.fill(px + PANEL_W, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, COL_BORDER);

      // Title & divider
      g.drawCenteredString(this.font, Component.translatable("dialogue.abyss.pact_title"), px + PANEL_W / 2, py + 10, COL_TITLE);
      g.fill(px + 10, py + 22, px + PANEL_W - 10, py + 23, COL_BORDER);

      int ty = py + 30;
      int lineSpacing = 10;

      // Render dialogue body text lines
      for (int i = 1; i <= 5; ++i) {
         Component line = Component.translatable("dialogue.abyss.pact_line_" + i);
         g.drawCenteredString(this.font, line, px + PANEL_W / 2, ty, COL_BODY);
         ty += lineSpacing;
      }

      ty += 4;
      g.drawCenteredString(this.font, Component.translatable("dialogue.abyss.pact_warning"), px + PANEL_W / 2, ty, COL_WARNING);

      super.render(g, mx, my, pt);
   }

   @Override
   public void renderBackground(GuiGraphics g) {
      // Custom background rendered in render()
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) { // ESC Key
         ModNetwork.CHANNEL.sendToServer(new PacketPactResponse(false));
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }
}