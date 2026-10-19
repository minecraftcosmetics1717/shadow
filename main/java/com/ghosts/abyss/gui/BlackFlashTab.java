package com.ghosts.abyss.gui;

import com.ghosts.abyss.BlackFlashPoints;
import com.ghosts.abyss.AbyssGamerules;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketActivateAtWillBlackFlash;
import com.ghosts.abyss.network.PacketPurchaseBlackFlashPerk;
import com.ghosts.abyss.network.PacketPurchaseBlackFlashPerk.Perk;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class BlackFlashTab implements IMenuTab {
   private static final int COL_PANEL = -15987700;
   private static final int COL_BORDER = -5627872;
   private static final int COL_TAB_ACTIVE = -9564401;
   private static final int COL_TAB_HOVER = -14022648;
   private static final int COL_HEADER = -2276284;
   private static final int COL_LABEL = -1;
   private static final int COL_SUBTEXT = -7829368;
   private static final int COL_EXP = -30652;
   private static final int COL_GOLD = -10496;
   private static final int COL_DIM = -11193549;
   private static final int COL_BG = -451936256;
   private static final int COL_INNER = 1152000032;
   private static final int COL_REQ_MET = -11141291;
   private static final int COL_REQ_UNMET = -43691;
   private static final int PAD = 8;
   private static final int BTN_H = 14;
   private static final int BTN_W = 160;

   private int scrollOffset = 0;
   private int contentHeight = 0;
   private int storedPh = 230;

   private JslopButton btnChanceUp;
   private JslopButton btnFatigueUp;
   private JslopButton btnEnergyUp;
   private JslopButton btnBrainUp;
   private JslopButton btnUnlockAtWill;
   private JslopButton btnChargeAtWill;

   @Override
   public int panelColor() {
      return COL_PANEL;
   }

   @Override
   public int borderColor() {
      return COL_BORDER;
   }

   @Override
   public int tabActiveColor() {
      return COL_TAB_ACTIVE;
   }

   @Override
   public int tabHoverColor() {
      return COL_TAB_HOVER;
   }

   @Override
   public int titleColor() {
      return COL_EXP;
   }

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
      return Component.translatable("tab.abyss.black_flash");
   }

   @Override
   public List<AbstractWidget> buildWidgets(int px, int py, int pw, int ph) {
      this.storedPh = ph;
      this.scrollOffset = 0;
      List<AbstractWidget> widgets = new ArrayList<>();
      int cx = px + pw / 2;

      this.btnChanceUp = JslopButton.jslopBuilder(Component.literal("Upgrade"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(Perk.CHANCE)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-7659488, -3390396, -871366656, -870318080, 713760800)
              .build();
      widgets.add(this.btnChanceUp);

      this.btnFatigueUp = JslopButton.jslopBuilder(Component.literal("Upgrade"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(Perk.FATIGUE)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-7659488, -3390396, -871366656, -870318080, 713760800)
              .build();
      widgets.add(this.btnFatigueUp);

      this.btnEnergyUp = JslopButton.jslopBuilder(Component.literal("Upgrade"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(Perk.ENERGY)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-7659488, -3390396, -871366656, -870318080, 713760800)
              .build();
      widgets.add(this.btnEnergyUp);

      this.btnBrainUp = JslopButton.jslopBuilder(Component.literal("Upgrade"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(Perk.BRAIN)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-7659488, -3390396, -871366656, -870318080, 713760800)
              .build();
      widgets.add(this.btnBrainUp);

      this.btnUnlockAtWill = JslopButton.jslopBuilder(Component.literal("Unlock  (20 exp)"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketPurchaseBlackFlashPerk(Perk.AT_WILL)))
              .bounds(cx - 80, py, 160, 14)
              .colors(-5618688, -2263262, -871365376, -870315520, 715801600)
              .build();
      widgets.add(this.btnUnlockAtWill);

      this.btnChargeAtWill = JslopButton.jslopBuilder(Component.literal("Charge At-Will"), (btn) -> ModNetwork.CHANNEL.sendToServer(new PacketActivateAtWillBlackFlash()))
              .bounds(cx - 80, py, 160, 14)
              .colors(-5618688, -2263262, -871365376, -870315520, 715801600)
              .build();
      widgets.add(this.btnChargeAtWill);

      return widgets;
   }

   @Override
   public void tick() {
      if (this.btnChanceUp != null) this.btnChanceUp.tick();
      if (this.btnFatigueUp != null) this.btnFatigueUp.tick();
      if (this.btnEnergyUp != null) this.btnEnergyUp.tick();
      if (this.btnBrainUp != null) this.btnBrainUp.tick();
      if (this.btnUnlockAtWill != null) this.btnUnlockAtWill.tick();
      if (this.btnChargeAtWill != null) this.btnChargeAtWill.tick();
   }

   @Override
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
      if (!AbyssGamerules.isClientBlackFlashEnabled()) {
         g.fill(px, py, px + pw, py + ph, -653192943);
         Font f = Minecraft.getInstance().font;
         Component line1 = Component.literal("Black Flash features have been");
         Component line2 = Component.literal("disabled by a server rule.");
         g.drawString(f, line1, px + (pw - f.width(line1)) / 2, py + ph / 2 - 10, COL_SUBTEXT, false);
         g.drawString(f, line2, px + (pw - f.width(line2)) / 2, py + ph / 2 + 1, COL_SUBTEXT, false);

         if (this.btnChanceUp != null) this.btnChanceUp.visible = false;
         if (this.btnFatigueUp != null) this.btnFatigueUp.visible = false;
         if (this.btnEnergyUp != null) this.btnEnergyUp.visible = false;
         if (this.btnBrainUp != null) this.btnBrainUp.visible = false;
         if (this.btnUnlockAtWill != null) this.btnUnlockAtWill.visible = false;
         if (this.btnChargeAtWill != null) this.btnChargeAtWill.visible = false;

         this.contentHeight = ph;
      } else {
         Font font = Minecraft.getInstance().font;
         int cx = px + pw / 2;
         int cy = py + 8 - this.scrollOffset;

         g.enableScissor(px, py, px + pw, py + ph);

         int exp = BlackFlashPoints.getClientExp();
         g.drawCenteredString(font, "Black Flash Experience: " + exp, cx, cy, COL_EXP);
         cy += 14;

         int descW = pw - 16 - 16;

         // --- CHANCE PERK ---
         int chanceTier = BlackFlashPoints.getClientChanceTier();
         List<FormattedCharSequence> chanceDesc = font.split(Component.literal("Each tier sharpens a sorcerer's instincts, drawing them closer to that fleeting moment when a strike and cursed energy converge perfectly."), descW);
         int chanceBoxH = 65 + chanceDesc.size() * 9;
         this.drawBox(g, px + 8, cy, pw - 16, chanceBoxH, COL_BG, COL_BORDER, COL_INNER);
         g.drawCenteredString(font, "Black Flash Chance", cx, cy + 4, COL_HEADER);
         g.fill(px + 8 + 8, cy + 14, px + pw - 8 - 8, cy + 15, COL_INNER);
         int chanceDescY = cy + 19;

         for (FormattedCharSequence line : chanceDesc) {
            g.drawString(font, line, cx - font.width(line) / 2, chanceDescY, COL_LABEL, false);
            chanceDescY += 9;
         }

         g.drawCenteredString(font, String.format("Tier %d / %d  (+%d rolls)", chanceTier, 5, chanceTier), cx, chanceDescY + 8, COL_SUBTEXT);
         this.drawTierBar(g, cx, chanceDescY + 19, chanceTier, 5);

         if (this.btnChanceUp != null) {
            int btnY = cy + chanceBoxH - 14 - 4;
            this.btnChanceUp.setY(btnY);
            this.btnChanceUp.visible = btnY + 14 > py && btnY < py + ph;
            this.btnChanceUp.active = chanceTier < 5 && exp >= 5;
            this.btnChanceUp.setMessage(Component.literal(chanceTier < 5 ? "Upgrade  (5 exp)" : "Maxed"));
         }

         cy += chanceBoxH + 6;

         // --- FATIGUE PERK ---
         int fatigueTier = BlackFlashPoints.getClientFatigueTier();
         List<FormattedCharSequence> fatigueDesc = font.split(Component.literal("When cursed energy aligns with impact, the mind and body are not merely tested — they are renewed. Each tier deepens that restoration."), descW);
         int fatigueBoxH = 65 + fatigueDesc.size() * 9;
         this.drawBox(g, px + 8, cy, pw - 16, fatigueBoxH, COL_BG, COL_BORDER, COL_INNER);
         g.drawCenteredString(font, "Fatigue Reduction", cx, cy + 4, COL_HEADER);
         g.fill(px + 8 + 8, cy + 14, px + pw - 8 - 8, cy + 15, COL_INNER);
         int fatigueDescY = cy + 19;

         for (FormattedCharSequence line : fatigueDesc) {
            g.drawString(font, line, cx - font.width(line) / 2, fatigueDescY, COL_LABEL, false);
            fatigueDescY += 9;
         }

         g.drawCenteredString(font, String.format("Tier %d / %d  (+%ds reduction)", fatigueTier, 4, fatigueTier * 30), cx, fatigueDescY + 8, COL_SUBTEXT);
         this.drawTierBar(g, cx, fatigueDescY + 19, fatigueTier, 4);

         if (this.btnFatigueUp != null) {
            int btnY = cy + fatigueBoxH - 14 - 4;
            this.btnFatigueUp.setY(btnY);
            this.btnFatigueUp.visible = btnY + 14 > py && btnY < py + ph;
            this.btnFatigueUp.active = fatigueTier < 4 && exp >= 5;
            this.btnFatigueUp.setMessage(Component.literal(fatigueTier < 4 ? "Upgrade  (5 exp)" : "Maxed"));
         }

         cy += fatigueBoxH + 6;

         // --- ENERGY PERK ---
         int energyTier = BlackFlashPoints.getClientEnergyTier();
         List<FormattedCharSequence> energyDesc = font.split(Component.literal("A Black Flash does not merely damage — it resonates. The cursed energy discharged in that instant reverberates back through the sorcerer, amplified with each tier of mastery."), descW);
         int energyBoxH = 65 + energyDesc.size() * 9;
         this.drawBox(g, px + 8, cy, pw - 16, energyBoxH, COL_BG, COL_BORDER, COL_INNER);
         g.drawCenteredString(font, "Cursed Energy", cx, cy + 4, COL_HEADER);
         g.fill(px + 8 + 8, cy + 14, px + pw - 8 - 8, cy + 15, COL_INNER);
         int energyDescY = cy + 19;

         for (FormattedCharSequence line : energyDesc) {
            g.drawString(font, line, cx - font.width(line) / 2, energyDescY, COL_LABEL, false);
            energyDescY += 9;
         }

         g.drawCenteredString(font, String.format("Tier %d / %d  (+%d energy)", energyTier, 3, energyTier * 150), cx, energyDescY + 8, COL_SUBTEXT);
         this.drawTierBar(g, cx, energyDescY + 19, energyTier, 3);

         if (this.btnEnergyUp != null) {
            int btnY = cy + energyBoxH - 14 - 4;
            this.btnEnergyUp.setY(btnY);
            this.btnEnergyUp.visible = btnY + 14 > py && btnY < py + ph;
            this.btnEnergyUp.active = energyTier < 3 && exp >= 5;
            this.btnEnergyUp.setMessage(Component.literal(energyTier < 3 ? "Upgrade  (5 exp)" : "Maxed"));
         }

         cy += energyBoxH + 6;

         // --- BRAIN PERK ---
         int brainTier = BlackFlashPoints.getClientBrainTier();
         int[] brainReductions = new int[]{0, 15, 30, 60};
         List<FormattedCharSequence> brainDesc = font.split(Component.literal("The mental toll of sustained combat fades faster for those who have truly mastered the Black Flash. Each tier accelerates recovery from brain damage."), descW);
         int brainBoxH = 65 + brainDesc.size() * 9;
         this.drawBox(g, px + 8, cy, pw - 16, brainBoxH, COL_BG, COL_BORDER, COL_INNER);
         g.drawCenteredString(font, "Brain Damage Recovery", cx, cy + 4, COL_HEADER);
         g.fill(px + 8 + 8, cy + 14, px + pw - 8 - 8, cy + 15, COL_INNER);
         int brainDescY = cy + 19;

         for (FormattedCharSequence line : brainDesc) {
            g.drawString(font, line, cx - font.width(line) / 2, brainDescY, COL_LABEL, false);
            brainDescY += 9;
         }

         g.drawCenteredString(font, String.format("Tier %d / %d  (+%ds recovery)", brainTier, 3, brainReductions[brainTier]), cx, brainDescY + 8, COL_SUBTEXT);
         this.drawTierBar(g, cx, brainDescY + 19, brainTier, 3);

         if (this.btnBrainUp != null) {
            int btnY = cy + brainBoxH - 14 - 4;
            this.btnBrainUp.setY(btnY);
            this.btnBrainUp.visible = btnY + 14 > py && btnY < py + ph;
            this.btnBrainUp.active = brainTier < 3 && exp >= 5;
            this.btnBrainUp.setMessage(Component.literal(brainTier < 3 ? "Upgrade  (5 exp)" : "Maxed"));
         }

         cy += brainBoxH + 6;

         // --- AT-WILL PERK ---
         boolean atWillUnlocked = BlackFlashPoints.isClientAtWillUnlocked();
         boolean atWillCharged = BlackFlashPoints.isClientAtWillCharged();
         long cooldownEnd = BlackFlashPoints.getClientAtWillCooldownEnd();
         long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
         boolean onCooldown = !atWillCharged && gameTime < cooldownEnd;
         long cooldownLeft = Math.max(0L, (cooldownEnd - gameTime) / 20L);
         boolean chanceMaxed = chanceTier >= 5;

         List<FormattedCharSequence> atWillDesc = font.split(Component.literal("The pinnacle of Black Flash mastery. Those who have touched that state of perfect convergence can call upon it deliberately — ensuring their next strike resonates. Charges once at a time."), descW);
         int atWillBoxH = 58 + atWillDesc.size() * 9;
         this.drawBox(g, px + 8, cy, pw - 16, atWillBoxH, COL_BG, COL_BORDER, COL_INNER);

         String atWillTitle = atWillUnlocked ? (atWillCharged ? "At-Will Black Flash  [§aCharged§r]" : (onCooldown ? "At-Will Black Flash  [§7Cooldown§r]" : "At-Will Black Flash  [§eReady§r]")) : "At-Will Black Flash";
         g.drawCenteredString(font, atWillTitle, cx, cy + 4, COL_HEADER);
         g.fill(px + 8 + 8, cy + 14, px + pw - 8 - 8, cy + 15, COL_INNER);
         int atWillDescY = cy + 19;

         for (FormattedCharSequence line : atWillDesc) {
            g.drawString(font, line, cx - font.width(line) / 2, atWillDescY, COL_LABEL, false);
            atWillDescY += 9;
         }

         if (atWillUnlocked) {
            String statusText = atWillCharged ? "§aCharged — your next strike is certain." : (onCooldown ? "§7Cooldown: " + cooldownLeft + "s remaining." : "§eReady to charge.");
            g.drawCenteredString(font, statusText, cx, atWillDescY + 8, COL_LABEL);
         } else {
            String checkMark = chanceMaxed ? "✔" : "✘";
            int checkColor = chanceMaxed ? COL_REQ_MET : COL_REQ_UNMET;
            String reqLine = checkMark + " Max Black Flash Chance tier";
            g.drawCenteredString(font, reqLine, cx, atWillDescY + 2, checkColor);
         }

         if (this.btnUnlockAtWill != null) {
            int btnY = cy + atWillBoxH - 14 - 4;
            this.btnUnlockAtWill.setY(btnY);
            this.btnUnlockAtWill.visible = !atWillUnlocked && btnY + 14 > py && btnY < py + ph;
            this.btnUnlockAtWill.active = !atWillUnlocked && exp >= 20 && chanceMaxed;
         }

         if (this.btnChargeAtWill != null) {
            int btnY = cy + atWillBoxH - 14 - 4;
            this.btnChargeAtWill.setY(btnY);
            this.btnChargeAtWill.visible = atWillUnlocked && btnY + 14 > py && btnY < py + ph;
            this.btnChargeAtWill.active = atWillUnlocked && !atWillCharged && !onCooldown;
            this.btnChargeAtWill.setMessage(Component.literal(atWillCharged ? "Already Charged" : (onCooldown ? "On Cooldown (" + cooldownLeft + "s)" : "Charge At-Will")));
         }

         cy += atWillBoxH + 6;
         this.contentHeight = cy - py + this.scrollOffset + 8;
         g.disableScissor();
      }
   }

   private void drawBox(GuiGraphics g, int x, int y, int w, int h, int bg, int border, int inner) {
      g.fill(x, y, x + w, y + h, bg);
      g.fill(x, y, x + w, y + 1, border);
      g.fill(x, y + h - 1, x + w, y + h, border);
      g.fill(x, y, x + 1, y + h, border);
      g.fill(x + w - 1, y, x + w, y + h, border);
      g.fill(x + 2, y + 2, x + w - 2, y + 3, inner);
      g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, inner);
   }

   private void drawTierBar(GuiGraphics g, int cx, int y, int current, int max) {
      int totalW = max * 14;
      int startX = cx - totalW / 2;

      for (int i = 0; i < max; ++i) {
         g.fill(startX + i * 14, y, startX + i * 14 + 12, y + 5, i < current ? COL_GOLD : COL_DIM);
      }
   }
}