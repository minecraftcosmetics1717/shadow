package com.ghosts.abyss.gui;

import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.RctPoints;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketPurchaseRctPerk;
import com.ghosts.abyss.network.PacketPurchaseRctPerk.Perk;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class RctTab implements IMenuTab {
   private static final int COL_HEADER = -5537836;
   private static final int COL_LABEL = -3355444;
   private static final int COL_EXP = -12255352;
   private static final int COL_GOLD = -10496;
   private static final int COL_GREY = -7829368;
   private static final int COL_DIM = -11184811;
   private static final int COL_BG = -452657136;
   private static final int COL_BORDER = -9814112;
   private static final int COL_INNER = 1147813792;
   private static final int PAD = 8;
   private static final int BTN_H = 14;
   private static final int BTN_W = 160;
   private int scrollOffset = 0;
   private int contentHeight = 0;
   private int storedPh = 230;
   private JslopButton btnEfficiencyUp;
   private JslopButton btnProfUp;
   private JslopButton btnUnlockOutput;

   @Override
   public int getScrollOffset() {
      return this.scrollOffset;
   }

   @Override
   public int getContentHeight() {
      return this.contentHeight;
   }

   @Override
   public Component getTitle() {
      return Component.translatable("tab.abyss.rct");
   }

   @Override
   public List<AbstractWidget> buildWidgets(int px, int py, int pw, int ph) {
      this.storedPh = ph;
      this.scrollOffset = 0;
      List<AbstractWidget> widgets = new ArrayList<>();
      int cx = px + pw / 2;

      this.btnEfficiencyUp = JslopButton.jslopBuilder(Component.literal("Upgrade Efficiency"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseRctPerk(Perk.EFFICIENCY)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-9814112, -6656048, -872087536, -871561179, 711606176)
              .build();
      widgets.add(this.btnEfficiencyUp);

      this.btnProfUp = JslopButton.jslopBuilder(Component.literal("Upgrade Proficiency"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseRctPerk(Perk.PROFICIENCY)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-9814112, -6656048, -872087536, -871561179, 711606176)
              .build();
      widgets.add(this.btnProfUp);

      this.btnUnlockOutput = JslopButton.jslopBuilder(Component.literal("Unlock  (20 exp)"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseRctPerk(Perk.OUTPUT)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-9814112, -6656048, -872087536, -871561179, 711606176)
              .build();
      widgets.add(this.btnUnlockOutput);

      return widgets;
   }

   public void tick() {
      if (this.btnEfficiencyUp != null) this.btnEfficiencyUp.tick();
      if (this.btnProfUp != null) this.btnProfUp.tick();
      if (this.btnUnlockOutput != null) this.btnUnlockOutput.tick();
   }

   public boolean onMouseScrolled(double mx, double my, double delta) {
      int maxScroll = Math.max(0, this.contentHeight - this.storedPh);
      if (maxScroll == 0) {
         return false;
      } else {
         this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int)(delta * 10.0F)));
         return true;
      }
   }

   @Override
   public void render(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my, float pt) {
      this.storedPh = ph;
      if (!AbyssGamerules.isClientReverseCursedTechniqueEnabled()) {
         g.fill(px, py, px + pw, py + ph, -653192943);
         Font font = Minecraft.getInstance().font;
         Component line1 = Component.literal("Reverse Cursed Technique features");
         Component line2 = Component.literal("have been disabled by a server rule.");

         g.drawString(font, line1, px + (pw - font.width(line1)) / 2, py + ph / 2 - 10, COL_GREY, false);
         g.drawString(font, line2, px + (pw - font.width(line2)) / 2, py + ph / 2 + 1, COL_GREY, false);

         if (this.btnEfficiencyUp != null) this.btnEfficiencyUp.visible = false;
         if (this.btnProfUp != null) this.btnProfUp.visible = false;
         if (this.btnUnlockOutput != null) this.btnUnlockOutput.visible = false;

         this.contentHeight = ph;
      } else {
         Font font = Minecraft.getInstance().font;
         int cx = px + pw / 2;
         int cy = py + 8 - this.scrollOffset;

         g.enableScissor(px, py, px + pw, py + ph);

         int exp = RctPoints.getClientExp();
         float acc = RctPoints.getClientExpAccumulator();
         g.drawCenteredString(font, "RCT Experience: " + exp, cx, cy, COL_EXP);
         cy += 11;

         g.drawCenteredString(font, String.format("Next point: %.1f%%", acc / 250.0F * 100.0F), cx, cy, COL_GREY);
         cy += 14;

         // Efficiency Section
         int effTier = RctPoints.getClientEfficiencyTier();
         float effMult = RctPoints.getClientEfficiencyMultiplier();
         int effBoxH = 60;
         this.drawBox(g, px + 8, cy, pw - 16, effBoxH);
         g.drawCenteredString(font, "RCT Efficiency", cx, cy + 4, COL_HEADER);
         g.fill(px + 16, cy + 14, px + pw - 16, cy + 15, COL_INNER);
         g.drawCenteredString(font, String.format("Tier %d / %d  (%.1fx output)", effTier, 5, effMult), cx, cy + 18, COL_LABEL);
         this.drawTierBar(g, cx, cy + 30, effTier, 5);

         if (this.btnEfficiencyUp != null) {
            int btnY = cy + effBoxH - 14 - 4;
            this.btnEfficiencyUp.setY(btnY);
            this.btnEfficiencyUp.visible = btnY + 14 > py && btnY < py + ph;
            this.btnEfficiencyUp.active = effTier < 5 && exp >= 5;
            this.btnEfficiencyUp.setMessage(Component.literal(effTier < 5 ? "Upgrade  (5 exp)" : "Maxed"));
         }

         cy += effBoxH + 6;

         // Proficiency Section
         int profTier = RctPoints.getClientProficiencyTier();
         float profDiv = RctPoints.getClientProficiencyDivisor();
         int profBoxH = 60;
         this.drawBox(g, px + 8, cy, pw - 16, profBoxH);
         g.drawCenteredString(font, "RCT Proficiency", cx, cy + 4, COL_HEADER);
         g.fill(px + 16, cy + 14, px + pw - 16, cy + 15, COL_INNER);
         g.drawCenteredString(font, String.format("Tier %d / %d  (%.1fx fatigue delay)", profTier, 4, profDiv), cx, cy + 18, COL_LABEL);
         this.drawTierBar(g, cx, cy + 30, profTier, 4);

         if (this.btnProfUp != null) {
            int btnY = cy + profBoxH - 14 - 4;
            this.btnProfUp.setY(btnY);
            this.btnProfUp.visible = btnY + 14 > py && btnY < py + ph;
            this.btnProfUp.active = profTier < 4 && exp >= 5;
            this.btnProfUp.setMessage(Component.literal(profTier < 4 ? "Upgrade  (5 exp)" : "Maxed"));
         }

         cy += profBoxH + 6;

// RCT Output Section (one-time unlock)
         boolean outputUnlocked = RctPoints.isClientOutputUnlocked();
         int descW = pw - 32;
         List<FormattedCharSequence> descLines = font.split(
                 Component.literal("Complete mastery over reverse cursed technique, allowing the user to manipulate the flow of their reverse cursed energy."),
                 descW
         );
         int outputBoxH = 42 + descLines.size() * 9 + (outputUnlocked ? 0 : 18); // extra space only for the button
         this.drawBox(g, px + 8, cy, pw - 16, outputBoxH);
         g.drawCenteredString(font, "RCT Output", cx, cy + 4, COL_HEADER);
         g.fill(px + 16, cy + 14, px + pw - 16, cy + 15, COL_INNER);

         int descY = cy + 19;
         for (FormattedCharSequence line : descLines) {
            g.drawString(font, line, cx - font.width(line) / 2, descY, COL_LABEL, false);
            descY += 9;
         }

         if (this.btnUnlockOutput != null) {
            int btnY = cy + outputBoxH - 14 - 4;
            this.btnUnlockOutput.setY(btnY);
            this.btnUnlockOutput.visible = btnY + 14 > py && btnY < py + ph;   // always visible when in view
            this.btnUnlockOutput.active = !outputUnlocked && exp >= 20;
            this.btnUnlockOutput.setMessage(Component.literal(outputUnlocked ? "Unlocked" : "Unlock  (20 exp)"));
         }

         cy += outputBoxH + 6;
         this.contentHeight = cy - (py - this.scrollOffset) + 8;

         g.disableScissor();
      }
   }

   private void drawBox(GuiGraphics g, int x, int y, int w, int h) {
      g.fill(x, y, x + w, y + h, COL_BG);
      g.fill(x, y, x + w, y + 1, COL_BORDER);
      g.fill(x, y + h - 1, x + w, y + h, COL_BORDER);
      g.fill(x, y, x + 1, y + h, COL_BORDER);
      g.fill(x + w - 1, y, x + w, y + h, COL_BORDER);
      g.fill(x + 2, y + 2, x + w - 2, y + 3, COL_INNER);
      g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, COL_INNER);
   }

   private void drawTierBar(GuiGraphics g, int cx, int y, int current, int max) {
      int totalW = max * 14;
      int startX = cx - totalW / 2;

      for (int i = 0; i < max; ++i) {
         g.fill(startX + i * 14, y, startX + i * 14 + 12, y + 5, i < current ? COL_GOLD : COL_DIM);
      }
   }
}