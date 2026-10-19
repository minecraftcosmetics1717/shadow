package com.ghosts.abyss.gui;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class JslopButton extends Button {
   private static final int COL_FILL = -871757800;
   private static final int COL_FILL_HOVER = -870838214;
   private static final int COL_FILL_DISABLED = 1426720792;
   private static final int COL_BORDER = -11916168;
   private static final int COL_BORDER_HOVER = -7708736;
   private static final int COL_BORDER_DIM = 1144199765;
   private static final int COL_TEXT = -4473925;
   private static final int COL_TEXT_HOVER = -1118482;
   private static final int COL_TEXT_DISABLED = -11184811;
   private static final int COL_INNER_LINE = 711606176;
   private static final int TEXT_PAD = 5;

   private Integer overrideBorder = null;
   private Integer overrideBorderHover = null;
   private Integer overrideFill = null;
   private Integer overrideFillHover = null;
   private Integer overrideInnerLine = null;

   private int scrollOffset = 0;
   private int scrollPauseTimer = 0;
   private static final int SCROLL_PAUSE = 30;
   private static final int SCROLL_SPEED = 1;

   void applyColors(int border, int borderHover, int fill, int fillHover, int innerLine) {
      this.overrideBorder = border;
      this.overrideBorderHover = borderHover;
      this.overrideFill = fill;
      this.overrideFillHover = fillHover;
      this.overrideInnerLine = innerLine;
   }

   protected JslopButton(int x, int y, int w, int h, Component label, Button.OnPress onPress) {
      super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
   }

   public static Builder jslopBuilder(Component label, Button.OnPress onPress) {
      return new Builder(label, onPress);
   }

   public void tick() {
      Font font = Minecraft.getInstance().font;
      String text = this.getMessage().getString();
      int maxWidth = this.getWidth() - 10;
      int textWidth = font.width(text);

      if (this.isHoveredOrFocused() && textWidth > maxWidth) {
         int maxScroll = textWidth - maxWidth;
         if (this.scrollPauseTimer > 0) {
            --this.scrollPauseTimer;
            if (this.scrollPauseTimer == 0 && this.scrollOffset >= maxScroll) {
               this.scrollOffset = 0;
               this.scrollPauseTimer = 15;
            }
         } else {
            ++this.scrollOffset;
            if (this.scrollOffset >= maxScroll) {
               this.scrollOffset = maxScroll;
               this.scrollPauseTimer = 30;
            }
         }
      } else {
         this.scrollOffset = 0;
         this.scrollPauseTimer = 0;
      }
   }

   @Override
   public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
      boolean hovered = this.isHoveredOrFocused() && this.active;
      boolean disabled = !this.active;

      int fillBase = this.overrideFill != null ? this.overrideFill : COL_FILL;
      int fillHover = this.overrideFillHover != null ? this.overrideFillHover : COL_FILL_HOVER;
      int borderBase = this.overrideBorder != null ? this.overrideBorder : COL_BORDER;
      int borderHov = this.overrideBorderHover != null ? this.overrideBorderHover : COL_BORDER_HOVER;
      int innerLine = this.overrideInnerLine != null ? this.overrideInnerLine : COL_INNER_LINE;

      int borderDim = this.overrideBorder != null ? (1140850688 | (this.overrideBorder & 0xFFFFFF)) : COL_BORDER_DIM;
      int fill = disabled ? COL_FILL_DISABLED : (hovered ? fillHover : fillBase);
      int border = disabled ? borderDim : (hovered ? borderHov : borderBase);
      int text = disabled ? COL_TEXT_DISABLED : (hovered ? COL_TEXT_HOVER : COL_TEXT);

      int x = this.getX();
      int y = this.getY();
      int w = this.getWidth();
      int h = this.getHeight();

      // Outer panel & borders
      g.fill(x, y, x + w, y + h, fill);
      g.fill(x, y, x + w, y + 1, border);
      g.fill(x, y + h - 1, x + w, y + h, border);
      g.fill(x, y, x + 1, y + h, border);
      g.fill(x + w - 1, y, x + w, y + h, border);

      if (!disabled) {
         g.fill(x + 1, y + 1, x + w - 1, y + 2, innerLine);
         g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, innerLine);
      }

      Font font = Minecraft.getInstance().font;
      String label = this.getMessage().getString();
      int maxWidth = w - 10;
      int textWidth = font.width(label);

      g.enableScissor(x + 5, y, x + w - 5, y + h);

      int textX;
      if (textWidth <= maxWidth) {
         textX = x + (w - textWidth) / 2;
      } else {
         textX = x + 5 - this.scrollOffset;
      }

      int textY = y + (h - 9) / 2;
      g.drawString(font, label, textX, textY, text, false);

      g.disableScissor();
   }

   public static String truncate(Font font, String text, int maxWidth) {
      if (font.width(text) <= maxWidth) {
         return text;
      } else {
         String ellipsis = "...";
         for (int ellipsisWidth = font.width(ellipsis); !text.isEmpty() && font.width(text) + ellipsisWidth > maxWidth; text = text.substring(0, text.length() - 1)) {
         }
         return text + ellipsis;
      }
   }

   public static class Builder {
      private final Component label;
      private final Button.OnPress onPress;
      private int x;
      private int y;
      private int w = 150;
      private int h = 20;
      private Integer border = null;
      private Integer borderHover = null;
      private Integer fill = null;
      private Integer fillHover = null;
      private Integer innerLine = null;

      public Builder(Component label, Button.OnPress onPress) {
         this.label = label;
         this.onPress = onPress;
      }

      public Builder bounds(int x, int y, int w, int h) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         return this;
      }

      public Builder colors(int border, int borderHover, int fill, int fillHover, int innerLine) {
         this.border = border;
         this.borderHover = borderHover;
         this.fill = fill;
         this.fillHover = fillHover;
         this.innerLine = innerLine;
         return this;
      }

      public JslopButton build() {
         JslopButton btn = new JslopButton(this.x, this.y, this.w, this.h, this.label, this.onPress);
         if (this.border != null) {
            btn.applyColors(this.border, this.borderHover, this.fill, this.fillHover, this.innerLine);
         }
         return btn;
      }
   }
}