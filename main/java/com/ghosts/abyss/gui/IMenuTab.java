package com.ghosts.abyss.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public interface IMenuTab {
   Component getTitle();

   List<AbstractWidget> buildWidgets(int var1, int var2, int var3, int var4);

   void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8);

   default void renderTooltips(GuiGraphics graphics, int panelX, int panelY, int panelW, int panelH, int mouseX, int mouseY) {
   }

   default boolean onMouseScrolled(double mx, double my, double delta) {
      return false;
   }

   default void tick() {
   }

   default void onClose() {
   }

   default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return false;
   }

   default boolean charTyped(char c, int modifiers) {
      return false;
   }

   default int panelColor() {
      return -15592932;
   }

   default int borderColor() {
      return -9814112;
   }

   default int tabActiveColor() {
      return -9814112;
   }

   default int tabHoverColor() {
      return -14017974;
   }

   default int titleColor() {
      return -5537836;
   }

   default int getScrollOffset() {
      return 0;
   }

   default int getContentHeight() {
      return 0;
   }
}
