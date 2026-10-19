package com.ghosts.abyss.gui;

import com.ghosts.abyss.AdvTechPoints;
import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.RefinementPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketRequestUnlockHWB;
import com.ghosts.abyss.network.PacketSpendRefinementPoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DomainTab implements IMenuTab {
   private static final int COL_HEADER = -5537836;
   private static final int COL_LABEL = -3355444;
   private static final int COL_VALUE = -1;
   private static final int COL_GOLD = -10496;
   private static final int COL_GREEN = -12255352;
   private static final int COL_BAR_BG = -14803410;
   private static final int COL_BAR_FILL = -8695873;
   private static final int COL_BAR_SHINE = -6459425;
   private static final int COL_BORDER = -9814112;
   private static final int COL_DIVIDER = -14017974;
   private static final int BAR_H = 8;
   private static final int BTN_W = 180;
   private static final int BTN_H = 20;
   private JslopButton spendButton;
   private JslopButton hwbUnlockButton;

   @Override
   public Component getTitle() {
      return Component.translatable("tab.abyss.domain");
   }

   @Override
   public List<AbstractWidget> buildWidgets(int px, int py, int pw, int ph) {
      List<AbstractWidget> widgets = new ArrayList<>();
      this.spendButton = JslopButton.jslopBuilder(
              Component.translatable("button.abyss.spend_point"),
              (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketSpendRefinementPoint())
      ).bounds(px + (pw - 180) / 2, py + 100, 180, 20).build();
      widgets.add(this.spendButton);

      this.hwbUnlockButton = JslopButton.jslopBuilder(
              Component.translatable("button.abyss.unlock_hwb"),
              (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketRequestUnlockHWB())
      ).bounds(px + (pw - 180) / 2, py + 195, 180, 20).build();
      widgets.add(this.hwbUnlockButton);

      return widgets;
   }

   @Override
   public void render(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my, float pt) {
      Player player = Minecraft.getInstance().player;
      int available = player != null ? RefinementPoints.getAvailable(player) : 0;
      int refinement = player != null ? RefinementPoints.getRefinement(player) : 0;
      int atAvailable = player != null ? AdvTechPoints.getAvailable(player) : 0;
      boolean hwbUnlocked = player != null && player.getPersistentData().getBoolean("unlocked_hwb");
      boolean hwbWitnessed = player != null && player.getPersistentData().getBoolean("witnessed_hwb");
      boolean refinementEnabled = AbyssGamerules.isClientDomainRefinementEnabled();
      boolean hwbEnabled = AbyssGamerules.isClientHollowWickerBasketEnabled();

      if (this.spendButton != null) {
         this.spendButton.active = refinementEnabled && available > 0 && refinement < 50;
      }

      if (this.hwbUnlockButton != null) {
         this.hwbUnlockButton.active = hwbEnabled && !hwbUnlocked && atAvailable > 0 && hwbWitnessed;
      }

      Font font = Minecraft.getInstance().font;
      int cx = px + 12;
      int cy = py + 10;

      g.drawString(font, Component.translatable("label.abyss.refinement_stats"), cx, cy, COL_HEADER, false);
      g.fill(cx, cy + 10, px + pw - 12, cy + 11, COL_BORDER);
      cy += 18;

      String refinementStr = refinement + " / 50";
      g.drawString(font, Component.translatable("label.abyss.refinement_level"), cx, cy, COL_LABEL, false);
      g.drawString(font, Component.literal(refinementStr), px + pw - 12 - font.width(refinementStr), cy, COL_VALUE, false);
      cy += 12;

      this.drawStatBar(g, cx, cy, pw - 24, 8, refinement, 50);
      cy += 18;

      String availableStr = String.valueOf(available);
      g.drawString(font, Component.translatable("label.abyss.points_available"), cx, cy, COL_LABEL, false);
      g.drawString(font, Component.literal(availableStr), px + pw - 12 - font.width(availableStr), cy, COL_GOLD, false);

      cy = py + 126;
      g.fill(cx, cy, px + pw - 12, cy + 1, COL_DIVIDER);
      cy += 8;

      g.drawString(font, Component.translatable("label.abyss.hwb_header"), cx, cy, COL_HEADER, false);
      g.fill(cx, cy + 10, px + pw - 12, cy + 11, COL_BORDER);
      cy += 18;

      Component statusText;
      int statusColor;
      if (hwbUnlocked) {
         statusText = Component.translatable("label.abyss.hwb_unlocked");
         statusColor = COL_GREEN;
      } else if (!hwbWitnessed) {
         statusText = Component.translatable("label.abyss.hwb_not_witnessed");
         statusColor = -7829368;
      } else {
         statusText = Component.translatable("label.abyss.hwb_witnessed");
         statusColor = COL_GOLD;
      }

      g.drawString(font, statusText, cx, cy, statusColor, false);
      cy += 12;

      String atStr = atAvailable + " / 5";
      g.drawString(font, Component.translatable("label.abyss.advtech_points"), cx, cy, COL_LABEL, false);
      g.drawString(font, Component.literal(atStr), px + pw - 12 - font.width(atStr), cy, COL_GOLD, false);

      if (!refinementEnabled) {
         drawDisabledOverlay(g, font, px + 4, py + 6, pw - 8, 116);
      }

      if (!hwbEnabled) {
         drawDisabledOverlay(g, font, px + 4, py + 130, pw - 8, 87);
      }
   }

   private static void drawDisabledOverlay(GuiGraphics g, Font font, int x, int y, int w, int h) {
      g.fill(x, y, x + w, y + h, -653192943);
      Component msg = Component.literal("Disabled by server rule.");
      g.drawString(font, msg, x + (w - font.width(msg)) / 2, y + (h - 8) / 2, -7829368, false);
   }

   @Override
   public void renderTooltips(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my) {
      Font font = Minecraft.getInstance().font;

      if (this.hwbUnlockButton != null && !this.hwbUnlockButton.active && this.isHovering(this.hwbUnlockButton.getX(), this.hwbUnlockButton.getY(), this.hwbUnlockButton.getWidth(), this.hwbUnlockButton.getHeight(), mx, my)) {
         Player player = Minecraft.getInstance().player;
         boolean hwbUnlocked = player != null && player.getPersistentData().getBoolean("unlocked_hwb");
         boolean hwbWitnessed = player != null && player.getPersistentData().getBoolean("witnessed_hwb");
         String tipKey = hwbUnlocked ? "tooltip.abyss.hwb_already_unlocked" : (!hwbWitnessed ? "tooltip.abyss.hwb_not_witnessed_tip" : "tooltip.abyss.no_advtech_points");

         // Fixed: Pass Component directly without getVisualOrderText()
         g.renderComponentTooltip(font, List.of(Component.translatable(tipKey)), mx, my);
      }

      if (this.spendButton != null && !this.spendButton.active && this.isHovering(this.spendButton.getX(), this.spendButton.getY(), this.spendButton.getWidth(), this.spendButton.getHeight(), mx, my)) {
         // Fixed: Pass Component directly without getVisualOrderText()
         g.renderComponentTooltip(font, List.of(Component.translatable("tooltip.abyss.no_points")), mx, my);
      }
   }

   private void drawStatBar(GuiGraphics g, int x, int y, int w, int h, int value, int max) {
      g.fill(x, y, x + w, y + h, COL_BAR_BG);
      int fillW = (int) ((float) Math.min(value, max) / (float) max * (float) w);
      if (fillW > 0) {
         g.fill(x, y, x + fillW, y + h, COL_BAR_FILL);
         g.fill(x, y, x + fillW, y + 2, COL_BAR_SHINE);
      }

      g.fill(x - 1, y - 1, x + w + 1, y, COL_BORDER);
      g.fill(x - 1, y + h, x + w + 1, y + h + 1, COL_BORDER);
      g.fill(x - 1, y - 1, x, y + h + 1, COL_BORDER);
      g.fill(x + w, y - 1, x + w + 1, y + h + 1, COL_BORDER);
   }

   private boolean isHovering(int x, int y, int w, int h, int mx, int my) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }
}