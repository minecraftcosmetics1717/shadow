package com.ghosts.abyss.gui;

import com.ghosts.abyss.ClientSetup;
import com.ghosts.abyss.csm.ClientCsmCache;
import com.ghosts.abyss.csm.CsmSpiritData;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketCsmFeedFinger;
import com.ghosts.abyss.network.PacketCsmMarkUnique;
import com.ghosts.abyss.network.PacketCsmRenameSpirit;
import com.ghosts.abyss.network.PacketCsmToggleSpared;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CsmTab implements IMenuTab {
   private static final int COL_BG = -268106488;
   private static final int COL_BORDER = -7667712;
   private static final int COL_INNER = 864747520;
   private static final int COL_HEADER = -3399134;
   private static final int COL_LABEL = -5592406;
   private static final int COL_DIM = -12303292;
   private static final int COL_GOLD = -10496;
   private static final int COL_SPARED = -8947849;
   private static final int COL_WHITE = -1;
   private static final int PAD = 8;
   private static final int ROW_H = 18;
   private static final int BTN_H = 13;
   private static final int SMALL_BTN_W = 18;
   private static final int MAX_VISIBLE = 6;

   private int scrollOffset = 0;
   private int contentHeight = 0;
   private int storedPx;
   private int storedPy;
   private int storedPw;
   private int storedPh;
   private int listScrollOffset = 0;
   private int listBoxStartY;
   private int listBoxEndY;

   private int renameSpiritIdx = -1;
   private EditBox renameSpiritField;
   private JslopButton confirmRenameSpiritBtn;

   private final List<JslopButton> uniqueButtons = new ArrayList<>();
   private final List<JslopButton> sparedButtons = new ArrayList<>();
   private final List<JslopButton> fingerButtons = new ArrayList<>();
   private final List<JslopButton> renameSpiritBtns = new ArrayList<>();

   @Override
   public Component getTitle() {
      return Component.literal("CSM");
   }

   @Override
   public int borderColor() {
      return COL_BORDER;
   }

   @Override
   public int tabActiveColor() {
      return -11206656;
   }

   @Override
   public int tabHoverColor() {
      return -13631488;
   }

   @Override
   public int titleColor() {
      return COL_HEADER;
   }

   @Override
   public int panelColor() {
      return -16448248;
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
   public List<AbstractWidget> buildWidgets(int px, int py, int pw, int ph) {
      this.storedPx = px;
      this.storedPy = py;
      this.storedPw = pw;
      this.storedPh = ph;
      this.scrollOffset = 0;
      this.listScrollOffset = 0;
      this.renameSpiritIdx = -1;

      this.uniqueButtons.clear();
      this.sparedButtons.clear();
      this.fingerButtons.clear();
      this.renameSpiritBtns.clear();

      List<AbstractWidget> widgets = new ArrayList<>();
      Font font = Minecraft.getInstance().font;

      for (int i = 0; i < MAX_VISIBLE; ++i) {
         final int visualRow = i;
         JslopButton unique = JslopButton.jslopBuilder(Component.literal("✦"), (b) -> this.onMarkUnique(visualRow))
                 .bounds(px, py, SMALL_BTN_W, BTN_H)
                 .colors(-10092544, -6737101, -871759872, -871038976, 577110016)
                 .build();
         unique.visible = false;
         this.uniqueButtons.add(unique);
         widgets.add(unique);

         JslopButton spared = JslopButton.jslopBuilder(Component.literal("◇"), (b) -> this.onToggleSpared(visualRow))
                 .bounds(px, py, SMALL_BTN_W, BTN_H)
                 .colors(-14013910, -12303292, -872086267, -871757302, 573188650)
                 .build();
         spared.visible = false;
         this.sparedButtons.add(spared);
         widgets.add(spared);

         JslopButton finger = JslopButton.jslopBuilder(Component.literal("●"), (b) -> this.onFeedFinger(visualRow))
                 .bounds(px, py, SMALL_BTN_W, BTN_H)
                 .colors(-10075136, -6724096, -871758336, -871035648, 577127424)
                 .build();
         finger.visible = false;
         this.fingerButtons.add(finger);
         widgets.add(finger);

         JslopButton ren = JslopButton.jslopBuilder(Component.literal("✏"), (b) -> this.activateSpiritRename(visualRow))
                 .bounds(px, py, SMALL_BTN_W, BTN_H)
                 .colors(-15058342, -14001526, -872412648, -872411104, 572144218)
                 .build();
         ren.visible = false;
         this.renameSpiritBtns.add(ren);
         widgets.add(ren);
      }

      this.confirmRenameSpiritBtn = JslopButton.jslopBuilder(Component.literal("✓"), (b) -> this.confirmSpiritRename())
              .bounds(px, py, SMALL_BTN_W, BTN_H)
              .colors(-15050214, -13989334, -872411136, -872407040, 572152346)
              .build();
      this.confirmRenameSpiritBtn.visible = false;
      widgets.add(this.confirmRenameSpiritBtn);

      this.renameSpiritField = new EditBox(font, px + PAD + 4, py, 100, BTN_H, Component.literal(""));
      this.renameSpiritField.setMaxLength(50);
      this.renameSpiritField.visible = false;
      widgets.add(this.renameSpiritField);

      return widgets;
   }

   @Override
   public void render(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my, float pt) {
      this.storedPx = px;
      this.storedPy = py;
      this.storedPw = pw;
      this.storedPh = ph;

      Font font = Minecraft.getInstance().font;
      int cx = px + pw / 2;
      int cy = py + PAD - this.scrollOffset;

      g.enableScissor(px, py, px + pw, py + ph);

      if (!ClientSetup.isLocalPlayerCsm(Minecraft.getInstance())) {
         this.hideAllButtons();
         g.fill(px, py, px + pw, py + ph, -653982456);
         g.drawCenteredString(font, "§8You are not a Cursed Spirit Manipulator.", cx, py + ph / 2 - 10, COL_DIM);
         g.drawCenteredString(font, "§8Acquire CT=18 (Geto) to use this menu.", cx, py + ph / 2 + 2, COL_DIM);
         this.contentHeight = ph;
         g.disableScissor();
      } else {
         List<CsmSpiritData> spirits = ClientCsmCache.getAll();
         this.drawBox(g, px + PAD, cy, pw - 16, 28);
         g.drawCenteredString(font, "§4Cursed Spirit Manipulation", cx, cy + 4, COL_HEADER);
         g.drawCenteredString(font, "§8Spirits stored: §c" + spirits.size(), cx, cy + 15, COL_LABEL);

         cy += 34;
         int spiritCount = spirits.size();
         int listBoxH = 132 + (spiritCount > MAX_VISIBLE ? 10 : 0);
         this.listBoxStartY = cy;
         this.listBoxEndY = cy + listBoxH;

         this.drawBox(g, px + PAD, cy, pw - 16, listBoxH);
         g.drawCenteredString(font, "§4■ Stored Spirits", cx, cy + 4, COL_HEADER);
         g.fill(px + PAD + 8, cy + 14, px + pw - PAD - 8, cy + 15, COL_INNER);

         int rowStartY = cy + 18;
         int innerClipBottom = cy + 18 + 108;

         this.hidePerRowButtons();
         if (this.renameSpiritField != null) this.renameSpiritField.visible = false;
         if (this.confirmRenameSpiritBtn != null) this.confirmRenameSpiritBtn.visible = false;

         if (spirits.isEmpty()) {
            g.drawCenteredString(font, "§8No spirits stored.", cx, rowStartY + 4, COL_DIM);
         } else {
            int maxScroll = Math.max(0, spiritCount - MAX_VISIBLE);
            this.listScrollOffset = Math.max(0, Math.min(this.listScrollOffset, maxScroll));

            int actionButtonsWidth = (SMALL_BTN_W + 2) * 4;
            int maxTextWidth = Math.max(20, pw - PAD * 2 - actionButtonsWidth - 18);

            g.enableScissor(px, rowStartY, px + pw, innerClipBottom);

            for (int i = this.listScrollOffset; i < Math.min(spiritCount, this.listScrollOffset + MAX_VISIBLE); ++i) {
               CsmSpiritData d = spirits.get(i);
               int visualRow = i - this.listScrollOffset;
               int rowY = rowStartY + visualRow * ROW_H;

               String dot = d.spared ? "§7◆§r" : "§4●§r";
               g.drawString(font, Component.literal(dot), px + PAD + 4, rowY + (ROW_H - 9) / 2, COL_WHITE, false);

               if (i != this.renameSpiritIdx) {
                  String label = d.shortLabel();
                  if (font.width(label) > maxTextWidth) {
                     label = font.plainSubstrByWidth(label, maxTextWidth - font.width("…")) + "…";
                  }

                  int textX = px + PAD + 14;
                  int textY = rowY + (ROW_H - 9) / 2;
                  if (d.fingers > 0) {
                     g.drawString(font, label, textX, textY, COL_GOLD, false);
                  } else {
                     g.drawString(font, label, textX, textY, d.spared ? COL_SPARED : COL_LABEL, false);
                  }
               }
            }

            g.disableScissor();

            if (spiritCount > MAX_VISIBLE) {
               String hint = "▲▼ " + (this.listScrollOffset + 1) + "-" + Math.min(this.listScrollOffset + MAX_VISIBLE, spiritCount) + " / " + spiritCount;
               g.drawCenteredString(font, hint, cx, innerClipBottom + 1, COL_DIM);
            }

            for (int i = this.listScrollOffset; i < Math.min(spiritCount, this.listScrollOffset + MAX_VISIBLE); ++i) {
               CsmSpiritData d = spirits.get(i);
               int visualRow = i - this.listScrollOffset;
               int rowY = rowStartY + visualRow * ROW_H;
               int btnY = rowY + 2;
               boolean inView = btnY + BTN_H > rowStartY && btnY < innerClipBottom;
               int btnX = px + pw - PAD - 2;

               if (visualRow < this.renameSpiritBtns.size()) {
                  JslopButton rb = this.renameSpiritBtns.get(visualRow);
                  btnX -= (SMALL_BTN_W + 2);
                  if (i == this.renameSpiritIdx && inView) {
                     int editX = px + PAD + 14;
                     if (this.renameSpiritField != null) {
                        this.renameSpiritField.setX(editX);
                        this.renameSpiritField.setY(btnY);
                        this.renameSpiritField.setWidth(Math.max(30, btnX - editX - 2));
                        this.renameSpiritField.visible = true;
                     }

                     if (this.confirmRenameSpiritBtn != null) {
                        this.confirmRenameSpiritBtn.setX(btnX);
                        this.confirmRenameSpiritBtn.setY(btnY);
                        this.confirmRenameSpiritBtn.visible = true;
                     }

                     rb.visible = false;
                  } else {
                     rb.setX(btnX);
                     rb.setY(btnY);
                     rb.visible = inView;
                  }
               }

               if (visualRow < this.fingerButtons.size()) {
                  JslopButton fb = this.fingerButtons.get(visualRow);
                  btnX -= (SMALL_BTN_W + 2);
                  fb.setX(btnX);
                  fb.setY(btnY);
                  fb.visible = inView && d.isUnique;
                  fb.active = d.isUnique && d.fingers < 20;
               }

               if (visualRow < this.sparedButtons.size()) {
                  JslopButton sb = this.sparedButtons.get(visualRow);
                  btnX -= (SMALL_BTN_W + 2);
                  sb.setX(btnX);
                  sb.setY(btnY);
                  sb.visible = inView;
                  sb.setMessage(Component.literal(d.spared ? "◆" : "◇"));
               }

               if (visualRow < this.uniqueButtons.size()) {
                  JslopButton ub = this.uniqueButtons.get(visualRow);
                  btnX -= (SMALL_BTN_W + 2);
                  ub.setX(btnX);
                  ub.setY(btnY);
                  ub.visible = inView && !d.isUnique && d.count > 0;
                  ub.active = !d.isUnique && d.count > 0;
               }
            }
         }

         cy += listBoxH + 6;

         int legendH = 119;
         this.drawBox(g, px + PAD, cy, pw - 16, legendH);
         g.drawCenteredString(font, "§4Legend", cx, cy + 4, COL_HEADER);
         g.fill(px + PAD + 8, cy + 14, px + pw - PAD - 8, cy + 15, COL_INNER);

         int lx = px + PAD + 8;
         int ly = cy + 20;
         g.drawString(font, "§c✦§r Mark Unique", lx, ly, COL_LABEL, false);
         ly += 17;
         g.drawString(font, "§7◇§r Spare from Uzumaki", lx, ly, COL_LABEL, false);
         ly += 11;
         g.drawString(font, "§7◆§r Spared from Uzumaki", lx, ly, COL_LABEL, false);
         ly += 11;
         g.drawString(font, "§4●§r Consumed", lx, ly, COL_LABEL, false);
         ly += 17;
         g.drawString(font, "§6●§r Feed Finger", lx, ly, COL_LABEL, false);
         ly += 11;
         g.drawString(font, "§6[✦]§r Finger-bearer", lx, ly, COL_LABEL, false);
         ly += 17;
         g.drawString(font, "§9✏§r Rename Spirit", lx, ly, COL_LABEL, false);

         cy += legendH + 6;
         this.contentHeight = cy - (py - this.scrollOffset) + PAD;

         g.disableScissor();
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.renameSpiritIdx >= 0) {
         if (keyCode == 257 || keyCode == 335) { // Enter
            this.confirmSpiritRename();
            return true;
         }

         if (keyCode == 256) { // Escape
            this.cancelSpiritRename();
            return true;
         }

         if (this.renameSpiritField != null && this.renameSpiritField.isFocused()) {
            return this.renameSpiritField.keyPressed(keyCode, scanCode, modifiers);
         }
      }

      return false;
   }

   @Override
   public boolean charTyped(char c, int modifiers) {
      if (this.renameSpiritIdx >= 0 && this.renameSpiritField != null && this.renameSpiritField.isFocused()) {
         return this.renameSpiritField.charTyped(c, modifiers);
      }
      return false;
   }

   @Override
   public boolean onMouseScrolled(double mx, double my, double delta) {
      if (my >= (double) this.listBoxStartY && my <= (double) this.listBoxEndY) {
         int maxScroll = Math.max(0, ClientCsmCache.getCount() - MAX_VISIBLE);
         if (maxScroll > 0) {
            this.listScrollOffset = Math.max(0, Math.min(maxScroll, this.listScrollOffset - (int) delta));
            return true;
         }
      }

      int maxScroll = Math.max(0, this.contentHeight - this.storedPh);
      if (maxScroll == 0) {
         return false;
      } else {
         this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) (delta * 10.0F)));
         return true;
      }
   }

   @Override
   public void tick() {
      if (this.renameSpiritField != null) {
         this.renameSpiritField.tick();
      }
   }

   @Override
   public void onClose() {
      this.cancelSpiritRename();
   }

   private int getSpiritIndex(int visualRow) {
      return this.listScrollOffset + visualRow;
   }

   private void onMarkUnique(int visualRow) {
      int idx = this.getSpiritIndex(visualRow);
      ModNetwork.CHANNEL.sendToServer(new PacketCsmMarkUnique(idx));
   }

   private void onToggleSpared(int visualRow) {
      int idx = this.getSpiritIndex(visualRow);
      ModNetwork.CHANNEL.sendToServer(new PacketCsmToggleSpared(idx));
   }

   private void onFeedFinger(int visualRow) {
      int idx = this.getSpiritIndex(visualRow);
      ModNetwork.CHANNEL.sendToServer(new PacketCsmFeedFinger(idx));
   }

   private void activateSpiritRename(int visualRow) {
      int idx = this.getSpiritIndex(visualRow);
      this.renameSpiritIdx = idx;
      List<CsmSpiritData> spirits = ClientCsmCache.getAll();
      if (this.renameSpiritField != null && idx < spirits.size()) {
         this.renameSpiritField.setValue(spirits.get(idx).displayName);
         this.renameSpiritField.moveCursorToEnd();
         this.renameSpiritField.setFocused(true);
         Screen screen = Minecraft.getInstance().screen;
         if (screen != null) {
            screen.setFocused(this.renameSpiritField);
         }
      }
   }

   private void confirmSpiritRename() {
      if (this.renameSpiritIdx >= 0 && this.renameSpiritField != null) {
         String name = this.renameSpiritField.getValue().strip();
         if (!name.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new PacketCsmRenameSpirit(this.renameSpiritIdx, name));
         }

         this.cancelSpiritRename();
      }
   }

   private void cancelSpiritRename() {
      this.renameSpiritIdx = -1;
      if (this.renameSpiritField != null) {
         this.renameSpiritField.visible = false;
         this.renameSpiritField.setFocused(false);
      }

      if (this.confirmRenameSpiritBtn != null) {
         this.confirmRenameSpiritBtn.visible = false;
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

   private void hideAllButtons() {
      this.hidePerRowButtons();
      if (this.renameSpiritField != null) this.renameSpiritField.visible = false;
      if (this.confirmRenameSpiritBtn != null) this.confirmRenameSpiritBtn.visible = false;
   }

   private void hidePerRowButtons() {
      this.uniqueButtons.forEach(b -> b.visible = false);
      this.sparedButtons.forEach(b -> b.visible = false);
      this.fingerButtons.forEach(b -> b.visible = false);
      this.renameSpiritBtns.forEach(b -> b.visible = false);
   }

   public static boolean isLocalPlayerCsm() {
      return ClientSetup.isLocalPlayerCsm(Minecraft.getInstance());
   }
}