package com.ghosts.abyss;

import com.ghosts.abyss.gui.AscendantTab;
import com.ghosts.abyss.gui.BlackFlashTab;
import com.ghosts.abyss.gui.CsmTab;
import com.ghosts.abyss.gui.DomainTab;
import com.ghosts.abyss.gui.IMenuTab;
import com.ghosts.abyss.gui.RctTab;
import com.ghosts.abyss.gui.YutaTab;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdvancedJujutsuScreen extends Screen {
   private static final int COL_BG = -1157627904;
   private static final int COL_PANEL = -15592932;
   private static final int COL_BORDER = -9814112;
   private static final int COL_TAB_IDLE = -14803410;
   private static final int COL_TAB_TEXT = -1;
   private static final int PANEL_W = 280;
   private static final int PANEL_H = 230;
   private static final int BORDER = 1;
   private static final int TAB_H = 20;
   private static final int TITLE_H = 22;
   private static final int SIDE_TAB_W = 70;

   private final List<IMenuTab> mainTabs = List.of(new DomainTab(), new BlackFlashTab(), new RctTab(), new AscendantTab());
   private final YutaTab yutaTab = new YutaTab();
   private final CsmTab csmTab = new CsmTab();
   private int activeTab = 0;
   private int px;
   private int py;
   private final List<int[]> tabBounds = new ArrayList<>();

   public AdvancedJujutsuScreen() {
      this(0);
   }

   public AdvancedJujutsuScreen(int startTab) {
      super(Component.translatable("screen.abyss.advanced_menu"));
      this.activeTab = startTab;
   }

   @Override
   protected void init() {
      super.init();
      this.px = (this.width - PANEL_W) / 2;
      this.py = (this.height - PANEL_H) / 2 + 11;
      this.rebuildTabWidgets();
   }

   private void rebuildTabWidgets() {
      this.clearWidgets();
      this.tabBounds.clear();
      int tabCount = this.mainTabs.size();
      int tabW = PANEL_W / tabCount;

      for (int i = 0; i < tabCount; ++i) {
         int tx = this.px + i * tabW;
         int ty = this.py - TAB_H;
         this.tabBounds.add(new int[]{tx, ty, tabW, TAB_H});
      }

      IMenuTab current = this.getCurrentTab();
      List<AbstractWidget> widgets = current.buildWidgets(this.px, this.py, PANEL_W, PANEL_H);
      widgets.forEach(this::addRenderableWidget);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      for (int i = 0; i < this.tabBounds.size(); ++i) {
         int[] b = this.tabBounds.get(i);
         if (mx >= (double) b[0] && mx <= (double) (b[0] + b[2]) && my >= (double) b[1] && my <= (double) (b[1] + b[3])) {
            if (this.activeTab != i) {
               this.getCurrentTab().onClose();
               this.activeTab = i;
               this.rebuildTabWidgets();
            }
            return true;
         }
      }

      if (YutaTab.isLocalPlayerYuta()) {
         int sideX = this.px + PANEL_W;
         int sideY = this.py - TAB_H - 1;
         if (mx >= (double) sideX && mx <= (double) (sideX + SIDE_TAB_W) && my >= (double) sideY && my <= (double) (sideY + TAB_H + 1)) {
            if (this.activeTab != -1) {
               this.getCurrentTab().onClose();
               this.activeTab = -1;
               this.rebuildTabWidgets();
            }
            return true;
         }
      }

      if (CsmTab.isLocalPlayerCsm()) {
         int csmOffset = YutaTab.isLocalPlayerYuta() ? 23 : 0;
         int sideX = this.px + PANEL_W;
         int sideY = this.py - TAB_H - 1 + csmOffset;
         if (mx >= (double) sideX && mx <= (double) (sideX + SIDE_TAB_W) && my >= (double) sideY && my <= (double) (sideY + TAB_H + 1)) {
            if (this.activeTab != -2) {
               this.getCurrentTab().onClose();
               this.activeTab = -2;
               this.rebuildTabWidgets();
            }
            return true;
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      g.fill(0, 0, this.width, this.height, COL_BG);
      IMenuTab active = this.getCurrentTab();
      int themeBorder = active.borderColor();
      int themePanel = active.panelColor();
      int themeTabActive = active.tabActiveColor();
      int themeTabHover = active.tabHoverColor();

      g.drawCenteredString(this.font, Component.translatable("screen.abyss.advanced_menu"), this.px + 140, this.py - TAB_H - TITLE_H + 4, active.titleColor());
      this.drawOutline(g, this.px - 1, this.py - TAB_H - 1, this.px + PANEL_W + 1, this.py + PANEL_H + 1, themeBorder);

      int tabCount = this.mainTabs.size();
      int tabW = PANEL_W / tabCount;

      for (int i = 0; i < tabCount; ++i) {
         int[] b = this.tabBounds.get(i);
         boolean isActive = i == this.activeTab;
         boolean hover = mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3];
         int fill = isActive ? themeTabActive : (hover ? themeTabHover : COL_TAB_IDLE);

         g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], fill);
         if (i < tabCount - 1) {
            g.fill(b[0] + b[2] - 1, b[1], b[0] + b[2], b[1] + b[3], themeBorder);
         }

         g.drawCenteredString(this.font, this.mainTabs.get(i).getTitle(), b[0] + b[2] / 2, b[1] + (TAB_H - 9) / 2, COL_TAB_TEXT);
      }

      if (YutaTab.isLocalPlayerYuta()) {
         int sideX = this.px + PANEL_W;
         int sideY = this.py - TAB_H - 1;
         boolean sideActive = this.activeTab == -1;
         boolean sideHover = mx >= sideX && mx <= sideX + SIDE_TAB_W && my >= sideY && my <= sideY + TAB_H + 1;
         int sideFill = sideActive ? this.yutaTab.tabActiveColor() : (sideHover ? this.yutaTab.tabHoverColor() : COL_TAB_IDLE);

         g.fill(sideX, sideY, sideX + SIDE_TAB_W, this.py, sideFill);
         g.fill(sideX, sideY, sideX + 1, this.py, this.yutaTab.borderColor());
         g.fill(sideX, sideY, sideX + SIDE_TAB_W, sideY + 1, this.yutaTab.borderColor());
         g.fill(sideX + SIDE_TAB_W - 1, sideY, sideX + SIDE_TAB_W, this.py, this.yutaTab.borderColor());

         g.drawCenteredString(this.font, Component.literal("Copy"), sideX + 35, this.py - TAB_H + (TAB_H - 9) / 2, COL_TAB_TEXT);
      }

      if (CsmTab.isLocalPlayerCsm()) {
         int csmOffset = YutaTab.isLocalPlayerYuta() ? 23 : 0;
         int sideX = this.px + PANEL_W;
         int sideY = this.py - TAB_H - 1 + csmOffset;
         boolean csmActive = this.activeTab == -2;
         boolean csmHover = mx >= sideX && mx <= sideX + SIDE_TAB_W && my >= sideY && my <= sideY + TAB_H + 1;
         int csmFill = csmActive ? this.csmTab.tabActiveColor() : (csmHover ? this.csmTab.tabHoverColor() : COL_TAB_IDLE);

         g.fill(sideX, sideY, sideX + SIDE_TAB_W, sideY + TAB_H + 1, csmFill);
         g.fill(sideX, sideY, sideX + 1, sideY + TAB_H + 1, this.csmTab.borderColor());
         g.fill(sideX, sideY, sideX + SIDE_TAB_W, sideY + 1, this.csmTab.borderColor());
         g.fill(sideX + SIDE_TAB_W - 1, sideY, sideX + SIDE_TAB_W, sideY + TAB_H + 1, this.csmTab.borderColor());

         g.drawCenteredString(this.font, Component.literal("CSM"), sideX + 35, sideY + (TAB_H - 9) / 2, -3399134);
      }

      g.fill(this.px, this.py, this.px + PANEL_W, this.py + PANEL_H, themePanel);
      active.render(g, this.px, this.py, PANEL_W, PANEL_H, mx, my, pt);

      g.enableScissor(this.px, this.py, this.px + PANEL_W, this.py + PANEL_H);
      super.render(g, mx, my, pt);
      g.disableScissor();

      active.renderTooltips(g, this.px, this.py, PANEL_W, PANEL_H, mx, my);

      int scrollOffset = active.getScrollOffset();
      int contentHeight = active.getContentHeight();
      if (contentHeight > PANEL_H) {
         int barX = this.px + PANEL_W - 5;
         int barY1 = this.py + 3;
         int barY2 = this.py + PANEL_H - 3;
         int barH = barY2 - barY1;

         g.fill(barX, barY1, barX + 4, barY2, 872415231);
         int maxScroll = contentHeight - PANEL_H;
         int thumbH = Math.max(14, barH * PANEL_H / contentHeight);
         int thumbY = barY1 + (barH - thumbH) * scrollOffset / maxScroll;
         g.fill(barX, thumbY, barX + 4, thumbY + thumbH, themeBorder);
      }
   }

   @Override
   public void renderBackground(GuiGraphics g) {
      // Overridden to avoid rendering default gradient background
   }

   private void drawOutline(GuiGraphics g, int x1, int y1, int x2, int y2, int colour) {
      g.fill(x1, y1, x2, y1 + 1, colour);
      g.fill(x1, y2 - 1, x2, y2, colour);
      g.fill(x1, y1, x1 + 1, y2, colour);
      g.fill(x2 - 1, y1, x2, y2, colour);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return this.getCurrentTab().keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char c, int modifiers) {
      return this.getCurrentTab().charTyped(c, modifiers) || super.charTyped(c, modifiers);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double delta) {
      return this.getCurrentTab().onMouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
   }

   @Override
   public void tick() {
      super.tick();
      this.getCurrentTab().tick();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private IMenuTab getCurrentTab() {
      if (this.activeTab == -1) {
         return this.yutaTab;
      } else if (this.activeTab == -2) {
         return this.csmTab;
      } else {
         return this.mainTabs.get(this.activeTab);
      }
   }
}