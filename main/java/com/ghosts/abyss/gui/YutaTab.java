package com.ghosts.abyss.gui;

import com.ghosts.abyss.ClientYutaTechniqueCache;
import com.ghosts.abyss.network.ModNetwork;
import com.ghosts.abyss.network.PacketRegisterYutaTechnique;
import com.ghosts.abyss.network.PacketRemoveYutaTechnique;
import com.ghosts.abyss.network.PacketRenameYutaTechnique;
import com.ghosts.abyss.network.PacketToggleCopyOnHit;
import com.ghosts.abyss.network.PacketYutaDomainSelect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.ForgeRegistries;

public class YutaTab implements IMenuTab {
   private static final int COL_HEADER = -11149876;
   private static final int COL_LABEL = -3355444;
   private static final int COL_GOLD = -10496;
   private static final int COL_GREEN = -12255352;
   private static final int COL_GREY = -5592406;
   private static final int COL_DIM = -11184811;
   private static final int COL_BG = -452978672;
   private static final int COL_BORDER = -13993366;
   private static final int COL_INNER = 1143634538;
   private static final int COL_SEL_BG = 1426115754;
   private static final int COL_TEAL = -16724822;

   private static final int PAD = 8;
   private static final int ROW_H = 16;
   private static final int BTN_W = 76;
   private static final int REM_W = 22;
   private static final int REN_W = 22;
   private static final int MAX_NAME_LEN = 50;
   private static final int BTN_H = 14;
   private static final int REG_BTN_W = 160;
   private static final int MAX_VISIBLE_ROWS = 7;

   private int scrollOffset = 0;
   private int contentHeight = 0;
   private int storedPh = 230;
   private int storedPx = 0;
   private int storedPy = 0;
   private int storedPw = 280;
   private int listScrollOffset = 0;
   private int listBoxStartY = 0;
   private int listBoxEndY = 0;

   private JslopButton registerButton;
   private JslopButton toggleCopyButton;
   private final List<JslopButton> domainSelectButtons = new ArrayList<>();
   private final List<JslopButton> removeButtons = new ArrayList<>();
   private final List<JslopButton> renameButtons = new ArrayList<>();
   private JslopButton confirmRenameButton;
   private EditBox renameField;
   private int renameIdx = -1;

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
      return Component.literal("Copy");
   }

   @Override
   public int borderColor() {
      return COL_BORDER;
   }

   @Override
   public int tabActiveColor() {
      return COL_BORDER;
   }

   @Override
   public int tabHoverColor() {
      return -15054272;
   }

   @Override
   public int titleColor() {
      return COL_HEADER;
   }

   @Override
   public int panelColor() {
      return -15983592;
   }

   @Override
   public List<AbstractWidget> buildWidgets(int px, int py, int pw, int ph) {
      this.storedPx = px;
      this.storedPy = py;
      this.storedPw = pw;
      this.storedPh = ph;
      this.scrollOffset = 0;
      this.listScrollOffset = 0;
      this.domainSelectButtons.clear();
      this.removeButtons.clear();
      this.renameButtons.clear();
      this.renameIdx = -1;

      List<AbstractWidget> widgets = new ArrayList<>();
      int cx = px + pw / 2;

      this.registerButton = JslopButton.jslopBuilder(Component.literal("Register held item  (1 Level)"), btn -> this.doRegister())
              .bounds(cx - 80, py, REG_BTN_W, BTN_H)
              .colors(COL_BORDER, -12272726, -872411128, -872407019, 707426922)
              .build();
      widgets.add(this.registerButton);

      this.toggleCopyButton = JslopButton.jslopBuilder(Component.literal("Disable New Copies"), btn -> ModNetwork.CHANNEL.sendToServer(new PacketToggleCopyOnHit()))
              .bounds(cx - 80, py, REG_BTN_W, BTN_H)
              .colors(COL_BORDER, -12272726, -872411128, -872407019, 707426922)
              .build();
      widgets.add(this.toggleCopyButton);

      for (int i = 0; i < 40; ++i) {
         int idx = i;

         JslopButton domBtn = JslopButton.jslopBuilder(Component.literal("Set Domain"), b -> ModNetwork.CHANNEL.sendToServer(new PacketYutaDomainSelect(idx)))
                 .bounds(px, py, BTN_W, BTN_H)
                 .colors(COL_BORDER, -12272726, -872411128, -872407019, 707426922)
                 .build();
         domBtn.visible = false;
         this.domainSelectButtons.add(domBtn);
         widgets.add(domBtn);

         JslopButton remBtn = JslopButton.jslopBuilder(Component.literal("✕"), b -> ModNetwork.CHANNEL.sendToServer(new PacketRemoveYutaTechnique(idx)))
                 .bounds(px, py, REM_W, BTN_H)
                 .colors(-10079437, -6732732, -871364600, -870314482, 577123123)
                 .build();
         remBtn.visible = false;
         this.removeButtons.add(remBtn);
         widgets.add(remBtn);

         JslopButton renBtn = JslopButton.jslopBuilder(Component.literal("✏"), b -> this.activateRename(idx))
                 .bounds(px, py, REN_W, BTN_H)
                 .colors(-14001542, -12285782, -872411112, -872409824, 573201018)
                 .build();
         renBtn.visible = false;
         this.renameButtons.add(renBtn);
         widgets.add(renBtn);
      }

      Font font = Minecraft.getInstance().font;
      this.confirmRenameButton = JslopButton.jslopBuilder(Component.literal("✓"), b -> this.confirmRename())
              .bounds(px, py, REN_W, BTN_H)
              .colors(-13993430, -12277180, -872411136, -872407040, 573209130)
              .build();
      this.confirmRenameButton.visible = false;
      widgets.add(this.confirmRenameButton);

      this.renameField = new EditBox(font, px + PAD + 16, py, 100, BTN_H, Component.literal(""));
      this.renameField.setMaxLength(MAX_NAME_LEN);
      this.renameField.visible = false;
      widgets.add(this.renameField);

      return widgets;
   }

   @Override
   public void tick() {
      if (this.registerButton != null) this.registerButton.tick();
      if (this.toggleCopyButton != null) this.toggleCopyButton.tick();
      if (this.confirmRenameButton != null) this.confirmRenameButton.tick();
      if (this.renameField != null) this.renameField.tick();

      for (JslopButton b : this.domainSelectButtons) b.tick();
      for (JslopButton b : this.removeButtons) b.tick();
      for (JslopButton b : this.renameButtons) b.tick();
   }

   @Override
   public boolean onMouseScrolled(double mx, double my, double delta) {
      if (my >= (double) this.listBoxStartY && my <= (double) this.listBoxEndY) {
         List<ClientYutaTechniqueCache.Entry> entries = ClientYutaTechniqueCache.getAll();
         int maxListScroll = Math.max(0, entries.size() - MAX_VISIBLE_ROWS);
         if (maxListScroll > 0) {
            this.listScrollOffset = Math.max(0, Math.min(maxListScroll, this.listScrollOffset - (int) delta));
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
   public void render(GuiGraphics g, int px, int py, int pw, int ph, int mx, int my, float pt) {
      this.storedPx = px;
      this.storedPy = py;
      this.storedPw = pw;
      this.storedPh = ph;
      Font font = Minecraft.getInstance().font;
      Minecraft mc = Minecraft.getInstance();
      int cx = px + pw / 2;
      int cy = py + PAD - this.scrollOffset;

      g.enableScissor(px, py, px + pw, py + ph);

      if (!isLocalPlayerYuta()) {
         this.hideAllButtons();
         g.fill(px, py, px + pw, py + ph, -653192943);
         g.drawCenteredString(font, Component.literal("You are not an Okkotsu Yuta sorcerer."), cx, py + ph / 2 - 10, COL_GREY);
         g.drawCenteredString(font, Component.literal("Acquire CT=5 to use this menu."), cx, py + ph / 2 + 1, COL_DIM);
         this.contentHeight = ph;
         g.disableScissor();
         return;
      }

      int xpLevel = mc.player != null ? mc.player.experienceLevel : 0;
      g.drawCenteredString(font, "Experience Level: " + xpLevel, cx, cy, COL_GOLD);
      cy += 12;

      // --- Technique Imitation Panel ---
      int regBoxH = 58;
      this.drawBox(g, px + PAD, cy, pw - 16, regBoxH);
      g.drawCenteredString(font, "Technique Imitation", cx, cy + 4, COL_HEADER);
      g.fill(px + PAD + 8, cy + 14, px + pw - PAD - 8, cy + 15, COL_INNER);

      ItemStack held = this.getHeldCopyItem();
      if (held != null && !held.isEmpty()) {
         String heldName = held.getHoverName().getString();
         g.drawCenteredString(font, "Held: " + heldName, cx, cy + 19, COL_TEAL);
         double heldSkill = held.getOrCreateTag().getDouble("skill");
         g.drawCenteredString(font, String.format("Skill: %.0f", heldSkill), cx, cy + 29, COL_LABEL);
      } else {
         g.drawCenteredString(font, "Hold a Copied Cursed Technique item", cx, cy + 19, COL_GREY);
         g.drawCenteredString(font, "to register it permanently.", cx, cy + 29, COL_GREY);
      }

      boolean canRegister = ClientYutaTechniqueCache.getCount() < 40;
      if (this.registerButton != null) {
         int btnY = cy + regBoxH - BTN_H - 4;
         this.registerButton.setY(btnY);
         this.registerButton.visible = btnY + BTN_H > py && btnY < py + ph;
         this.registerButton.active = canRegister && held != null && !held.isEmpty() && xpLevel >= 1;
         this.registerButton.setMessage(Component.literal(canRegister ? "Register held item  (1 Level)" : "Library full (40/40)"));
      }

      cy += regBoxH + 6;

      // --- Registered Techniques Panel ---
      List<ClientYutaTechniqueCache.Entry> entries = ClientYutaTechniqueCache.getAll();
      int domainIdx = ClientYutaTechniqueCache.getDomainIdx();
      int listBoxH = 136 + (entries.size() > MAX_VISIBLE_ROWS ? 10 : 0);
      this.listBoxStartY = cy;
      this.listBoxEndY = cy + listBoxH;
      this.drawBox(g, px + PAD, cy, pw - 16, listBoxH);

      String countLabel = "Registered Techniques: " + entries.size() + " / 40";
      g.drawCenteredString(font, countLabel, cx, cy + 4, COL_HEADER);
      g.fill(px + PAD + 8, cy + 14, px + pw - PAD - 8, cy + 15, COL_INNER);

      int rowStartY = cy + 18;
      int innerClipBottom = cy + 18 + 112;

      if (entries.isEmpty()) {
         g.drawCenteredString(font, "No techniques registered.", cx, rowStartY + 2, COL_DIM);
         this.hideAllDomainButtons();
      } else {
         int maxListScroll = Math.max(0, entries.size() - MAX_VISIBLE_ROWS);
         this.listScrollOffset = Math.max(0, Math.min(this.listScrollOffset, maxListScroll));

         g.enableScissor(px, rowStartY, px + pw, innerClipBottom);

         for (int i = this.listScrollOffset; i < Math.min(entries.size(), this.listScrollOffset + MAX_VISIBLE_ROWS); ++i) {
            ClientYutaTechniqueCache.Entry e = entries.get(i);
            int visualRow = i - this.listScrollOffset;
            int rowY = rowStartY + visualRow * ROW_H;
            boolean isSelected = i == domainIdx;

            if (isSelected) {
               g.fill(px + PAD + 1, rowY, px + pw - PAD - 1, rowY + ROW_H, COL_SEL_BG);
            }

            boolean isRenaming = i == this.renameIdx;
            String bullet = isSelected ? "§a●§r" : "§8○§r";
            g.drawString(font, Component.literal(bullet), px + PAD + 4, rowY + (ROW_H - 9) / 2, -1, false);

            if (!isRenaming) {
               String displayName = e.name().length() > 20 ? e.name().substring(0, 19) + "…" : e.name();
               g.drawString(font, displayName, px + PAD + 16, rowY + (ROW_H - 9) / 2, isSelected ? COL_GREEN : COL_LABEL, false);
            }
         }

         g.disableScissor();

         if (entries.size() > MAX_VISIBLE_ROWS) {
            String hint = "▲▼ scroll  (" + (this.listScrollOffset + 1) + "-" + Math.min(this.listScrollOffset + MAX_VISIBLE_ROWS, entries.size()) + " of " + entries.size() + ")";
            g.drawCenteredString(font, hint, cx, innerClipBottom + 1, COL_DIM);
         }

         if (this.renameField != null) this.renameField.visible = false;
         if (this.confirmRenameButton != null) this.confirmRenameButton.visible = false;

         for (int i = 0; i < this.domainSelectButtons.size(); ++i) {
            boolean inRange = i >= this.listScrollOffset && i < this.listScrollOffset + MAX_VISIBLE_ROWS && i < entries.size();
            JslopButton domBtn = this.domainSelectButtons.get(i);
            JslopButton remBtn = this.removeButtons.get(i);

            if (!inRange) {
               domBtn.visible = false;
               remBtn.visible = false;
               if (i < this.renameButtons.size()) {
                  this.renameButtons.get(i).visible = false;
               }
            } else {
               boolean isSelected = i == domainIdx;
               int visualRow = i - this.listScrollOffset;
               int rowY = rowStartY + visualRow * ROW_H;
               int btnY = rowY + 1;
               boolean inView = btnY + BTN_H > rowStartY && btnY < innerClipBottom;

               int remX = px + pw - PAD - 2 - REM_W;
               remBtn.setX(remX);
               remBtn.setY(btnY);
               remBtn.visible = inView;
               remBtn.active = true;

               int renX = remX - REN_W - 2;
               int domX = renX - BTN_W - 2;
               JslopButton renBtn = i < this.renameButtons.size() ? this.renameButtons.get(i) : null;
               boolean isRenaming = i == this.renameIdx;

               if (renBtn != null) {
                  renBtn.setX(renX);
                  renBtn.setY(btnY);
                  renBtn.visible = inView && !isRenaming;
               }

               if (isRenaming && inView && this.renameField != null) {
                  int editX = px + PAD + 16;
                  this.renameField.setX(editX);
                  this.renameField.setY(btnY);
                  this.renameField.setWidth(renX - editX - 2);
                  this.renameField.visible = true;

                  if (this.confirmRenameButton != null) {
                     this.confirmRenameButton.setX(renX);
                     this.confirmRenameButton.setY(btnY);
                     this.confirmRenameButton.visible = true;
                  }
               }

               domBtn.setX(domX);
               domBtn.setY(btnY);
               domBtn.setWidth(BTN_W);
               domBtn.visible = inView;
               domBtn.active = !isSelected;
               domBtn.setMessage(Component.literal(isSelected ? "✓ Domain" : "Set Domain"));
            }
         }
      }

      cy += listBoxH + 6;

      // --- Domain Sure-Hit Panel ---
      int domBoxH = 32;
      this.drawBox(g, px + PAD, cy, pw - 16, domBoxH);
      g.drawCenteredString(font, "Domain Sure-Hit", cx, cy + 4, COL_HEADER);
      g.fill(px + PAD + 8, cy + 14, px + pw - PAD - 8, cy + 15, COL_INNER);

      String selectedName;
      if (domainIdx >= 0 && domainIdx < entries.size()) {
         selectedName = entries.get(domainIdx).name();
         if (selectedName.length() > 28) {
            selectedName = selectedName.substring(0, 27) + "…";
         }
      } else {
         selectedName = "JJC Default (first copy item)";
      }

      g.drawCenteredString(font, "Selected: " + selectedName, cx, cy + 19, domainIdx < 0 ? COL_GREY : COL_GREEN);
      cy += domBoxH + 6;

      // --- Copy Acquisition Panel ---
      boolean copyDisabled = mc.player != null && mc.player.getPersistentData().getBoolean("jslop_disable_copy");
      int copyBoxH = 56;
      this.drawBox(g, px + PAD, cy, pw - 16, copyBoxH);
      g.drawCenteredString(font, "Copy Acquisition", cx, cy + 4, COL_HEADER);
      g.fill(px + PAD + 8, cy + 14, px + pw - PAD - 8, cy + 15, COL_INNER);
      g.drawCenteredString(font, copyDisabled ? "§cDisabled§r — new copies will not be received." : "§aEnabled§r — copies received while Rika is deployed.", cx, cy + 20, COL_LABEL);

      if (this.toggleCopyButton != null) {
         int btnY = cy + copyBoxH - BTN_H - 4;
         this.toggleCopyButton.setY(btnY);
         this.toggleCopyButton.setMessage(Component.literal(copyDisabled ? "Enable New Copies" : "Disable New Copies"));
         this.toggleCopyButton.visible = btnY + BTN_H > py && btnY < py + ph;
         this.toggleCopyButton.active = true;
      }

      cy += copyBoxH + 6;
      this.contentHeight = cy - (py - this.scrollOffset) + PAD;

      g.disableScissor();
   }

   private void doRegister() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         int slot = mc.player.getInventory().selected;
         ModNetwork.CHANNEL.sendToServer(new PacketRegisterYutaTechnique(slot));
      }
   }

   private ItemStack getHeldCopyItem() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return null;
      }

      Item copiedCT = ForgeRegistries.ITEMS.getValue(new ResourceLocation("jujutsucraft", "copied_cursed_technique"));
      if (copiedCT == null) {
         return null;
      }

      ItemStack held = mc.player.getMainHandItem();
      if (!held.isEmpty() && held.getItem() == copiedCT) {
         return held.getOrCreateTag().getDouble("skill") <= 0.0D ? null : held;
      }

      return null;
   }

   public static boolean isLocalPlayerYuta() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      }

      try {
         Class<?> varsClass = Class.forName("net.mcreator.jujutsucraft.network.JujutsucraftModVariables");
         Field capField = varsClass.getDeclaredField("PLAYER_VARIABLES_CAPABILITY");
         capField.setAccessible(true);

         @SuppressWarnings("unchecked")
         Capability<Object> cap = (Capability<Object>) capField.get(null);
         Object vars = mc.player.getCapability(cap, null).orElse(null);

         if (vars == null) {
            return false;
         }

         Field ct1 = vars.getClass().getDeclaredField("PlayerCurseTechnique");
         Field ct2 = vars.getClass().getDeclaredField("PlayerCurseTechnique2");
         ct1.setAccessible(true);
         ct2.setAccessible(true);

         return (Double) ct1.get(vars) == 5.0D || (Double) ct2.get(vars) == 5.0D;
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.renameIdx < 0) {
         return false;
      }

      if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
         this.confirmRename();
         return true;
      } else if (keyCode == 256) { // Escape
         this.cancelRename();
         return true;
      } else {
         return this.renameField != null && this.renameField.isFocused() && this.renameField.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean charTyped(char c, int modifiers) {
      if (this.renameIdx < 0) {
         return false;
      }
      return this.renameField != null && this.renameField.isFocused() && this.renameField.charTyped(c, modifiers);
   }

   private void activateRename(int idx) {
      this.renameIdx = idx;
      List<ClientYutaTechniqueCache.Entry> entries = ClientYutaTechniqueCache.getAll();
      if (this.renameField != null) {
         if (idx >= 0 && idx < entries.size()) {
            this.renameField.setValue(entries.get(idx).name());
            this.renameField.moveCursorToEnd();
         }

         this.renameField.setFocused(true);
         Screen s = Minecraft.getInstance().screen;
         if (s != null) {
            s.setFocused(this.renameField);
         }
      }
   }

   private void confirmRename() {
      if (this.renameIdx >= 0 && this.renameField != null) {
         String newName = this.renameField.getValue().strip();
         if (!newName.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new PacketRenameYutaTechnique(this.renameIdx, newName));
         }
         this.cancelRename();
      }
   }

   private void cancelRename() {
      this.renameIdx = -1;
      if (this.renameField != null) {
         this.renameField.visible = false;
         this.renameField.setFocused(false);
      }

      if (this.confirmRenameButton != null) {
         this.confirmRenameButton.visible = false;
      }

      Screen s = Minecraft.getInstance().screen;
      if (s != null) {
         s.setFocused(null);
      }
   }

   @Override
   public void onClose() {
      this.cancelRename();
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
      if (this.registerButton != null) this.registerButton.visible = false;
      if (this.toggleCopyButton != null) this.toggleCopyButton.visible = false;
      this.hideAllDomainButtons();
   }

   private void hideAllDomainButtons() {
      for (JslopButton b : this.domainSelectButtons) b.visible = false;
      for (JslopButton b : this.removeButtons) b.visible = false;
      for (JslopButton b : this.renameButtons) b.visible = false;

      if (this.renameField != null) this.renameField.visible = false;
      if (this.confirmRenameButton != null) this.confirmRenameButton.visible = false;
   }
}